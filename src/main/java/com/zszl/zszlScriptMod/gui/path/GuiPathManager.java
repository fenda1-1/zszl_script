// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/path/GuiPathManager.java
// !! 这是为“动作列表”也添加了拖拽排序功能的最终版本 !!

package com.zszl.zszlScriptMod.gui.path;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.GuiActionEditor;
import net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.gui.MainUiLayoutManager;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import com.zszl.zszlScriptMod.path.runtime.locks.ResourceLockManager;
import com.zszl.zszlScriptMod.path.validation.PathConfigValidator;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.shadowbaritone.utils.GuiPathingPolicy;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GuiPathManager extends ThemedGuiScreen {
    private static final int SEQUENCE_SEARCH_FIELD_ID = 9101;
    private static final String UNGROUPED_SEQUENCE_TITLE = "未分类";
    private static final String UNGROUPED_SEQUENCE_KEY = "__ungrouped__";
    private static final String SEQUENCE_SEARCH_PLACEHOLDER = "搜索序列...";
    private static final String PREFERENCES_FILE_NAME = "gui_path_manager_layout.json";
    private static final int LAYOUT_DIVIDER_WIDTH = 12;
    private static final int LAYOUT_DIVIDER_HIT_THICKNESS = 24;
    private static final int MIN_LEFT_COLUMN_WIDTH = 150;
    private static final int MIN_STEP_COLUMN_WIDTH = 220;
    private static final int MIN_ACTION_COLUMN_WIDTH = 140;
    private static final int MIN_CATEGORY_HEIGHT = 108;
    private static final int MIN_SEQUENCE_HEIGHT = 168;
    private static final int STEP_ACTION_CARD_BASE_HEIGHT = 28;
    private static final String[] NOTE_COLOR_CODES = new String[] {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
    };
    private static final String[] NOTE_FORMAT_CODES = new String[] {
            "§l", "§n", "§o", "§m", "§k", "§r"
    };
    private static final String[] NOTE_FORMAT_BUTTON_TEXTS = new String[] {
            "§l粗", "§n下", "§o斜", "§m删", "§k乱", "§r重"
    };
    private static boolean preferencesLoaded = false;
    private static double savedLeftDividerRatio = 0.24D;
    private static double savedRightDividerRatio = 0.77D;
    private static double savedCategoryHeightRatio = 0.34D;
    private static int savedDebugMonitorX = Integer.MIN_VALUE;
    private static int savedDebugMonitorY = Integer.MIN_VALUE;
    private static int savedDebugMonitorPreferredWidth = 0;
    private static int savedDebugMonitorPreferredHeight = 0;
    private static boolean savedDebugMonitorMinimized = false;
    private static Method cachedPathStepGetNoteMethod;
    private static Method cachedPathStepSetNoteMethod;

    private static String defaultCategory() {
        return I18n.format("path.category.default");
    }

    private static String builtinCategory() {
        return I18n.format("path.category.builtin");
    }

    // --- 数据与状态 ---
    private List<PathSequence> allSequences;
    private List<String> categories;
    private static String selectedCategory = "";
    private static String selectedSubCategory = "";
    private static int selectedSequenceIndex = -1;
    private static int selectedStepIndex = -1;
    private static int selectedActionIndex = -1;
    private static String pendingFocusCategory = null;
    private static String pendingFocusSequenceName = null;
    private List<PathSequence> sequencesInCategory;
    private final List<CategoryTreeRow> visibleCategoryRows = new ArrayList<>();
    private final List<SequenceListRow> sequenceRows = new ArrayList<>();
    private final Set<String> collapsedSequenceGroups = new LinkedHashSet<>();

    private PathSequence selectedSequence = null;
    private PathStep selectedStep = null;
    private ActionData selectedAction = null;
    private int hoveredSequenceIndex = -1;
    private String hoveredSequenceNote = null;

    // --- 滚动条 ---
    private static int categoryScrollOffset = 0;
    private static int sequenceScrollOffset = 0;
    private static int stepScrollOffset = 0;
    private static int actionScrollOffset = 0;
    private int maxCategoryScroll = 0;
    private int maxSequenceScroll = 0;
    private int maxStepScroll = 0;
    private int maxActionScroll = 0;

    private boolean isDraggingCategory, isDraggingSequence, isDraggingStep, isDraggingAction;
    private int scrollClickY;
    private double leftDividerRatio = savedLeftDividerRatio;
    private double rightDividerRatio = savedRightDividerRatio;
    private double categoryHeightRatio = savedCategoryHeightRatio;
    private boolean draggingLeftDivider = false;
    private boolean draggingRightDivider = false;
    private boolean draggingCategoryDivider = false;

    private static boolean categoryPanelCollapsed = false;
    private GuiTextField sequenceSearchField;
    private String sequenceSearchQuery = "";
    private Rectangle categoryPanelToggleBounds;
    private Rectangle sequenceSearchClearBounds;
    private Rectangle leftDividerBounds;
    private Rectangle rightDividerBounds;
    private Rectangle categoryDividerBounds;
    private Rectangle headerToolbarViewportBounds;
    private int headerToolbarScrollIndex = 0;
    private int headerToolbarMaxScrollIndex = 0;

    // --- 步骤列表拖拽状态 ---
    private int draggingStepIndex = -1;
    private int stepDragMouseStartY = -1;
    private int stepDropIndex = -1;
    private final LinkedHashSet<Integer> selectedStepIndices = new LinkedHashSet<>();
    private final LinkedHashMap<Integer, Rectangle> visibleStepNoteButtons = new LinkedHashMap<>();
    private int selectionAnchorStepIndex = -1;

    // --- 核心新增：动作列表拖拽状态 ---
    private int draggingActionIndex = -1;
    private int actionDragMouseStartY = -1;
    private int actionDropIndex = -1;
    private final LinkedHashSet<Integer> selectedActionIndices = new LinkedHashSet<>();
    private int selectionAnchorActionIndex = -1;
    // --- 新增结束 ---

    private enum ClipboardPayloadType {
        NONE,
        STEPS,
        ACTIONS
    }

    private ClipboardPayloadType clipboardPayloadType = ClipboardPayloadType.NONE;
    private final List<PathStep> stepClipboard = new ArrayList<>();
    private final List<ActionData> actionClipboard = new ArrayList<>();
    private String clipboardStatusText = "";
    private int clipboardStatusColor = 0xFF8CFF9E;
    private static final int MAX_HISTORY_STATES = 80;
    private final Deque<EditorHistoryState> undoHistory = new ArrayDeque<>();
    private final Deque<EditorHistoryState> redoHistory = new ArrayDeque<>();
    private boolean restoringHistory = false;

    private boolean stepNotePopupVisible = false;
    private int editingStepNoteIndex = -1;
    private GuiTextField stepNotePopupField;
    private Rectangle stepNotePopupBounds = null;
    private Rectangle stepNotePopupCloseBounds = null;
    private Rectangle stepNotePopupSaveBounds = null;
    private Rectangle stepNotePopupCancelBounds = null;
    private final List<Rectangle> stepNotePopupColorBounds = new ArrayList<>();
    private final List<Rectangle> stepNotePopupFormatBounds = new ArrayList<>();

    private static final class EditorHistoryState {
        final List<PathSequence> sequences;
        final String selectedCategory;
        final String selectedSubCategory;
        final String selectedSequenceName;
        final int selectedStepIndex;
        final int selectedActionIndex;
        final LinkedHashSet<Integer> selectedStepIndices;
        final LinkedHashSet<Integer> selectedActionIndices;
        final int sequenceScrollOffset;
        final int stepScrollOffset;
        final int actionScrollOffset;
        final int selectionAnchorStepIndex;
        final int selectionAnchorActionIndex;

        private EditorHistoryState(List<PathSequence> sequences, String selectedCategory, String selectedSubCategory,
                String selectedSequenceName, int selectedStepIndex, int selectedActionIndex,
                LinkedHashSet<Integer> selectedStepIndices, LinkedHashSet<Integer> selectedActionIndices,
                int sequenceScrollOffset, int stepScrollOffset, int actionScrollOffset,
                int selectionAnchorStepIndex, int selectionAnchorActionIndex) {
            this.sequences = sequences;
            this.selectedCategory = selectedCategory;
            this.selectedSubCategory = selectedSubCategory;
            this.selectedSequenceName = selectedSequenceName;
            this.selectedStepIndex = selectedStepIndex;
            this.selectedActionIndex = selectedActionIndex;
            this.selectedStepIndices = selectedStepIndices;
            this.selectedActionIndices = selectedActionIndices;
            this.sequenceScrollOffset = sequenceScrollOffset;
            this.stepScrollOffset = stepScrollOffset;
            this.actionScrollOffset = actionScrollOffset;
            this.selectionAnchorStepIndex = selectionAnchorStepIndex;
            this.selectionAnchorActionIndex = selectionAnchorActionIndex;
        }
    }

    private static final Map<String, TextFormatting> ACTION_COLORS = new HashMap<>();
    static {
        ACTION_COLORS.put("delay", TextFormatting.YELLOW);
        ACTION_COLORS.put("command", TextFormatting.AQUA);
        ACTION_COLORS.put("key", TextFormatting.LIGHT_PURPLE);
        ACTION_COLORS.put("click", TextFormatting.GREEN);
        ACTION_COLORS.put("setview", TextFormatting.GOLD);
        ACTION_COLORS.put("rightclickblock", TextFormatting.BLUE);
        ACTION_COLORS.put("rightclickentity", TextFormatting.DARK_AQUA);
        ACTION_COLORS.put("takeallitems", TextFormatting.WHITE);
        ACTION_COLORS.put("dropfiltereditems", TextFormatting.RED);
        ACTION_COLORS.put("autochestclick", TextFormatting.DARK_GREEN);
        ACTION_COLORS.put("hunt", TextFormatting.DARK_RED); // 为 hunt 添加颜色
    }

    private static final class CategoryTreeRow {
        final String category;
        final String subCategory;
        final boolean customCategory;
        Rectangle bounds;
        Rectangle toggleBounds;

        private CategoryTreeRow(String category, String subCategory, boolean customCategory) {
            this.category = category == null ? "" : category;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.customCategory = customCategory;
        }

        boolean isSubCategory() {
            return !subCategory.isEmpty();
        }

        boolean isCustomRoot() {
            return customCategory && subCategory.isEmpty();
        }
    }

    private static final class SequenceListRow {
        final boolean header;
        final String title;
        final String groupKey;
        final boolean collapsed;
        final PathSequence sequence;
        Rectangle bounds;

        private SequenceListRow(boolean header, String title, String groupKey, boolean collapsed,
                PathSequence sequence) {
            this.header = header;
            this.title = title == null ? "" : title;
            this.groupKey = groupKey == null ? "" : groupKey;
            this.collapsed = collapsed;
            this.sequence = sequence;
        }

        static SequenceListRow header(String title, String groupKey, boolean collapsed) {
            return new SequenceListRow(true, title, groupKey, collapsed, null);
        }

        static SequenceListRow sequence(PathSequence sequence) {
            return new SequenceListRow(false, "", "", false, sequence);
        }
    }

    // --- UI 布局 ---
    private int categoryListWidth = 150;
    private int sequenceListWidth = 150;
    private int actionListWidth = 180;
    private int stepListX, stepListY, stepListWidth, stepListHeight;
    private int actionListX, actionListY, actionListHeight;
    private int categoryListX, categoryListY, categoryListHeight;
    private int sequenceListX, sequenceListY, sequenceListHeight;
    private int panelX, panelY, panelWidth, panelHeight;
    private int contentX, contentY, contentWidth, contentHeight;
    private int leftColumnWidth;
    private int stepColumnWidth;
    private int actionPaneX, actionPaneY, actionPaneWidth, actionPaneHeight;
    private int stepPaneX, stepPaneY, stepPaneWidth, stepPaneHeight;
    private int sequenceFooterCardY, sequenceFooterCardHeight;
    private int stepHeaderCardY, stepHeaderCardHeight;
    private int stepToolsCardY, stepToolsCardHeight;
    private int stepCoordsCardY, stepCoordsCardHeight;
    private int actionToolsCardY, actionToolsCardHeight;
    private int actionSaveCardY, actionSaveCardHeight;

    // --- UI 控件 ---
    private GuiButton btnAddSeq, btnDeleteSeq, btnMoveSeq, btnCopySeq, btnRenameSeq;
    private GuiButton btnAddStep, btnDeleteStep, btnCopyStep, btnMoveStepUp, btnMoveStepDown;
    private GuiButton btnEditStepFailure;
    private GuiButton btnAddAction, btnDeleteAction, btnMoveActionUp, btnMoveActionDown, btnEditAction;
    private GuiButton btnCopyAction;
    private GuiTextField gotoX, gotoY, gotoZ;
    private GuiButton btnGetCoords, btnClearCoords;
    private GuiButton btnCloseGui, btnSingleExec;
    private GuiButton btnManageCategories;
    private GuiButton btnLoopDelay;
    private GuiButton btnSetNote;
    private GuiButton btnNonInterruptingExecution;
    private GuiButton btnLockConflictPolicy;
    private GuiButton btnDebugPauseResume;
    private GuiButton btnDebugClearTrace;
    private GuiButton btnDebugStep;
    private GuiButton btnToggleActionBreakpoint;
    private GuiButton btnNodeEditor;
    private GuiButton btnLegacyTriggerRules;
    private GuiButton btnExecutionLogs;
    private GuiButton btnActionTemplates;
    private GuiButton btnActionVariableManager;
    private GuiButton btnToggleStepHeader;
    private GuiButton btnToolbarPrev;
    private GuiButton btnToolbarNext;
    private GuiButton btnDebugPanel;
    private boolean initialized = false;
    private boolean isDirty = false;
    private boolean reloadFromManagerRequested = false;
    private static boolean stepHeaderExpanded = false;
    private ActionData actionDetailPopupAction = null;
    private int actionDetailPopupIndex = -1;
    private Rectangle actionDetailPopupBounds = null;
    private boolean debugMonitorVisible = false;
    private boolean debugMonitorMinimized = false;
    private boolean draggingDebugMonitor = false;
    private int debugMonitorX = Integer.MIN_VALUE;
    private int debugMonitorY = Integer.MIN_VALUE;
    private int debugMonitorWidth = 0;
    private int debugMonitorHeight = 0;
    private int debugMonitorPreferredWidth = 0;
    private int debugMonitorPreferredHeight = 0;
    private int debugMonitorDragOffsetX = 0;
    private int debugMonitorDragOffsetY = 0;
    private int debugMonitorScrollOffset = 0;
    private int debugMonitorMaxScroll = 0;
    private boolean resizingDebugMonitorRight = false;
    private boolean resizingDebugMonitorBottom = false;
    private boolean resizingDebugMonitorCorner = false;
    private int debugMonitorResizeStartMouseX = 0;
    private int debugMonitorResizeStartMouseY = 0;
    private int debugMonitorResizeStartWidth = 0;
    private int debugMonitorResizeStartHeight = 0;
    private Rectangle debugMonitorBounds = null;
    private Rectangle debugMonitorHeaderBounds = null;
    private Rectangle debugMonitorCloseBounds = null;
    private Rectangle debugMonitorMinimizeBounds = null;
    private Rectangle debugMonitorContentBounds = null;
    private Rectangle debugMonitorResizeRightBounds = null;
    private Rectangle debugMonitorResizeBottomBounds = null;
    private Rectangle debugMonitorResizeCornerBounds = null;

    private float uiScale() {
        float sx = this.width / 1024.0f;
        float sy = this.height / 720.0f;
        return Math.max(0.78f, Math.min(1.0f, Math.min(sx, sy)));
    }

    private int s(int base) {
        return Math.max(1, Math.round(base * uiScale()));
    }

    private boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private int fitFooterToolButtonWidth(int availableWidth, int buttonCount, int gap, int preferredWidth) {
        if (buttonCount <= 0) {
            return preferredWidth;
        }
        int raw = (availableWidth - gap * (buttonCount - 1)) / buttonCount;
        int minWidth = s(40);
        return Math.max(minWidth, Math.min(preferredWidth, raw));
    }

    private String compactFooterToolLabel(String fullLabel, String compactLabel, int buttonWidth) {
        if (buttonWidth >= s(84)) {
            return fullLabel;
        }
        return compactLabel;
    }

    private int getUniformCardHeight(int itemCount, int columns, int buttonHeight, int rowGap, int topPadding,
            int bottomPadding) {
        int safeColumns = Math.max(1, columns);
        int rows = Math.max(1, (itemCount + safeColumns - 1) / safeColumns);
        return topPadding + rows * buttonHeight + Math.max(0, rows - 1) * rowGap + bottomPadding;
    }

    private int getStepToolsColumnCount() {
        int innerWidth = Math.max(s(96), stepPaneWidth - s(12));
        return innerWidth >= s(210) ? 3 : 2;
    }

    private int getActionToolsColumnCount() {
        int innerWidth = Math.max(s(96), actionPaneWidth - s(12));
        return innerWidth >= s(150) ? 2 : 1;
    }

    private int getSaveCardColumnCount() {
        int innerWidth = Math.max(s(80), actionPaneWidth - s(12));
        return innerWidth >= s(150) ? 2 : 1;
    }

    private boolean shouldWrapCoordinateButtons() {
        int innerWidth = Math.max(s(120), stepPaneWidth - s(12));
        int minFieldWidth = s(44);
        int buttonWidth = s(55);
        int gap = s(5);
        int requiredWidth = minFieldWidth * 3 + buttonWidth * 2 + gap * 4;
        return innerWidth < requiredWidth;
    }

    private int getCoordinateCardHeight() {
        return shouldWrapCoordinateButtons() ? s(66) : s(42);
    }

    private void layoutCoordinateControls(int startX, int cardY, int availableWidth) {
        if (gotoX == null || gotoY == null || gotoZ == null || btnGetCoords == null || btnClearCoords == null) {
            return;
        }
        int gap = s(5);
        int fieldHeight = s(20);
        int fieldY = cardY + s(11);
        int innerWidth = Math.max(s(120), availableWidth);
        boolean wrapped = shouldWrapCoordinateButtons();
        int fieldWidth;
        if (wrapped) {
            fieldWidth = Math.max(s(38), (innerWidth - gap * 2) / 3);
        } else {
            fieldWidth = Math.max(s(38), (innerWidth - s(55) * 2 - gap * 4) / 3);
        }

        gotoX.x = startX;
        gotoX.y = fieldY;
        gotoX.width = fieldWidth;
        gotoX.height = fieldHeight;
        gotoY.x = startX + fieldWidth + gap;
        gotoY.y = fieldY;
        gotoY.width = fieldWidth;
        gotoY.height = fieldHeight;
        gotoZ.x = startX + 2 * (fieldWidth + gap);
        gotoZ.y = fieldY;
        gotoZ.width = fieldWidth;
        gotoZ.height = fieldHeight;

        if (wrapped) {
            int buttonWidth = Math.max(s(56), (innerWidth - gap) / 2);
            int buttonY = fieldY + fieldHeight + s(6);
            btnGetCoords.x = startX;
            btnGetCoords.y = buttonY;
            btnGetCoords.width = buttonWidth;
            btnGetCoords.height = fieldHeight;
            btnClearCoords.x = startX + buttonWidth + gap;
            btnClearCoords.y = buttonY;
            btnClearCoords.width = buttonWidth;
            btnClearCoords.height = fieldHeight;
            return;
        }

        int buttonX = startX + 3 * (fieldWidth + gap);
        int buttonWidth = s(55);
        btnGetCoords.x = buttonX;
        btnGetCoords.y = fieldY;
        btnGetCoords.width = buttonWidth;
        btnGetCoords.height = fieldHeight;
        btnClearCoords.x = buttonX + buttonWidth + gap;
        btnClearCoords.y = fieldY;
        btnClearCoords.width = buttonWidth;
        btnClearCoords.height = fieldHeight;
    }

    private int getDebugMonitorButtonColumnCount() {
        return debugMonitorWidth >= s(250) ? 2 : 1;
    }

    private int getDebugMonitorButtonAreaHeight() {
        return getUniformCardHeight(4, getDebugMonitorButtonColumnCount(), s(20), s(4), 0, 0);
    }

    private void layoutDebugMonitorButtons() {
        if (btnDebugPauseResume == null || btnDebugClearTrace == null || btnDebugStep == null
                || btnToggleActionBreakpoint == null || debugMonitorBounds == null) {
            return;
        }
        int footerPadding = s(8);
        int buttonAreaHeight = getDebugMonitorButtonAreaHeight();
        int buttonStartY = debugMonitorY + debugMonitorHeight - footerPadding - buttonAreaHeight;
        int buttonStartX = debugMonitorX + s(8);
        int buttonWidth = debugMonitorWidth - s(16);
        layoutUniformButtonGrid(
                Arrays.asList(btnDebugPauseResume, btnDebugClearTrace, btnDebugStep, btnToggleActionBreakpoint),
                buttonStartX, buttonStartY, buttonWidth, getDebugMonitorButtonColumnCount(), s(4), s(4), s(20));
    }

    private boolean handleDebugMonitorButtonClick(int mouseX, int mouseY) throws IOException {
        if (debugMonitorMinimized) {
            return false;
        }
        List<GuiButton> debugButtons = Arrays.asList(
                btnDebugPauseResume, btnDebugClearTrace, btnDebugStep, btnToggleActionBreakpoint);
        for (GuiButton button : debugButtons) {
            if (button == null || !button.visible) {
                continue;
            }
            if (button.mousePressed(this.mc, mouseX, mouseY)) {
                button.playPressSound(this.mc.getSoundHandler());
                this.actionPerformed(button);
                return true;
            }
        }
        return false;
    }

    private void layoutUniformButtonGrid(List<GuiButton> buttons, int startX, int startY, int availableWidth,
            int columns,
            int horizontalGap, int verticalGap, int buttonHeight) {
        if (buttons == null || buttons.isEmpty()) {
            return;
        }
        int safeColumns = Math.max(1, columns);
        int width = Math.max(s(32),
                (Math.max(s(48), availableWidth) - horizontalGap * Math.max(0, safeColumns - 1)) / safeColumns);
        for (int i = 0; i < buttons.size(); i++) {
            GuiButton button = buttons.get(i);
            if (button == null) {
                continue;
            }
            int row = i / safeColumns;
            int column = i % safeColumns;
            button.x = startX + column * (width + horizontalGap);
            button.y = startY + row * (buttonHeight + verticalGap);
            button.width = width;
            button.height = buttonHeight;
        }
    }

    private static synchronized void ensurePreferencesLoaded() {
        if (preferencesLoaded) {
            return;
        }
        preferencesLoaded = true;
        savedLeftDividerRatio = 0.24D;
        savedRightDividerRatio = 0.77D;
        savedCategoryHeightRatio = 0.34D;
        savedDebugMonitorX = Integer.MIN_VALUE;
        savedDebugMonitorY = Integer.MIN_VALUE;
        savedDebugMonitorPreferredWidth = 0;
        savedDebugMonitorPreferredHeight = 0;
        savedDebugMonitorMinimized = false;
        Path path = getPreferencesPath();
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root != null) {
                if (root.has("leftDividerRatio")) {
                    savedLeftDividerRatio = root.get("leftDividerRatio").getAsDouble();
                }
                if (root.has("rightDividerRatio")) {
                    savedRightDividerRatio = root.get("rightDividerRatio").getAsDouble();
                }
                if (root.has("categoryHeightRatio")) {
                    savedCategoryHeightRatio = root.get("categoryHeightRatio").getAsDouble();
                }
                if (root.has("debugMonitorX")) {
                    savedDebugMonitorX = root.get("debugMonitorX").getAsInt();
                }
                if (root.has("debugMonitorY")) {
                    savedDebugMonitorY = root.get("debugMonitorY").getAsInt();
                }
                if (root.has("debugMonitorPreferredWidth")) {
                    savedDebugMonitorPreferredWidth = root.get("debugMonitorPreferredWidth").getAsInt();
                }
                if (root.has("debugMonitorPreferredHeight")) {
                    savedDebugMonitorPreferredHeight = root.get("debugMonitorPreferredHeight").getAsInt();
                }
                if (root.has("debugMonitorMinimized")) {
                    savedDebugMonitorMinimized = root.get("debugMonitorMinimized").getAsBoolean();
                }
            }
        } catch (Exception ignored) {
            savedLeftDividerRatio = 0.24D;
            savedRightDividerRatio = 0.77D;
            savedCategoryHeightRatio = 0.34D;
            savedDebugMonitorX = Integer.MIN_VALUE;
            savedDebugMonitorY = Integer.MIN_VALUE;
            savedDebugMonitorPreferredWidth = 0;
            savedDebugMonitorPreferredHeight = 0;
            savedDebugMonitorMinimized = false;
        }
    }

    private static synchronized void saveLayoutPreferences() {
        Path path = getPreferencesPath();
        if (path == null) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            JsonObject root = new JsonObject();
            root.addProperty("leftDividerRatio", savedLeftDividerRatio);
            root.addProperty("rightDividerRatio", savedRightDividerRatio);
            root.addProperty("categoryHeightRatio", savedCategoryHeightRatio);
            root.addProperty("debugMonitorX", savedDebugMonitorX);
            root.addProperty("debugMonitorY", savedDebugMonitorY);
            root.addProperty("debugMonitorPreferredWidth", savedDebugMonitorPreferredWidth);
            root.addProperty("debugMonitorPreferredHeight", savedDebugMonitorPreferredHeight);
            root.addProperty("debugMonitorMinimized", savedDebugMonitorMinimized);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private static Path getPreferencesPath() {
        return ProfileManager.getCurrentProfileDir().resolve(PREFERENCES_FILE_NAME);
    }

    private void recalculateEditorLayout() {
        panelX = 6;
        panelY = 6;
        panelWidth = Math.min(this.width - 4, Math.max(320, this.width - 12));
        panelHeight = Math.min(this.height - 4, Math.max(220, this.height - 12));

        int contentInset = s(10);
        int titleBarHeight = 20;
        contentX = panelX + contentInset;
        contentY = panelY + titleBarHeight + s(8);
        contentWidth = panelWidth - contentInset * 2;
        contentHeight = panelHeight - titleBarHeight - s(16);

        int availableWidth = Math.max(1, contentWidth - LAYOUT_DIVIDER_WIDTH * 2);
        int minLeftWidth = Math.max(s(132), Math.min(MIN_LEFT_COLUMN_WIDTH, Math.max(s(132), availableWidth / 3)));
        int minActionWidth = Math.max(s(128), Math.min(MIN_ACTION_COLUMN_WIDTH, Math.max(s(128), availableWidth / 3)));
        int minStepWidth = Math.max(s(168),
                Math.min(MIN_STEP_COLUMN_WIDTH, Math.max(s(168), availableWidth - minLeftWidth - minActionWidth)));
        if (minLeftWidth + minStepWidth + minActionWidth > availableWidth) {
            int overflow = minLeftWidth + minStepWidth + minActionWidth - availableWidth;
            int shrinkLeft = Math.min(overflow / 2, Math.max(0, minLeftWidth - s(120)));
            minLeftWidth -= shrinkLeft;
            overflow -= shrinkLeft;
            int shrinkAction = Math.min(overflow, Math.max(0, minActionWidth - s(120)));
            minActionWidth -= shrinkAction;
            overflow -= shrinkAction;
            minStepWidth = Math.max(s(140), availableWidth - minLeftWidth - minActionWidth);
        }
        int maxLeftWidth = Math.max(minLeftWidth, availableWidth - minStepWidth - minActionWidth);
        leftColumnWidth = MathHelper.clamp((int) Math.round(availableWidth * leftDividerRatio), minLeftWidth,
                maxLeftWidth);
        int minRightBoundary = leftColumnWidth + minStepWidth;
        int maxRightBoundary = Math.max(minRightBoundary, availableWidth - minActionWidth);
        int rightBoundary = MathHelper.clamp((int) Math.round(availableWidth * rightDividerRatio), minRightBoundary,
                maxRightBoundary);
        stepColumnWidth = Math.max(minStepWidth, rightBoundary - leftColumnWidth);
        actionListWidth = Math.max(minActionWidth, availableWidth - rightBoundary);
        leftDividerRatio = leftColumnWidth / (double) Math.max(1, availableWidth);
        rightDividerRatio = rightBoundary / (double) Math.max(1, availableWidth);

        int dividerX = contentX + leftColumnWidth;
        int secondDividerX = contentX + rightBoundary + LAYOUT_DIVIDER_WIDTH;
        int dividerHitOffset = Math.max(0, (LAYOUT_DIVIDER_HIT_THICKNESS - LAYOUT_DIVIDER_WIDTH) / 2);
        leftDividerBounds = new Rectangle(dividerX - dividerHitOffset, contentY + 4, LAYOUT_DIVIDER_HIT_THICKNESS,
                Math.max(40, contentHeight - 8));
        rightDividerBounds = new Rectangle(secondDividerX - dividerHitOffset, contentY + 4,
                LAYOUT_DIVIDER_HIT_THICKNESS,
                Math.max(40, contentHeight - 8));

        categoryListX = contentX;
        sequenceListX = contentX;
        stepPaneX = dividerX + LAYOUT_DIVIDER_WIDTH;
        actionPaneX = secondDividerX + LAYOUT_DIVIDER_WIDTH;

        int minCategoryHeight = Math.max(s(88), Math.min(MIN_CATEGORY_HEIGHT, Math.max(s(88), contentHeight / 3)));
        int minSequenceHeight = Math.max(s(120), Math.min(MIN_SEQUENCE_HEIGHT, Math.max(s(120), contentHeight / 3)));
        int availableLeftHeight = Math.max(1, contentHeight - (categoryPanelCollapsed ? s(8) : LAYOUT_DIVIDER_WIDTH));
        int maxCategoryHeight = Math.max(minCategoryHeight, availableLeftHeight - minSequenceHeight);
        int categoryHeight = categoryPanelCollapsed ? getCollapsedCategoryPanelHeight()
                : MathHelper.clamp((int) Math.round(availableLeftHeight * categoryHeightRatio), minCategoryHeight,
                        maxCategoryHeight);
        if (!categoryPanelCollapsed) {
            categoryHeightRatio = categoryHeight / (double) Math.max(1, availableLeftHeight);
        }

        categoryListY = contentY;
        categoryListWidth = leftColumnWidth;
        categoryListHeight = categoryHeight;
        int categorySequenceGap = categoryPanelCollapsed ? s(8) : LAYOUT_DIVIDER_WIDTH;
        categoryDividerBounds = categoryPanelCollapsed ? null
                : new Rectangle(contentX + 4, categoryListY + categoryListHeight - dividerHitOffset,
                        Math.max(40, leftColumnWidth - 8), LAYOUT_DIVIDER_HIT_THICKNESS);

        sequenceListY = categoryListY + categoryListHeight + categorySequenceGap;
        sequenceListWidth = leftColumnWidth;
        sequenceListHeight = Math.max(minSequenceHeight, contentY + contentHeight - sequenceListY);

        stepPaneY = contentY;
        stepPaneWidth = Math.max(minStepWidth, secondDividerX - stepPaneX);
        stepPaneHeight = contentHeight;

        actionPaneY = contentY;
        actionPaneWidth = Math.max(minActionWidth, panelX + panelWidth - contentInset - actionPaneX);
        actionPaneHeight = contentHeight;

        sequenceFooterCardHeight = s(82);
        sequenceFooterCardY = sequenceListY + sequenceListHeight - sequenceFooterCardHeight;

        stepHeaderCardY = stepPaneY;
        stepHeaderCardHeight = stepHeaderExpanded ? s(62) : getCollapsedStepHeaderHeight();
        int stepButtonHeight = s(20);
        int stepButtonGap = s(4);
        stepToolsCardHeight = getUniformCardHeight(6, getStepToolsColumnCount(), stepButtonHeight, stepButtonGap, s(7),
                s(7));
        stepCoordsCardHeight = getCoordinateCardHeight();
        stepCoordsCardY = stepPaneY + stepPaneHeight - stepCoordsCardHeight;
        stepToolsCardY = stepCoordsCardY - s(8) - stepToolsCardHeight;
        stepListX = stepPaneX + s(6);
        stepListWidth = stepPaneWidth - s(12);
        stepListY = stepHeaderCardY + stepHeaderCardHeight + s(8);
        stepListHeight = Math.max(s(80), stepToolsCardY - stepListY - s(6));

        actionToolsCardHeight = getUniformCardHeight(6, getActionToolsColumnCount(), s(20), s(4), s(8), s(8));
        actionSaveCardHeight = getUniformCardHeight(2, getSaveCardColumnCount(), s(20), s(4), s(8), s(8));
        actionSaveCardY = actionPaneY + actionPaneHeight - actionSaveCardHeight;
        actionToolsCardY = actionSaveCardY - s(8) - actionToolsCardHeight;
        actionListX = actionPaneX + s(6);
        actionListY = actionPaneY + s(30);
        actionListWidth = actionPaneWidth - s(12);
        actionListHeight = Math.max(s(80), actionToolsCardY - actionListY - s(6));
        ensureDebugMonitorWindowBounds();
    }

    private void persistCurrentLayoutRatios() {
        savedLeftDividerRatio = leftDividerRatio;
        savedRightDividerRatio = rightDividerRatio;
        savedCategoryHeightRatio = categoryHeightRatio;
        saveLayoutPreferences();
    }

    private void persistDebugMonitorLayout() {
        savedDebugMonitorX = debugMonitorX;
        savedDebugMonitorY = debugMonitorY;
        savedDebugMonitorPreferredWidth = debugMonitorPreferredWidth;
        savedDebugMonitorPreferredHeight = debugMonitorPreferredHeight;
        savedDebugMonitorMinimized = debugMonitorMinimized;
        saveLayoutPreferences();
    }

    private List<GuiButton> getHeaderToolbarButtons() {
        return Arrays.asList(btnActionTemplates, btnExecutionLogs, btnDebugPanel, btnLegacyTriggerRules,
                btnActionVariableManager, btnNodeEditor);
    }

    private void layoutHeaderToolbarButtons() {
        List<GuiButton> buttons = getHeaderToolbarButtons();
        if (btnToolbarPrev == null || btnToolbarNext == null) {
            return;
        }
        String title = I18n.format("gui.path.manager.title");
        int titleX = panelX + s(12);
        int titleWidth = fontRenderer == null ? 120 : fontRenderer.getStringWidth(title);
        int toolbarX = titleX + titleWidth + s(16);
        int toolbarY = panelY + 1;
        int toolbarHeight = s(18);
        int toolbarRight = panelX + panelWidth - s(10);
        int toolbarWidth = Math.max(0, toolbarRight - toolbarX);
        int arrowWidth = s(18);
        int gap = s(5);

        List<Integer> buttonWidths = new ArrayList<>();
        int totalWidth = 0;
        for (GuiButton button : buttons) {
            String plainLabel = TextFormatting.getTextWithoutFormattingCodes(button.displayString);
            int width = MathHelper.clamp(fontRenderer.getStringWidth(plainLabel == null ? "" : plainLabel) + s(18),
                    s(74), s(124));
            buttonWidths.add(width);
            totalWidth += width;
        }
        totalWidth += gap * Math.max(0, buttons.size() - 1);

        boolean overflow = totalWidth > toolbarWidth;
        btnToolbarPrev.visible = overflow;
        btnToolbarNext.visible = overflow;
        btnToolbarPrev.enabled = overflow && headerToolbarScrollIndex > 0;
        int viewportX = toolbarX;
        int viewportWidth = toolbarWidth;
        if (overflow) {
            viewportX += arrowWidth + gap;
            viewportWidth = Math.max(s(60), toolbarWidth - (arrowWidth + gap) * 2);
            btnToolbarPrev.x = toolbarX;
            btnToolbarPrev.y = toolbarY;
            btnToolbarPrev.width = arrowWidth;
            btnToolbarPrev.height = toolbarHeight;
            btnToolbarNext.width = arrowWidth;
            btnToolbarNext.height = toolbarHeight;
            btnToolbarNext.x = panelX + panelWidth - s(10) - arrowWidth;
            btnToolbarNext.y = toolbarY;
        }
        headerToolbarViewportBounds = new Rectangle(viewportX, toolbarY, Math.max(0, viewportWidth), toolbarHeight);

        int maxStart = 0;
        for (int start = 0; start < buttons.size(); start++) {
            int used = 0;
            for (int i = start; i < buttons.size(); i++) {
                int next = buttonWidths.get(i) + (used == 0 ? 0 : gap);
                if (used + next > viewportWidth) {
                    break;
                }
                used += next;
                maxStart = start;
            }
        }
        headerToolbarMaxScrollIndex = Math.max(0, maxStart);
        headerToolbarScrollIndex = MathHelper.clamp(headerToolbarScrollIndex, 0, headerToolbarMaxScrollIndex);
        btnToolbarNext.enabled = overflow && headerToolbarScrollIndex < headerToolbarMaxScrollIndex;

        int buttonX = viewportX;
        boolean hiddenAny = false;
        for (int i = 0; i < buttons.size(); i++) {
            GuiButton button = buttons.get(i);
            int width = buttonWidths.get(i);
            if (i < headerToolbarScrollIndex) {
                button.visible = false;
                hiddenAny = true;
                continue;
            }
            if (buttonX + width > viewportX + viewportWidth) {
                button.visible = false;
                hiddenAny = true;
                continue;
            }
            button.x = buttonX;
            button.y = toolbarY;
            button.width = width;
            button.height = toolbarHeight;
            button.visible = true;
            buttonX += width + gap;
        }
        if (!overflow) {
            headerToolbarMaxScrollIndex = 0;
            btnToolbarPrev.visible = false;
            btnToolbarNext.visible = false;
        } else if (!hiddenAny) {
            btnToolbarNext.enabled = false;
        }
    }

    public GuiPathManager() {
        ensurePreferencesLoaded();
        this.leftDividerRatio = savedLeftDividerRatio;
        this.rightDividerRatio = savedRightDividerRatio;
        this.categoryHeightRatio = savedCategoryHeightRatio;
        this.debugMonitorX = savedDebugMonitorX;
        this.debugMonitorY = savedDebugMonitorY;
        this.debugMonitorPreferredWidth = savedDebugMonitorPreferredWidth;
        this.debugMonitorPreferredHeight = savedDebugMonitorPreferredHeight;
        this.debugMonitorMinimized = savedDebugMonitorMinimized;
    }

    public static GuiPathManager openForSequence(String category, String sequenceName) {
        selectedCategory = category == null || category.trim().isEmpty() ? defaultCategory() : category.trim();
        selectedSubCategory = "";
        selectedSequenceIndex = -1;
        selectedStepIndex = -1;
        selectedActionIndex = -1;
        categoryScrollOffset = 0;
        sequenceScrollOffset = 0;
        stepScrollOffset = 0;
        actionScrollOffset = 0;
        pendingFocusCategory = selectedCategory;
        pendingFocusSequenceName = sequenceName == null ? "" : sequenceName.trim();
        return new GuiPathManager();
    }

    public void markDirty() {
        this.isDirty = true;
    }

    public void requestReloadFromManager() {
        this.reloadFromManagerRequested = true;
        this.isDirty = true;
    }

    private List<PathSequence> copySequencesForHistory(List<PathSequence> source) {
        List<PathSequence> copied = new ArrayList<>();
        if (source == null) {
            return copied;
        }
        for (PathSequence sequence : source) {
            if (sequence != null) {
                copied.add(new PathSequence(sequence));
            }
        }
        return copied;
    }

    private EditorHistoryState captureHistoryState() {
        return new EditorHistoryState(
                copySequencesForHistory(this.allSequences),
                selectedCategory,
                selectedSubCategory,
                selectedSequence == null ? "" : normalize(selectedSequence.getName()),
                selectedStepIndex,
                selectedActionIndex,
                new LinkedHashSet<>(selectedStepIndices),
                new LinkedHashSet<>(selectedActionIndices),
                sequenceScrollOffset,
                stepScrollOffset,
                actionScrollOffset,
                selectionAnchorStepIndex,
                selectionAnchorActionIndex);
    }

    private void pushUndoHistory(String reason) {
        if (restoringHistory) {
            return;
        }
        undoHistory.push(captureHistoryState());
        while (undoHistory.size() > MAX_HISTORY_STATES) {
            undoHistory.removeLast();
        }
        redoHistory.clear();
    }

    private void restoreHistoryState(EditorHistoryState state) {
        if (state == null) {
            return;
        }
        restoringHistory = true;
        try {
            this.allSequences = copySequencesForHistory(state.sequences);
            selectedCategory = state.selectedCategory == null ? defaultCategory() : state.selectedCategory;
            selectedSubCategory = state.selectedSubCategory == null ? "" : state.selectedSubCategory;
            filterSequencesByCategory();

            int sequenceIndex = -1;
            if (state.selectedSequenceName != null && !state.selectedSequenceName.trim().isEmpty()) {
                sequenceIndex = findSequenceIndex(null, state.selectedSequenceName);
            }
            selectSequence(sequenceIndex);

            if (selectedSequence != null) {
                if (state.selectedStepIndex >= 0 && state.selectedStepIndex < selectedSequence.getSteps().size()) {
                    setActiveStepPreservingSelection(state.selectedStepIndex);
                } else {
                    selectStep(-1);
                }
                selectedStepIndices.clear();
                for (Integer index : state.selectedStepIndices) {
                    if (index != null && index >= 0 && index < selectedSequence.getSteps().size()) {
                        selectedStepIndices.add(index);
                    }
                }
                if (selectedStepIndex >= 0 && selectedStepIndices.isEmpty()) {
                    selectedStepIndices.add(selectedStepIndex);
                }
                selectionAnchorStepIndex = state.selectionAnchorStepIndex;

                if (selectedStep != null) {
                    if (state.selectedActionIndex >= 0 && state.selectedActionIndex < selectedStep.getActions().size()) {
                        setActiveActionPreservingSelection(state.selectedActionIndex);
                    } else {
                        selectAction(-1);
                    }
                    selectedActionIndices.clear();
                    for (Integer index : state.selectedActionIndices) {
                        if (index != null && index >= 0 && index < selectedStep.getActions().size()) {
                            selectedActionIndices.add(index);
                        }
                    }
                    if (selectedActionIndex >= 0 && selectedActionIndices.isEmpty()) {
                        selectedActionIndices.add(selectedActionIndex);
                    }
                    selectionAnchorActionIndex = state.selectionAnchorActionIndex;
                }
            }

            sequenceScrollOffset = state.sequenceScrollOffset;
            stepScrollOffset = state.stepScrollOffset;
            actionScrollOffset = state.actionScrollOffset;
            closeStepNotePopup();
            actionDetailPopupAction = null;
            actionDetailPopupIndex = -1;
            actionDetailPopupBounds = null;
            markDirty();
            updateButtonStates();
        } finally {
            restoringHistory = false;
        }
    }

    private void performUndo() {
        if (undoHistory.isEmpty()) {
            return;
        }
        redoHistory.push(captureHistoryState());
        restoreHistoryState(undoHistory.pop());
        clipboardStatusText = "已撤销上一步编辑";
        clipboardStatusColor = 0xFFFFD77A;
    }

    private void performRedo() {
        if (redoHistory.isEmpty()) {
            return;
        }
        undoHistory.push(captureHistoryState());
        restoreHistoryState(redoHistory.pop());
        clipboardStatusText = "已恢复上一步编辑";
        clipboardStatusColor = 0xFF8CFF9E;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeKey(String value) {
        return normalize(value).toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return normalize(value).isEmpty();
    }

    private boolean isCustomCategory(String category) {
        String normalizedCategory = normalize(category);
        if (normalizedCategory.isEmpty()) {
            return false;
        }
        for (PathSequence sequence : allSequences) {
            if (sequence != null
                    && sequence.isCustom()
                    && normalizedCategory.equalsIgnoreCase(normalize(sequence.getCategory()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRootCategoryMode() {
        return isCustomCategory(selectedCategory) && isBlank(selectedSubCategory);
    }

    private String buildSequenceGroupKey(String category, String subCategory) {
        String normalizedCategory = normalizeKey(category);
        String normalizedSubCategory = isBlank(subCategory) ? UNGROUPED_SEQUENCE_KEY : normalizeKey(subCategory);
        return normalizedCategory + "::" + normalizedSubCategory;
    }

    private boolean isSequenceGroupCollapsed(String groupKey) {
        return groupKey != null && !groupKey.isEmpty() && collapsedSequenceGroups.contains(groupKey);
    }

    private void toggleSequenceGroupCollapsed(String groupKey) {
        if (groupKey == null || groupKey.isEmpty()) {
            return;
        }
        if (!collapsedSequenceGroups.add(groupKey)) {
            collapsedSequenceGroups.remove(groupKey);
        }
    }

    private void syncSelectedCategoryState() {
        if (selectedCategory == null || selectedCategory.trim().isEmpty()) {
            selectedCategory = defaultCategory();
        }
        if (!categories.contains(selectedCategory)) {
            selectedCategory = categories.isEmpty() ? defaultCategory() : categories.get(0);
            selectedSubCategory = "";
        }
        if (!isCustomCategory(selectedCategory)) {
            selectedSubCategory = "";
            return;
        }
        if (isBlank(selectedSubCategory)) {
            return;
        }
        for (String subCategory : MainUiLayoutManager.getSubCategories(selectedCategory)) {
            if (normalize(selectedSubCategory).equalsIgnoreCase(normalize(subCategory))) {
                selectedSubCategory = subCategory;
                return;
            }
        }
        selectedSubCategory = "";
    }

    private double roundCoordinate(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    private double resolveSafeStandingY(EntityPlayerSP player) {
        if (player == null || player.world == null || player.getEntityBoundingBox() == null) {
            return Double.NaN;
        }

        AxisAlignedBB playerBox = player.getEntityBoundingBox();
        AxisAlignedBB supportProbe = playerBox.grow(-0.05D, 0.0D, -0.05D).offset(0.0D, -0.20D, 0.0D);
        List<AxisAlignedBB> collisions = player.world.getCollisionBoxes(player, supportProbe);
        double feetY = playerBox.minY;
        double supportTop = Double.NEGATIVE_INFINITY;

        for (AxisAlignedBB collision : collisions) {
            if (collision == null) {
                continue;
            }
            if (collision.maxY > feetY + 0.25D) {
                continue;
            }
            if (collision.maxY < feetY - 1.25D) {
                continue;
            }
            supportTop = Math.max(supportTop, collision.maxY);
        }

        if (supportTop == Double.NEGATIVE_INFINITY) {
            return feetY;
        }

        return Math.max(feetY, supportTop + 0.01D);
    }

    private double[] captureCurrentSafeGotoPoint() {
        if (mc == null || mc.player == null) {
            return new double[] { Double.NaN, Double.NaN, Double.NaN };
        }

        EntityPlayerSP player = mc.player;
        double safeY = resolveSafeStandingY(player);
        return new double[] {
                roundCoordinate(player.posX),
                roundCoordinate(Double.isNaN(safeY) ? player.posY : safeY),
                roundCoordinate(player.posZ)
        };
    }

    public List<ActionData> copySelectedActionsForTemplateDraft() {
        List<ActionData> copied = new ArrayList<>();
        if (selectedStep == null || selectedStep.getActions() == null || selectedStep.getActions().isEmpty()) {
            return copied;
        }
        List<Integer> indices = new ArrayList<>(selectedActionIndices);
        if (indices.isEmpty() && selectedActionIndex >= 0) {
            indices.add(selectedActionIndex);
        }
        Collections.sort(indices);
        for (Integer index : indices) {
            if (index == null || index < 0 || index >= selectedStep.getActions().size()) {
                continue;
            }
            copied.add(new ActionData(selectedStep.getActions().get(index)));
        }
        return copied;
    }

    public List<ActionData> copySelectedStepActionsForTemplateDraft() {
        List<ActionData> copied = new ArrayList<>();
        if (selectedStep == null || selectedStep.getActions() == null) {
            return copied;
        }
        for (ActionData action : selectedStep.getActions()) {
            if (action != null) {
                copied.add(new ActionData(action));
            }
        }
        return copied;
    }

    public String getTemplateDraftSourceText() {
        if (selectedSequence == null) {
            return "未选择序列";
        }
        if (selectedStep == null) {
            return "序列: " + safeText(selectedSequence.getName()) + " / 未选择步骤";
        }
        int selectedActionCount = selectedActionIndices.isEmpty() ? (selectedActionIndex >= 0 ? 1 : 0)
                : selectedActionIndices.size();
        return "序列: " + safeText(selectedSequence.getName()) + " / 步骤 #" + (selectedStepIndex + 1)
                + " / 已选动作 " + selectedActionCount + " / 步骤动作 " + selectedStep.getActions().size();
    }

    public boolean insertTemplateActionsIntoSelectedStep(List<ActionData> templateActions, String templateName) {
        if (selectedSequence == null || selectedStep == null || templateActions == null || templateActions.isEmpty()) {
            return false;
        }
        int insertionBaseIndex = selectedStep.getActions().size();
        pushUndoHistory("insert-template-actions");
        for (ActionData action : templateActions) {
            if (action != null) {
                selectedStep.addAction(cloneTemplateActionForInsertion(action, insertionBaseIndex));
            }
        }
        if (selectedStep.getActions().size() > insertionBaseIndex) {
            selectAction(selectedStep.getActions().size() - 1);
        }
        markDirty();
        updateButtonStates();
        return true;
    }

    public boolean insertTemplateAsNewStep(List<ActionData> templateActions, String templateName) {
        if (selectedSequence == null || templateActions == null || templateActions.isEmpty()) {
            return false;
        }
        int insertIndex = selectedStepIndex >= 0 && selectedStepIndex < selectedSequence.getSteps().size()
                ? selectedStepIndex + 1
                : selectedSequence.getSteps().size();
        PathStep newStep = new PathStep(captureCurrentSafeGotoPoint());
        String note = templateName == null || templateName.trim().isEmpty()
                ? "动作模板"
                : "动作模板: " + templateName.trim();
        newStep.setNote(note);
        for (ActionData action : templateActions) {
            if (action != null) {
                newStep.addAction(cloneTemplateActionForInsertion(action, 0));
            }
        }
        if (newStep.getActions().isEmpty()) {
            return false;
        }
        pushUndoHistory("insert-template-step");
        selectedSequence.getSteps().add(insertIndex, newStep);
        selectStep(insertIndex);
        selectAction(newStep.getActions().size() - 1);
        markDirty();
        updateButtonStates();
        return true;
    }

    private ActionData cloneTemplateActionForInsertion(ActionData action, int actionIndexOffset) {
        ActionData copied = new ActionData(action);
        if (actionIndexOffset <= 0 || copied == null || copied.type == null
                || !"goto_action".equalsIgnoreCase(copied.type) || copied.params == null
                || !copied.params.has("targetActionIndex")) {
            return copied;
        }
        JsonElement target = copied.params.get("targetActionIndex");
        if (target == null || target.isJsonNull()) {
            return copied;
        }
        try {
            int targetIndex = Integer.parseInt(target.getAsString().trim());
            copied.params.addProperty("targetActionIndex", targetIndex + actionIndexOffset);
        } catch (Exception ignored) {
        }
        return copied;
    }

    private void reloadData() {
        PathSequenceManager.initializePathSequences();
        MainUiLayoutManager.ensureLoaded();
        this.allSequences = PathSequenceManager.getAllVisibleSequences();
        this.categories = PathSequenceManager.getVisibleCategories();
        syncSelectedCategoryState();
        filterSequencesByCategory();
    }

    private void filterSequencesByCategory() {
        PathSequence previousSelection = selectedSequence;
        String previousSelectionName = previousSelection == null ? null : previousSelection.getName();
        int previousScrollOffset = sequenceScrollOffset;
        String normalizedSearch = PinyinSearchHelper.normalizeQuery(sequenceSearchQuery);

        syncSelectedCategoryState();

        this.sequencesInCategory = allSequences.stream()
                .filter(s -> selectedCategory.equals(s.getCategory()))
                .filter(s -> isBlank(selectedSubCategory)
                        || normalize(selectedSubCategory).equalsIgnoreCase(normalize(s.getSubCategory())))
                .filter(s -> matchesSequenceSearch(s, normalizedSearch))
                .collect(Collectors.toList());

        sortSequencesInCategory();
        rebuildSequenceRows();

        int selectedIndex = findSequenceIndex(previousSelection, previousSelectionName);
        if (selectedIndex != -1) {
            selectSequence(selectedIndex);
        } else {
            selectSequence(-1);
        }

        int visibleItemCount = getSequenceVisibleItemCount();
        int maxScroll = Math.max(0, sequenceRows.size() - visibleItemCount);
        sequenceScrollOffset = MathHelper.clamp(previousScrollOffset, 0, maxScroll);
    }

    private boolean matchesSequenceSearch(PathSequence sequence, String normalizedSearch) {
        if (sequence == null || normalizedSearch.isEmpty()) {
            return true;
        }
        String searchText = normalize(sequence.getName())
                + " "
                + normalize(getSequenceDisplayName(sequence))
                + " "
                + normalize(sequence.getCategory())
                + " "
                + normalize(sequence.getSubCategory())
                + " "
                + normalize(sequence.getNote());
        return PinyinSearchHelper.matchesNormalized(searchText, normalizedSearch);
    }

    private void rebuildSequenceRows() {
        sequenceRows.clear();
        if (!isRootCategoryMode()) {
            for (PathSequence sequence : sequencesInCategory) {
                sequenceRows.add(SequenceListRow.sequence(sequence));
            }
            return;
        }

        boolean searchActive = !isBlank(sequenceSearchQuery);
        Set<String> addedSubCategories = new LinkedHashSet<>();

        for (String subCategory : MainUiLayoutManager.getSubCategories(selectedCategory)) {
            List<PathSequence> groupSequences = sequencesInCategory.stream()
                    .filter(sequence -> normalize(subCategory).equalsIgnoreCase(normalize(sequence.getSubCategory())))
                    .collect(Collectors.toList());
            addSequenceGroupRows(subCategory, subCategory, groupSequences, !searchActive);
            addedSubCategories.add(normalizeKey(subCategory));
        }

        Map<String, List<PathSequence>> extraGroups = new LinkedHashMap<>();
        List<PathSequence> ungroupedSequences = new ArrayList<>();
        for (PathSequence sequence : sequencesInCategory) {
            String subCategory = normalize(sequence.getSubCategory());
            if (subCategory.isEmpty()) {
                ungroupedSequences.add(sequence);
                continue;
            }
            if (!addedSubCategories.contains(normalizeKey(subCategory))) {
                extraGroups.computeIfAbsent(subCategory, key -> new ArrayList<>()).add(sequence);
            }
        }

        for (Map.Entry<String, List<PathSequence>> entry : extraGroups.entrySet()) {
            addSequenceGroupRows(entry.getKey(), entry.getKey(), entry.getValue(), true);
        }

        if (!ungroupedSequences.isEmpty()) {
            addSequenceGroupRows(UNGROUPED_SEQUENCE_TITLE, "", ungroupedSequences, true);
        }
    }

    private void addSequenceGroupRows(String title, String subCategory, List<PathSequence> sequences,
            boolean includeWhenEmpty) {
        if (!includeWhenEmpty && (sequences == null || sequences.isEmpty())) {
            return;
        }
        String groupKey = buildSequenceGroupKey(selectedCategory, subCategory);
        boolean collapsed = !isBlank(sequenceSearchQuery) ? false : isSequenceGroupCollapsed(groupKey);
        sequenceRows.add(SequenceListRow.header(title, groupKey, collapsed));
        if (!collapsed && sequences != null) {
            for (PathSequence sequence : sequences) {
                sequenceRows.add(SequenceListRow.sequence(sequence));
            }
        }
    }

    private void sortSequencesInCategory() {
        String sortMode = MainUiLayoutManager.getSortMode(selectedCategory);
        this.sequencesInCategory.sort((left, right) -> {
            int customCompare = Boolean.compare(right.isCustom(), left.isCustom());
            if (customCompare != 0) {
                return customCompare;
            }

            if (!left.isCustom() && !right.isCustom()) {
                return left.getName().compareToIgnoreCase(right.getName());
            }

            if (MainUiLayoutManager.SORT_ALPHABETICAL.equals(sortMode)) {
                return left.getName().compareToIgnoreCase(right.getName());
            }
            if (MainUiLayoutManager.SORT_LAST_OPENED.equals(sortMode)) {
                MainUiLayoutManager.SequenceOpenStats leftStats = MainUiLayoutManager.getSequenceStats(left.getName());
                MainUiLayoutManager.SequenceOpenStats rightStats = MainUiLayoutManager
                        .getSequenceStats(right.getName());
                int compare = Long.compare(rightStats.lastOpenedAt, leftStats.lastOpenedAt);
                return compare != 0 ? compare : left.getName().compareToIgnoreCase(right.getName());
            }
            if (MainUiLayoutManager.SORT_OPEN_COUNT.equals(sortMode)) {
                MainUiLayoutManager.SequenceOpenStats leftStats = MainUiLayoutManager.getSequenceStats(left.getName());
                MainUiLayoutManager.SequenceOpenStats rightStats = MainUiLayoutManager
                        .getSequenceStats(right.getName());
                int compare = Integer.compare(rightStats.openCount, leftStats.openCount);
                if (compare != 0) {
                    return compare;
                }
                compare = Long.compare(rightStats.lastOpenedAt, leftStats.lastOpenedAt);
                return compare != 0 ? compare : left.getName().compareToIgnoreCase(right.getName());
            }

            return 0;
        });
    }

    private int findSequenceIndex(PathSequence preferredSequence, String preferredSequenceName) {
        if (preferredSequence != null) {
            int index = sequencesInCategory.indexOf(preferredSequence);
            if (index >= 0) {
                return index;
            }
        }

        if (preferredSequenceName != null && !preferredSequenceName.trim().isEmpty()) {
            for (int i = 0; i < sequencesInCategory.size(); i++) {
                PathSequence candidate = sequencesInCategory.get(i);
                if (candidate != null && preferredSequenceName.equals(candidate.getName())) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean hasSequenceNameInEditor(PathSequence excludedSequence, String sequenceName) {
        if (sequenceName == null || sequenceName.trim().isEmpty()) {
            return false;
        }
        String normalizedName = sequenceName.trim();
        return allSequences.stream()
                .filter(sequence -> sequence != null && sequence != excludedSequence)
                .anyMatch(sequence -> normalizedName.equalsIgnoreCase(sequence.getName()));
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);

        if (!initialized || reloadFromManagerRequested) {
            reloadData();
            initialized = true;
            reloadFromManagerRequested = false;
        }
        isDirty = false;

        if (selectedSequenceIndex != -1 && selectedSequenceIndex < sequencesInCategory.size()) {
            this.selectedSequence = sequencesInCategory.get(selectedSequenceIndex);
            if (selectedStepIndex != -1 && selectedStepIndex < this.selectedSequence.getSteps().size()) {
                this.selectedStep = this.selectedSequence.getSteps().get(selectedStepIndex);
                if (selectedActionIndex != -1 && selectedActionIndex < this.selectedStep.getActions().size()) {
                    this.selectedAction = this.selectedStep.getActions().get(selectedActionIndex);
                } else {
                    selectAction(-1);
                }
            } else {
                selectStep(-1);
            }
        } else {
            selectSequence(-1);
        }

        recalculateEditorLayout();

        ensureSequenceSearchField();

        int manageButtonWidth = Math.max(s(54), Math.min(s(74), categoryListWidth - s(36)));
        btnManageCategories = new ThemedButton(0, categoryListX + categoryListWidth - manageButtonWidth - s(4),
                categoryListY + s(4), manageButtonWidth, s(18),
                "§b" + compactFooterToolLabel(I18n.format("gui.path.manager.manage_categories"), "分类",
                        manageButtonWidth));
        this.buttonList.add(btnManageCategories);

        int sequenceCardInnerX = sequenceListX + s(6);
        int sequenceCardInnerY = sequenceFooterCardY + s(8);
        int sequenceCardInnerWidth = sequenceListWidth - s(12);
        int sequenceButtonGap = s(6);
        int seqBtnWidth = Math.max(s(56), (sequenceCardInnerWidth - sequenceButtonGap) / 2);
        int seqBtnRow1Y = sequenceCardInnerY;
        int seqBtnRow2Y = seqBtnRow1Y + s(24);
        int seqBtnRow3Y = seqBtnRow2Y + s(24);

        btnAddSeq = new ThemedButton(1, sequenceCardInnerX, seqBtnRow1Y, seqBtnWidth, s(20),
                "§a" + I18n.format("gui.path.manager.add_seq"));
        btnCopySeq = new ThemedButton(4, sequenceCardInnerX + seqBtnWidth + sequenceButtonGap, seqBtnRow1Y, seqBtnWidth,
                s(20),
                "§b" + I18n.format("gui.path.manager.copy"));
        btnRenameSeq = new ThemedButton(5, sequenceCardInnerX, seqBtnRow2Y, seqBtnWidth, s(20),
                "§e" + I18n.format("gui.path.rename"));
        btnDeleteSeq = new ThemedButton(2, sequenceCardInnerX + seqBtnWidth + sequenceButtonGap, seqBtnRow2Y,
                seqBtnWidth,
                s(20),
                "§c" + I18n.format("gui.path.manager.delete"));
        btnMoveSeq = new ThemedButton(3, sequenceCardInnerX, seqBtnRow3Y, sequenceCardInnerWidth, s(20),
                "§e" + I18n.format("gui.path.manager.move_to_category"));
        btnActionTemplates = new ThemedButton(44, 0, 0, s(88), s(18), "§6动作模板");
        btnExecutionLogs = new ThemedButton(43, 0, 0, s(88), s(18), "§a执行日志");
        btnLegacyTriggerRules = new ThemedButton(40, 0, 0, s(98), s(18), "§d触发器规则");
        btnNodeEditor = new ThemedButton(34, 0, 0, s(96), s(18), "§b节点编辑区");
        btnToolbarPrev = new ThemedButton(47, 0, 0, s(18), s(18), "<");
        btnToolbarNext = new ThemedButton(48, 0, 0, s(18), s(18), ">");
        btnDebugPanel = new ThemedButton(49, 0, 0, s(106), s(18), "§b显示调试面板");

        this.buttonList.add(btnAddSeq);
        this.buttonList.add(btnCopySeq);
        this.buttonList.add(btnRenameSeq);
        this.buttonList.add(btnDeleteSeq);
        this.buttonList.add(btnMoveSeq);
        this.buttonList.add(btnActionTemplates);
        this.buttonList.add(btnExecutionLogs);
        this.buttonList.add(btnLegacyTriggerRules);
        this.buttonList.add(btnNodeEditor);
        this.buttonList.add(btnToolbarPrev);
        this.buttonList.add(btnToolbarNext);
        this.buttonList.add(btnDebugPanel);

        int stepCardInnerX = stepPaneX + s(6);
        int stepCardInnerWidth = stepPaneWidth - s(12);
        int stepButtonY = stepToolsCardY + s(7);
        int stepButtonGap = s(4);
        int stepButtonHeight = s(20);
        btnAddStep = new ThemedButton(10, stepCardInnerX, stepButtonY, s(40), stepButtonHeight,
                I18n.format("gui.path.manager.add"));
        btnDeleteStep = new ThemedButton(11, stepCardInnerX, stepButtonY, s(40), stepButtonHeight,
                I18n.format("gui.path.manager.delete"));
        btnCopyStep = new ThemedButton(19, stepCardInnerX, stepButtonY, s(40), stepButtonHeight,
                I18n.format("gui.path.manager.copy"));
        btnMoveStepUp = new ThemedButton(12, stepCardInnerX, stepButtonY, s(40), stepButtonHeight,
                I18n.format("gui.path.manager.move_up"));
        btnMoveStepDown = new ThemedButton(13, stepCardInnerX, stepButtonY, s(40), stepButtonHeight,
                I18n.format("gui.path.manager.move_down"));
        btnEditStepFailure = new ThemedButton(26, stepCardInnerX, stepButtonY, s(40), stepButtonHeight, "寻路重试");
        this.buttonList.add(btnAddStep);
        this.buttonList.add(btnDeleteStep);
        this.buttonList.add(btnCopyStep);
        this.buttonList.add(btnMoveStepUp);
        this.buttonList.add(btnMoveStepDown);
        this.buttonList.add(btnEditStepFailure);
        layoutUniformButtonGrid(
                Arrays.asList(btnAddStep, btnDeleteStep, btnCopyStep, btnMoveStepUp, btnMoveStepDown,
                        btnEditStepFailure),
                stepCardInnerX, stepButtonY, stepCardInnerWidth, getStepToolsColumnCount(), stepButtonGap,
                stepButtonGap,
                stepButtonHeight);

        gotoX = new GuiTextField(14, fontRenderer, stepCardInnerX, stepCoordsCardY + s(11), s(48), s(20));
        gotoY = new GuiTextField(15, fontRenderer, stepCardInnerX, stepCoordsCardY + s(11), s(48), s(20));
        gotoZ = new GuiTextField(16, fontRenderer, stepCardInnerX, stepCoordsCardY + s(11), s(48), s(20));
        btnGetCoords = new ThemedButton(17, stepCardInnerX, stepCoordsCardY + s(11), s(55), s(20),
                "§b" + I18n.format("gui.path.manager.get_coords"));
        btnClearCoords = new ThemedButton(18, stepCardInnerX, stepCoordsCardY + s(11), s(55), s(20),
                "§e" + I18n.format("gui.path.manager.clear_coords"));
        this.buttonList.add(btnGetCoords);
        this.buttonList.add(btnClearCoords);
        layoutCoordinateControls(stepCardInnerX, stepCoordsCardY, stepCardInnerWidth);

        int actionCardInnerX = actionPaneX + s(6);
        int actionCardInnerWidth = actionPaneWidth - s(12);
        int actionButtonGap = s(4);
        int actionButtonHeight = s(20);
        btnActionVariableManager = new ThemedButton(46, 0, 0, s(110), s(18), "§b动作全局变量");
        this.buttonList.add(btnActionVariableManager);

        int actionButtonY = actionToolsCardY + s(8);
        btnEditAction = new ThemedButton(20, actionCardInnerX, actionButtonY, s(56), actionButtonHeight,
                I18n.format("gui.path.manager.edit_action"));
        btnCopyAction = new ThemedButton(25, actionCardInnerX, actionButtonY, s(56), actionButtonHeight,
                I18n.format("gui.path.manager.copy"));
        btnAddAction = new ThemedButton(21, actionCardInnerX, actionButtonY, s(56), actionButtonHeight,
                I18n.format("gui.path.manager.add"));
        btnDeleteAction = new ThemedButton(22, actionCardInnerX, actionButtonY, s(56), actionButtonHeight,
                I18n.format("gui.path.manager.delete"));
        btnMoveActionUp = new ThemedButton(23, actionCardInnerX, actionButtonY, s(56), actionButtonHeight,
                I18n.format("gui.path.manager.move_up"));
        btnMoveActionDown = new ThemedButton(24, actionCardInnerX, actionButtonY, s(56), actionButtonHeight,
                I18n.format("gui.path.manager.move_down"));

        this.buttonList.add(btnEditAction);
        this.buttonList.add(btnCopyAction);
        this.buttonList.add(btnAddAction);
        this.buttonList.add(btnDeleteAction);
        this.buttonList.add(btnMoveActionUp);
        this.buttonList.add(btnMoveActionDown);
        layoutUniformButtonGrid(
                Arrays.asList(btnEditAction, btnCopyAction, btnAddAction, btnDeleteAction, btnMoveActionUp,
                        btnMoveActionDown),
                actionCardInnerX, actionButtonY, actionCardInnerWidth, getActionToolsColumnCount(), actionButtonGap,
                actionButtonGap, actionButtonHeight);

        int topButtonGap = s(5);
        int topToggleWidth = s(20);
        int topToggleReserve = topToggleWidth + s(8);
        int topControlWidth = Math.max(s(40), stepPaneWidth - s(12) - topToggleReserve);
        int topButtonWidth = Math.max(1, (topControlWidth - topButtonGap * 3) / 4);
        int topButtonY = stepHeaderCardY + s(8);
        int secondTopButtonY = topButtonY + s(25);

        btnCloseGui = new ThemedButton(30, stepPaneX + s(6), topButtonY, topButtonWidth, s(20), "");
        btnLoopDelay = new ThemedButton(32, stepPaneX + s(6) + topButtonWidth + topButtonGap, topButtonY,
                topButtonWidth, s(20), "");
        btnSetNote = new ThemedButton(33, stepPaneX + s(6) + 2 * (topButtonWidth + topButtonGap), topButtonY,
                topButtonWidth, s(20),
                "");
        btnSingleExec = new ThemedButton(31, stepPaneX + s(6) + 3 * (topButtonWidth + topButtonGap), topButtonY,
                topButtonWidth, s(20),
                "");
        btnNonInterruptingExecution = new ThemedButton(35, stepPaneX + s(6), secondTopButtonY,
                topButtonWidth * 2 + topButtonGap, s(20), "");
        btnLockConflictPolicy = new ThemedButton(41, stepPaneX + s(6) + topButtonWidth * 2 + topButtonGap * 2,
                secondTopButtonY,
                topButtonWidth * 2, s(20), "");
        btnDebugPauseResume = new ThemedButton(36, 0, 0, s(84), s(20), "");
        btnDebugClearTrace = new ThemedButton(37, 0, 0, s(84), s(20), "§7清空追踪");
        btnDebugStep = new ThemedButton(38, 0, 0, s(84), s(20), "§b单步");
        btnToggleActionBreakpoint = new ThemedButton(39, 0, 0, s(84), s(20), "§e切换动作断点");
        btnToggleStepHeader = new ThemedButton(45, stepPaneX + s(6) + topControlWidth + s(4), topButtonY,
                topToggleWidth,
                s(20), "");

        this.buttonList.add(btnCloseGui);
        this.buttonList.add(btnLoopDelay);
        this.buttonList.add(btnSetNote);
        this.buttonList.add(btnSingleExec);
        this.buttonList.add(btnNonInterruptingExecution);
        this.buttonList.add(btnLockConflictPolicy);
        this.buttonList.add(btnDebugPauseResume);
        this.buttonList.add(btnDebugClearTrace);
        this.buttonList.add(btnDebugStep);
        this.buttonList.add(btnToggleActionBreakpoint);
        this.buttonList.add(btnToggleStepHeader);

        int saveCardInnerX = actionPaneX + s(6);
        int saveButtonY = actionSaveCardY + s(8);
        int saveButtonGap = s(4);
        int saveButtonHeight = s(20);
        GuiButton discardButton = new ThemedButton(101, saveCardInnerX, saveButtonY, s(64), saveButtonHeight,
                I18n.format("gui.path.manager.discard_changes"));
        GuiButton saveButton = new ThemedButton(100, saveCardInnerX, saveButtonY, s(64), saveButtonHeight,
                "§a" + I18n.format("gui.path.manager.save_close"));
        this.buttonList.add(discardButton);
        this.buttonList.add(saveButton);
        layoutUniformButtonGrid(Arrays.asList(discardButton, saveButton), saveCardInnerX, saveButtonY,
                actionCardInnerWidth,
                getSaveCardColumnCount(), saveButtonGap, saveButtonGap, saveButtonHeight);

        layoutHeaderToolbarButtons();
        applyPendingFocus();
        updateButtonStates();
    }

    private void applyPendingFocus() {
        if (pendingFocusCategory != null && !pendingFocusCategory.trim().isEmpty()) {
            selectedCategory = pendingFocusCategory;
            if (!this.categories.contains(selectedCategory)) {
                selectedCategory = defaultCategory();
            }
            filterSequencesByCategory();
            pendingFocusCategory = null;
        }

        if (pendingFocusSequenceName == null || pendingFocusSequenceName.trim().isEmpty()) {
            return;
        }

        for (int i = 0; i < sequencesInCategory.size(); i++) {
            PathSequence sequence = sequencesInCategory.get(i);
            if (sequence != null && pendingFocusSequenceName.equals(sequence.getName())) {
                selectSequence(i);
                int rowIndex = findSequenceRowIndex(sequence);
                int visibleItemCount = getSequenceVisibleItemCount();
                int targetIndex = rowIndex >= 0 ? rowIndex : i;
                sequenceScrollOffset = MathHelper.clamp(targetIndex - visibleItemCount / 2, 0, Math.max(0,
                        sequenceRows.size() - visibleItemCount));
                break;
            }
        }
        pendingFocusSequenceName = null;
    }

    private void updateButtonStates() {
        boolean sequenceSelected = selectedSequence != null;
        boolean isCustomSequence = sequenceSelected && selectedSequence.isCustom();

        btnCloseGui.visible = sequenceSelected && stepHeaderExpanded;
        btnSingleExec.visible = sequenceSelected && stepHeaderExpanded;
        btnLoopDelay.visible = sequenceSelected && stepHeaderExpanded;
        btnSetNote.visible = sequenceSelected && stepHeaderExpanded;
        btnNonInterruptingExecution.visible = sequenceSelected && stepHeaderExpanded;
        btnLockConflictPolicy.visible = sequenceSelected && stepHeaderExpanded;
        boolean debugButtonsVisible = debugMonitorVisible && !debugMonitorMinimized;
        btnDebugPauseResume.visible = debugButtonsVisible;
        btnDebugClearTrace.visible = debugButtonsVisible;
        btnDebugStep.visible = debugButtonsVisible;
        btnToggleActionBreakpoint.visible = debugButtonsVisible;
        btnToggleStepHeader.visible = sequenceSelected;
        btnLoopDelay.enabled = isCustomSequence;
        btnSetNote.enabled = isCustomSequence;
        btnNonInterruptingExecution.enabled = isCustomSequence;
        btnLockConflictPolicy.enabled = isCustomSequence;
        if (btnToggleStepHeader != null) {
            btnToggleStepHeader.displayString = stepHeaderExpanded ? "▲" : "▼";
        }
        if (btnDebugPanel != null) {
            btnDebugPanel.displayString = debugMonitorVisible ? "§c隐藏调试面板" : "§b显示调试面板";
            btnDebugPanel.enabled = true;
        }

        if (sequenceSelected) {
            String yes = "§a" + I18n.format("gui.path.manager.yes");
            String no = "§c" + I18n.format("gui.path.manager.no");
            btnCloseGui.displayString = I18n.format("gui.path.manager.close_menu",
                    selectedSequence.shouldCloseGuiAfterStart() ? yes : no);
            btnSingleExec.displayString = I18n.format("gui.path.manager.single_exec",
                    selectedSequence.isSingleExecution() ? yes : no);
            btnLoopDelay.displayString = I18n.format("gui.path.manager.loop_delay",
                    selectedSequence.getLoopDelayTicks());
            btnSetNote.displayString = I18n.format("gui.path.manager.set_note");
            btnNonInterruptingExecution.displayString = "不打断其他序列: "
                    + (selectedSequence.isNonInterruptingExecution() ? yes : no);
            btnLockConflictPolicy.displayString = "锁冲突: "
                    + lockConflictPolicyToDisplay(selectedSequence.getLockConflictPolicy());
        }

        PathSequenceEventListener.DebugSnapshot debugSnapshot = PathSequenceEventListener.instance.getDebugSnapshot();
        boolean debuggingSelectedSequence = sequenceSelected
                && debugSnapshot.isTracking()
                && selectedSequence.getName().equals(debugSnapshot.getSequenceName());
        if (btnDebugPauseResume != null) {
            if (debuggingSelectedSequence) {
                btnDebugPauseResume.displayString = debugSnapshot.isPaused() ? "§a继续调试" : "§e调试暂停";
            } else {
                btnDebugPauseResume.displayString = "§7调试暂停";
            }
            btnDebugPauseResume.enabled = debuggingSelectedSequence;
        }
        if (btnDebugClearTrace != null) {
            btnDebugClearTrace.enabled = debugButtonsVisible;
        }
        if (btnDebugStep != null) {
            btnDebugStep.enabled = debuggingSelectedSequence && debugSnapshot.isPausedForDebug();
        }
        if (btnToggleActionBreakpoint != null) {
            boolean actionSelected = selectedSequence != null && selectedStep != null && selectedAction != null
                    && selectedActionIndex >= 0 && selectedStepIndex >= 0;
            btnToggleActionBreakpoint.enabled = actionSelected;
            if (actionSelected) {
                boolean hasBreakpoint = PathSequenceEventListener.hasDebugBreakpoint(selectedSequence.getName(),
                        selectedStepIndex, selectedActionIndex);
                btnToggleActionBreakpoint.displayString = hasBreakpoint ? "§c移除动作断点" : "§e切换动作断点";
            } else {
                btnToggleActionBreakpoint.displayString = "§7切换动作断点";
            }
        }

        btnDeleteSeq.enabled = isCustomSequence;
        btnMoveSeq.enabled = isCustomSequence;
        btnCopySeq.enabled = sequenceSelected;
        btnRenameSeq.enabled = isCustomSequence;

        btnAddStep.enabled = sequenceSelected;

        boolean stepSelected = selectedStepIndex != -1;
        btnDeleteStep.enabled = sequenceSelected && stepSelected;
        btnCopyStep.enabled = sequenceSelected && stepSelected;
        btnMoveStepUp.enabled = sequenceSelected && stepSelected && selectedStepIndex > 0;
        btnMoveStepDown.enabled = sequenceSelected && stepSelected
                && selectedStepIndex < selectedSequence.getSteps().size() - 1;
        btnEditStepFailure.enabled = sequenceSelected && stepSelected;

        gotoX.setEnabled(stepSelected);
        gotoY.setEnabled(stepSelected);
        gotoZ.setEnabled(stepSelected);
        btnGetCoords.enabled = stepSelected;
        btnClearCoords.enabled = stepSelected;

        btnAddAction.enabled = stepSelected;
        boolean actionSelected = selectedActionIndex != -1;
        btnEditAction.enabled = stepSelected && actionSelected;
        btnCopyAction.enabled = stepSelected && actionSelected;
        btnDeleteAction.enabled = stepSelected && actionSelected;
        btnMoveActionUp.enabled = stepSelected && actionSelected && selectedActionIndex > 0;
        btnMoveActionDown.enabled = stepSelected && actionSelected
                && selectedActionIndex < selectedStep.getActions().size() - 1;
        layoutDebugMonitorButtons();
        layoutHeaderToolbarButtons();
    }

    private String nextLockConflictPolicy(String current) {
        String normalized = current == null ? "" : current.trim().toUpperCase();
        if ("FAIL".equals(normalized)) {
            return "PREEMPT_BACKGROUND";
        }
        if ("PREEMPT_BACKGROUND".equals(normalized)) {
            return "WAIT";
        }
        return "FAIL";
    }

    private String lockConflictPolicyToDisplay(String policy) {
        String normalized = policy == null ? "" : policy.trim().toUpperCase();
        if ("FAIL".equals(normalized)) {
            return "失败退出";
        }
        if ("PREEMPT_BACKGROUND".equals(normalized)) {
            return "抢占后台";
        }
        return "等待";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        // ... (case 0 to 5, and case 10 to 18, case 30 to 32, case 100 to 101 保持不变)
        switch (button.id) {
            case 0: // 管理分类
                mc.displayGuiScreen(new GuiCategoryManager(this));
                break;
            case 1: // 新增序列
                mc.displayGuiScreen(
                        new GuiTextInput(this, I18n.format("gui.path.manager.input_new_sequence"), newName -> {
                            if (newName != null && !newName.trim().isEmpty()
                                    && allSequences.stream().noneMatch(s -> s.getName().equalsIgnoreCase(newName))) {
                                pushUndoHistory("add-sequence");
                                PathSequence newSequence = new PathSequence(newName.trim());
                                newSequence.setCustom(true);
                                newSequence.setCategory(selectedCategory);
                                newSequence.setSubCategory(selectedSubCategory);
                                if (!isBlank(selectedSubCategory)) {
                                    MainUiLayoutManager.addSubCategory(selectedCategory, selectedSubCategory);
                                }
                                this.allSequences.add(0, newSequence);
                                filterSequencesByCategory();
                                selectSequence(sequencesInCategory.indexOf(newSequence));
                            }
                            mc.displayGuiScreen(this);
                        }));
                break;
            case 2: // 删除序列
                if (btnDeleteSeq.enabled) {
                    pushUndoHistory("delete-sequence");
                    this.allSequences.remove(selectedSequence);
                    filterSequencesByCategory();
                }
                break;
            case 3: // 移动序列
                if (btnMoveSeq.enabled) {
                    mc.displayGuiScreen(new GuiCategorySelect(this, selectedCategory, newCategory -> {
                        pushUndoHistory("move-sequence");
                        selectedSequence.setCategory(newCategory);
                        if (!isBlank(selectedSequence.getSubCategory())) {
                            MainUiLayoutManager.addSubCategory(newCategory, selectedSequence.getSubCategory());
                        }
                        filterSequencesByCategory();
                        mc.displayGuiScreen(this);
                    }));
                }
                break;
            case 4: // 复制序列
                if (btnCopySeq.enabled) {
                    final PathSequence sourceSequence = selectedSequence;
                    String baseName = selectedSequence.getName();
                    String newName = baseName + " " + I18n.format("gui.path.manager.copy_suffix");
                    int copyCount = 2;
                    while (hasSequenceNameInEditor(null, newName)) {
                        newName = baseName + " " + I18n.format("gui.path.manager.copy_suffix_num", copyCount++);
                    }
                    final String resolvedName = newName;

                    mc.displayGuiScreen(new GuiCategorySelect(this, builtinCategory(), newCategory -> {
                        pushUndoHistory("copy-sequence");
                        PathSequence copiedSequence = new PathSequence(sourceSequence);
                        copiedSequence.setName(resolvedName);
                        copiedSequence.setCustom(true);
                        copiedSequence.setCategory(newCategory);
                        if (!isBlank(copiedSequence.getSubCategory())) {
                            MainUiLayoutManager.addSubCategory(newCategory, copiedSequence.getSubCategory());
                        }

                        this.allSequences.add(0, copiedSequence);
                        selectedCategory = newCategory;
                        filterSequencesByCategory();
                        selectSequence(sequencesInCategory.indexOf(copiedSequence));
                        mc.displayGuiScreen(this);
                    }));
                }
                break;
            case 5: // 重命名序列
                if (btnRenameSeq.enabled) {
                    final PathSequence renameTarget = selectedSequence;
                    String oldName = renameTarget.getName();
                    mc.displayGuiScreen(new GuiTextInput(this, I18n.format("gui.path.manager.input_new_name"), oldName,
                            newName -> {
                                String trimmedNewName = newName == null ? "" : newName.trim();
                            if (!trimmedNewName.isEmpty() && !trimmedNewName.equals(oldName)
                                        && !hasSequenceNameInEditor(renameTarget, trimmedNewName)) {
                                    pushUndoHistory("rename-sequence");
                                    renameTarget.setName(trimmedNewName);
                                    PathSequenceManager.notifyCustomSequenceRenamed(oldName, trimmedNewName,
                                            renameTarget);
                                    filterSequencesByCategory();
                                }
                                mc.displayGuiScreen(this);
                            }));
                }
                break;
            case 10: // 添加步骤
                if (btnAddStep.enabled) {
                    pushUndoHistory("add-step");
                    double[] pos = captureCurrentSafeGotoPoint();
                    selectedSequence.getSteps().add(new PathStep(pos));
                    selectStep(selectedSequence.getSteps().size() - 1);
                }
                break;
            case 11:
                if (btnDeleteStep.enabled) {
                    pushUndoHistory("delete-step");
                    deleteSelectedSteps();
                }
                break;
            case 19: // 复制步骤
                if (btnCopyStep.enabled) {
                    pushUndoHistory("copy-step");
                    PathStep copiedStep = new PathStep(selectedStep);
                    int newStepIndex = selectedStepIndex + 1;
                    selectedSequence.getSteps().add(newStepIndex, copiedStep);
                    selectStep(newStepIndex);
                }
                break;
            case 12:
                if (btnMoveStepUp.enabled) {
                    pushUndoHistory("move-step-up");
                    Collections.swap(selectedSequence.getSteps(), selectedStepIndex, selectedStepIndex - 1);
                    selectStep(selectedStepIndex - 1);
                }
                break;
            case 13:
                if (btnMoveStepDown.enabled) {
                    pushUndoHistory("move-step-down");
                    Collections.swap(selectedSequence.getSteps(), selectedStepIndex, selectedStepIndex + 1);
                    selectStep(selectedStepIndex + 1);
                }
                break;
            case 26:
                if (btnEditStepFailure.enabled && selectedStep != null) {
                    mc.displayGuiScreen(new GuiStepFailureEditor(this, getSelectedStepTargets(), selectedStep));
                }
                break;
            case 17: // 获取坐标
                if (btnGetCoords.enabled && mc.player != null) {
                    pushUndoHistory("capture-goto");
                    double[] newPos = captureCurrentSafeGotoPoint();
                    selectedStep.setGotoPoint(newPos);
                    updateGotoFields();
                }
                break;
            case 18: // 清空坐标
                if (btnClearCoords.enabled) {
                    pushUndoHistory("clear-goto");
                    selectedStep.setGotoPoint(new double[] { Double.NaN, Double.NaN, Double.NaN });
                    updateGotoFields();
                }
                break;
            case 20: // 编辑动作
                if (btnEditAction.enabled) {
                    final PathStep editingStep = selectedStep;
                    final int editingActionIndex = selectedActionIndex;
                    final ActionData editingAction = selectedAction;
                    mc.displayGuiScreen(new GuiActionEditor(this, editingAction, sa -> {
                        if (editingStep != null
                                && editingActionIndex >= 0
                                && editingActionIndex < editingStep.getActions().size()) {
                            pushUndoHistory("edit-action");
                            String oldUuid = PathSequenceManager.getPersistentActionUuid(editingAction);
                            String newUuid = PathSequenceManager.getPersistentActionUuid(sa);
                            if (!oldUuid.isEmpty() && !oldUuid.equals(newUuid)) {
                                PathSequenceManager.removePersistentActionRecord(editingAction);
                            }
                            editingStep.getActions().set(editingActionIndex, sa);
                            selectAction(editingActionIndex);
                        }
                    }, selectedSequence != null ? selectedSequence.getName() : null));
                }
                break;
            case 25: // 复制动作
                if (btnCopyAction.enabled) {
                    pushUndoHistory("copy-action");
                    ActionData copiedAction = new ActionData(selectedAction);
                    int newActionIndex = selectedActionIndex + 1;
                    selectedStep.getActions().add(newActionIndex, copiedAction);
                    selectAction(newActionIndex);
                }
                break;
            case 21: // 添加动作
                if (btnAddAction.enabled)
                    mc.displayGuiScreen(new GuiActionEditor(this, null, sa -> {
                        pushUndoHistory("add-action");
                        selectedStep.getActions().add(sa);
                        selectAction(selectedStep.getActions().size() - 1);
                    }, selectedSequence != null ? selectedSequence.getName() : null));
                break;
            case 22:
                if (btnDeleteAction.enabled) {
                    pushUndoHistory("delete-action");
                    deleteSelectedActions();
                }
                break;
            case 23:
                if (btnMoveActionUp.enabled) {
                    pushUndoHistory("move-action-up");
                    Collections.swap(selectedStep.getActions(), selectedActionIndex, selectedActionIndex - 1);
                    selectAction(selectedActionIndex - 1);
                }
                break;
            case 24:
                if (btnMoveActionDown.enabled) {
                    pushUndoHistory("move-action-down");
                    Collections.swap(selectedStep.getActions(), selectedActionIndex, selectedActionIndex + 1);
                    selectAction(selectedActionIndex + 1);
                }
                break;
            case 30:
                if (btnCloseGui.visible) {
                    pushUndoHistory("toggle-close-gui");
                    selectedSequence.setCloseGuiAfterStart(!selectedSequence.shouldCloseGuiAfterStart());
                }
                break;
            case 31:
                if (btnSingleExec.visible) {
                    pushUndoHistory("toggle-single-exec");
                    selectedSequence.setSingleExecution(!selectedSequence.isSingleExecution());
                }
                break;
            case 32:
                if (btnLoopDelay.enabled) {
                    mc.displayGuiScreen(new GuiTextInput(this, I18n.format("gui.path.manager.input_loop_delay"),
                            String.valueOf(selectedSequence.getLoopDelayTicks()), newTicksStr -> {
                                try {
                                    pushUndoHistory("loop-delay");
                                    int newTicks = Integer.parseInt(newTicksStr);
                                    selectedSequence.setLoopDelayTicks(Math.max(0, newTicks));
                                } catch (NumberFormatException e) {
                                }
                                mc.displayGuiScreen(this);
                            }));
                }
                break;
            case 33:
                if (btnSetNote.enabled) {
                    mc.displayGuiScreen(new GuiTextInput(this, I18n.format("gui.path.manager.input_note"),
                            selectedSequence.getNote(), newNote -> {
                                pushUndoHistory("sequence-note");
                                selectedSequence.setNote(newNote == null ? "" : newNote);
                                mc.displayGuiScreen(this);
                            }));
                }
                break;
            case 35:
                if (btnNonInterruptingExecution.enabled) {
                    pushUndoHistory("toggle-non-interrupting");
                    selectedSequence.setNonInterruptingExecution(!selectedSequence.isNonInterruptingExecution());
                }
                break;
            case 41:
                if (btnLockConflictPolicy.enabled) {
                    pushUndoHistory("lock-policy");
                    selectedSequence.setLockConflictPolicy(
                            nextLockConflictPolicy(selectedSequence.getLockConflictPolicy()));
                }
                break;
            case 36:
                if (btnDebugPauseResume.enabled) {
                    PathSequenceEventListener.DebugSnapshot debugSnapshot = PathSequenceEventListener.instance
                            .getDebugSnapshot();
                    if (debugSnapshot.isPaused()) {
                        PathSequenceEventListener.instance.resumeFromDebug();
                    } else {
                        PathSequenceEventListener.instance.pauseForDebug();
                    }
                }
                break;
            case 37:
                PathSequenceEventListener.instance.clearDebugTrace();
                break;
            case 38:
                if (btnDebugStep.enabled) {
                    PathSequenceEventListener.instance.requestDebugStep();
                }
                break;
            case 39:
                if (btnToggleActionBreakpoint.enabled && selectedSequence != null && selectedStep != null
                        && selectedAction != null && selectedStepIndex >= 0 && selectedActionIndex >= 0) {
                    PathSequenceEventListener.toggleDebugBreakpoint(selectedSequence.getName(), selectedStepIndex,
                            selectedActionIndex);
                }
                break;
            case 45:
                stepHeaderExpanded = !stepHeaderExpanded;
                initGui();
                break;
            case 40:
                mc.displayGuiScreen(new GuiLegacySequenceTriggerRules(this));
                break;
            case 43:
                mc.displayGuiScreen(new GuiExecutionLogViewer(this));
                break;
            case 44:
                mc.displayGuiScreen(new GuiActionTemplateLibrary(this, selectedSequence, selectedStep, selectedStepIndex));
                break;
            case 46:
                mc.displayGuiScreen(new GuiActionVariableManager(this, this.allSequences));
                break;
            case 34:
                mc.displayGuiScreen(new GuiNodeEditor(this));
                break;
            case 47:
                headerToolbarScrollIndex = Math.max(0, headerToolbarScrollIndex - 1);
                layoutHeaderToolbarButtons();
                break;
            case 48:
                headerToolbarScrollIndex = Math.min(headerToolbarMaxScrollIndex, headerToolbarScrollIndex + 1);
                layoutHeaderToolbarButtons();
                break;
            case 49:
                debugMonitorVisible = !debugMonitorVisible;
                if (debugMonitorVisible) {
                    ensureDebugMonitorWindowBounds();
                } else {
                    draggingDebugMonitor = false;
                }
                persistDebugMonitorLayout();
                break;
            case 100: // 保存
                List<PathConfigValidator.Issue> issues = PathConfigValidator.validateSequences(this.allSequences);
                List<PathConfigValidator.Issue> promptIssues = GuiPathValidationReport.filterIssuesForPrompt(issues);
                if (promptIssues.isEmpty()) {
                    PathSequenceManager.saveAllSequences(this.allSequences);
                    this.mc.displayGuiScreen(null);
                } else {
                    this.mc.displayGuiScreen(new GuiPathValidationReport(this, "保存前配置检查", "忽略并保存",
                            promptIssues, () -> {
                                PathSequenceManager.saveAllSequences(this.allSequences);
                                this.mc.displayGuiScreen(null);
                            }));
                }
                break;
            case 101:
                this.mc.displayGuiScreen(null);
                break;
        }
        updateButtonStates();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        hoveredSequenceIndex = -1;
        hoveredSequenceNote = null;
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "", this.fontRenderer);
        this.drawString(this.fontRenderer, I18n.format("gui.path.manager.title"), panelX + s(12), panelY + 6,
                0xFFF5FBFF);
        drawEditorChrome(mouseX, mouseY);

        drawCategoryList(mouseX, mouseY);
        drawSequenceList(mouseX, mouseY);
        drawPathDebugPanel(mouseX, mouseY);
        drawStepList(mouseX, mouseY);
        drawActionList(mouseX, mouseY);

        if (selectedStep != null) {
            this.drawString(fontRenderer, "GOTO X:", gotoX.x, gotoX.y - 12, 0xFFFFFF);
            drawThemedTextField(gotoX);
            this.drawString(fontRenderer, "Y:", gotoY.x, gotoY.y - 12, 0xFFFFFF);
            drawThemedTextField(gotoY);
            this.drawString(fontRenderer, "Z:", gotoZ.x, gotoZ.y - 12, 0xFFFFFF);
            drawThemedTextField(gotoZ);
        }

        if (debugMonitorVisible) {
            layoutDebugMonitorButtons();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (debugMonitorVisible) {
            drawDebugMonitorWindow(mouseX, mouseY, partialTicks);
        }

        if (stepNotePopupVisible) {
            drawStepNotePopup(mouseX, mouseY);
            return;
        }

        if (actionDetailPopupAction != null) {
            drawActionDetailPopup(mouseX, mouseY);
            return;
        }

        if (debugMonitorVisible && debugMonitorBounds != null && debugMonitorBounds.contains(mouseX, mouseY)) {
            return;
        }

        if (hoveredSequenceIndex >= 0 && hoveredSequenceNote != null && !hoveredSequenceNote.trim().isEmpty()) {
            drawHoveringText(Collections.singletonList("备注: " + hoveredSequenceNote.trim()), mouseX, mouseY);
            return;
        }

        int hoveredStepNoteIndex = getVisibleStepNoteIndexAt(mouseX, mouseY);
        if (hoveredStepNoteIndex >= 0) {
            List<String> noteTooltip = buildStepNoteTooltipLines(hoveredStepNoteIndex);
            if (!noteTooltip.isEmpty()) {
                drawHoveringText(noteTooltip, mouseX, mouseY);
                return;
            }
        }

        List<String> clipboardHint = getClipboardHoverHint(mouseX, mouseY);
        if (clipboardHint != null && !clipboardHint.isEmpty()) {
            drawHoveringText(clipboardHint, mouseX, mouseY);
            return;
        }

        List<String> headerTooltip = getHeaderButtonTooltip(mouseX, mouseY);
        if (headerTooltip != null && !headerTooltip.isEmpty()) {
            drawHoveringText(headerTooltip, mouseX, mouseY);
            return;
        }

        List<String> bottomTooltip = getBottomButtonTooltip(mouseX, mouseY);
        if (bottomTooltip != null && !bottomTooltip.isEmpty()) {
            drawHoveringText(bottomTooltip, mouseX, mouseY);
            return;
        }

        String dividerTooltip = getDividerTooltip(mouseX, mouseY);
        if (dividerTooltip != null && !dividerTooltip.isEmpty()) {
            drawHoveringText(Collections.singletonList(dividerTooltip), mouseX, mouseY);
        }
    }

    private void drawEditorChrome(int mouseX, int mouseY) {
        drawPaneShell(categoryListX, categoryListY, categoryListWidth, categoryListHeight, 0xFF57A7D4);
        drawPaneShell(sequenceListX, sequenceListY, sequenceListWidth, sequenceListHeight, 0xFF5F8EC8);
        drawPaneShell(stepPaneX, stepPaneY, stepPaneWidth, stepPaneHeight, 0xFF67B4E1);
        drawPaneShell(actionPaneX, actionPaneY, actionPaneWidth, actionPaneHeight, 0xFF6E95D7);

        drawCardShell(sequenceListX + s(4), sequenceFooterCardY, sequenceListWidth - s(8), sequenceFooterCardHeight,
                0xFF4A739B, mouseX, mouseY);
        drawCardShell(stepPaneX + s(4), stepHeaderCardY, stepPaneWidth - s(8), stepHeaderCardHeight, 0xFF507DA3,
                mouseX, mouseY);
        drawCardShell(stepPaneX + s(4), stepToolsCardY, stepPaneWidth - s(8), stepToolsCardHeight, 0xFF4C7399, mouseX,
                mouseY);
        drawCardShell(stepPaneX + s(4), stepCoordsCardY, stepPaneWidth - s(8), stepCoordsCardHeight, 0xFF4C7399, mouseX,
                mouseY);
        drawCardShell(actionPaneX + s(4), actionToolsCardY, actionPaneWidth - s(8), actionToolsCardHeight, 0xFF507DA3,
                mouseX, mouseY);
        drawCardShell(actionPaneX + s(4), actionSaveCardY, actionPaneWidth - s(8), actionSaveCardHeight, 0xFF4E7BA0,
                mouseX,
                mouseY);

        if (headerToolbarViewportBounds != null && headerToolbarViewportBounds.width > 0) {
            drawRect(headerToolbarViewportBounds.x - 4, headerToolbarViewportBounds.y - 1,
                    headerToolbarViewportBounds.x + headerToolbarViewportBounds.width + 4,
                    headerToolbarViewportBounds.y + headerToolbarViewportBounds.height + 1, 0x33101820);
        }

        drawDividerHandle(leftDividerBounds, mouseX, mouseY, draggingLeftDivider, false);
        drawDividerHandle(rightDividerBounds, mouseX, mouseY, draggingRightDivider, false);
        drawDividerHandle(categoryDividerBounds, mouseX, mouseY, draggingCategoryDivider, true);

        drawString(fontRenderer, "步骤列表", stepPaneX + s(8), stepPaneY + s(6), 0xFFEAF7FF);
        drawString(fontRenderer, "动作列表(右键详情)", actionPaneX + s(8), actionPaneY + s(6), 0xFFEAF7FF);
    }

    private void drawPaneShell(int x, int y, int width, int height, int accentColor) {
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, accentColor);
        drawRect(x, y, x + width, y + height, 0x8F16232F);
        drawRect(x + 1, y + 1, x + width - 1, y + 22, 0x2AFFFFFF);
        drawRect(x + 1, y + 22, x + width - 1, y + height - 1, 0x44101820);
    }

    private void drawCardShell(int x, int y, int width, int height, int accentColor, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        int border = hovered ? 0xFF8FD8FF : accentColor;
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, border);
        drawRect(x, y, x + width, y + height, 0xAA121B24);
        drawRect(x + 1, y + 1, x + width - 1, y + 2, 0x24FFFFFF);
    }

    private void drawDividerHandle(Rectangle bounds, int mouseX, int mouseY, boolean dragging, boolean horizontal) {
        if (bounds == null) {
            return;
        }
        boolean hovered = bounds.contains(mouseX, mouseY);
        int accent = dragging ? 0xFF7CD9FF : (hovered ? 0xFF63BFEF : 0xFF3E617A);
        int actualX = bounds.x + Math.max(0, (bounds.width - LAYOUT_DIVIDER_WIDTH) / 2);
        int actualY = bounds.y + Math.max(0, (bounds.height - LAYOUT_DIVIDER_WIDTH) / 2);
        if (hovered || dragging) {
            drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0x35111922);
        }
        if (horizontal) {
            drawRect(bounds.x, actualY, bounds.x + bounds.width, actualY + LAYOUT_DIVIDER_WIDTH, 0x77111922);
            int centerX = bounds.x + bounds.width / 2 - 12;
            for (int i = 0; i < 4; i++) {
                drawRect(centerX + i * 7, actualY + 3, centerX + i * 7 + 2, actualY + LAYOUT_DIVIDER_WIDTH - 3,
                        accent);
            }
            return;
        }
        drawRect(actualX, bounds.y, actualX + LAYOUT_DIVIDER_WIDTH, bounds.y + bounds.height, 0x77111922);
        drawRect(actualX + 5, bounds.y + 18, actualX + 7, bounds.y + bounds.height - 18, accent);
        int centerY = bounds.y + bounds.height / 2 - 12;
        for (int i = 0; i < 4; i++) {
            drawRect(actualX + 3, centerY + i * 7, actualX + LAYOUT_DIVIDER_WIDTH - 3, centerY + i * 7 + 2, accent);
        }
    }

    private String getDividerTooltip(int mouseX, int mouseY) {
        if (leftDividerBounds != null && leftDividerBounds.contains(mouseX, mouseY)) {
            return "拖动调整左侧分类/序列区与中间步骤区宽度";
        }
        if (rightDividerBounds != null && rightDividerBounds.contains(mouseX, mouseY)) {
            return "拖动调整中间步骤区与右侧动作区宽度";
        }
        if (categoryDividerBounds != null && categoryDividerBounds.contains(mouseX, mouseY)) {
            return "拖动调整分类导航与序列列表高度";
        }
        if (headerToolbarViewportBounds != null && headerToolbarViewportBounds.contains(mouseX, mouseY)
                && headerToolbarMaxScrollIndex > 0) {
            return "滚轮或左右箭头可横向浏览顶部工具条";
        }
        return null;
    }

    private String trimDisplayTextWithEllipsis(String text, int maxWidth) {
        String safe = safeText(text);
        if (safe.isEmpty()) {
            return "";
        }
        if (fontRenderer.getStringWidth(safe) <= maxWidth) {
            return safe;
        }
        int ellipsisWidth = fontRenderer.getStringWidth("...");
        return fontRenderer.trimStringToWidth(safe, Math.max(20, maxWidth - ellipsisWidth)) + "...";
    }

    private List<String> buildActionDetailLines(ActionData action) {
        List<String> lines = new ArrayList<>();
        if (action == null) {
            return lines;
        }
        lines.add("§b类型: §f" + safeText(action.type));
        lines.add("§b描述: §f" + safeText(action.getDescription()));
        if (action.params == null || action.params.entrySet().isEmpty()) {
            lines.add("§7参数: (空)");
            return lines;
        }
        lines.add("§e参数:");
        for (Map.Entry<String, com.google.gson.JsonElement> entry : action.params.entrySet()) {
            lines.add("§7- §f" + entry.getKey() + "§7: §f"
                    + safeText(entry.getValue() == null ? "" : entry.getValue().toString()));
        }
        return lines;
    }

    private void drawActionDetailPopup(int mouseX, int mouseY) {
        List<String> rawLines = buildActionDetailLines(actionDetailPopupAction);
        int popupWidth = Math.min(Math.max(260, actionPaneWidth + stepPaneWidth / 2), this.width - 60);
        int maxContentHeight = Math.max(120, this.height - 140);
        List<String> wrapped = new ArrayList<>();
        for (String line : rawLines) {
            List<String> lineParts = this.fontRenderer.listFormattedStringToWidth(line, popupWidth - 28);
            if (lineParts == null || lineParts.isEmpty()) {
                wrapped.add(line);
            } else {
                wrapped.addAll(lineParts);
            }
        }
        int maxLines = Math.max(6, maxContentHeight / (this.fontRenderer.FONT_HEIGHT + 2));
        if (wrapped.size() > maxLines) {
            wrapped = new ArrayList<>(wrapped.subList(0, maxLines - 1));
            wrapped.add("§7...");
        }
        int popupHeight = Math.min(maxContentHeight, 36 + wrapped.size() * (this.fontRenderer.FONT_HEIGHT + 2));
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;
        actionDetailPopupBounds = new Rectangle(popupX, popupY, popupWidth, popupHeight);

        drawRect(0, 0, this.width, this.height, 0x7A000000);
        drawCardShell(popupX, popupY, popupWidth, popupHeight, 0xFF6AA7D9, mouseX, mouseY);
        drawString(fontRenderer, "动作详情 #" + (actionDetailPopupIndex + 1), popupX + 10, popupY + 8, 0xFFFFFFFF);
        drawString(fontRenderer, "右键动作行打开，Esc / 点击外部关闭", popupX + 10, popupY + 20, 0xFF9FC5E3);

        int drawY = popupY + 38;
        for (String line : wrapped) {
            drawString(fontRenderer, line, popupX + 10, drawY, 0xFFFFFFFF);
            drawY += this.fontRenderer.FONT_HEIGHT + 2;
        }
    }

    private void ensureDebugMonitorWindowBounds() {
        int headerHeight = s(22);
        int compactWidth = Math.max(s(116),
                (fontRenderer == null ? 0 : fontRenderer.getStringWidth("调试面板")) + s(52));
        int defaultExpandedWidth = Math.min(Math.max(s(268), stepPaneWidth + Math.max(0, actionPaneWidth / 5)),
                panelWidth - s(24));
        int defaultExpandedHeight = Math.min(Math.max(s(208), stepPaneHeight / 2), panelHeight - s(34));
        if (debugMonitorPreferredWidth <= 0) {
            debugMonitorPreferredWidth = defaultExpandedWidth;
        }
        if (debugMonitorPreferredHeight <= 0) {
            debugMonitorPreferredHeight = defaultExpandedHeight;
        }
        debugMonitorWidth = debugMonitorMinimized
                ? compactWidth
                : MathHelper.clamp(debugMonitorPreferredWidth, Math.max(s(220), compactWidth), panelWidth - s(24));
        debugMonitorHeight = debugMonitorMinimized
                ? headerHeight + s(6)
                : MathHelper.clamp(debugMonitorPreferredHeight, Math.max(s(156), headerHeight + s(64)),
                        panelHeight - s(34));

        int minX = panelX + s(8);
        int minY = panelY + s(24);
        int maxX = panelX + panelWidth - debugMonitorWidth - s(8);
        int maxY = panelY + panelHeight - debugMonitorHeight - s(8);
        if (debugMonitorX == Integer.MIN_VALUE || debugMonitorY == Integer.MIN_VALUE) {
            debugMonitorX = Math.max(minX, actionPaneX + actionPaneWidth - debugMonitorWidth - s(10));
            debugMonitorY = Math.max(minY, stepPaneY + s(16));
        }

        debugMonitorX = MathHelper.clamp(debugMonitorX, minX, Math.max(minX, maxX));
        debugMonitorY = MathHelper.clamp(debugMonitorY, minY, Math.max(minY, maxY));
        debugMonitorBounds = new Rectangle(debugMonitorX, debugMonitorY, debugMonitorWidth, debugMonitorHeight);
        debugMonitorHeaderBounds = new Rectangle(debugMonitorX + 1, debugMonitorY + 1, debugMonitorWidth - 2,
                headerHeight);
        int controlSize = Math.max(12, headerHeight - 6);
        debugMonitorCloseBounds = new Rectangle(debugMonitorX + debugMonitorWidth - controlSize - s(4),
                debugMonitorY + 3,
                controlSize, controlSize);
        debugMonitorMinimizeBounds = new Rectangle(debugMonitorCloseBounds.x - controlSize - s(3), debugMonitorY + 3,
                controlSize, controlSize);
        int contentTop = debugMonitorHeaderBounds.y + debugMonitorHeaderBounds.height + s(6);
        int contentBottom = debugMonitorY + debugMonitorHeight - s(8) - getDebugMonitorButtonAreaHeight() - s(10);
        debugMonitorContentBounds = new Rectangle(debugMonitorX + s(8), contentTop, Math.max(s(96), debugMonitorWidth - s(22)),
                Math.max(s(36), contentBottom - contentTop));
        debugMonitorResizeRightBounds = new Rectangle(debugMonitorX + debugMonitorWidth - s(6), debugMonitorY + headerHeight,
                s(6), Math.max(s(20), debugMonitorHeight - headerHeight - s(12)));
        debugMonitorResizeBottomBounds = new Rectangle(debugMonitorX + s(8), debugMonitorY + debugMonitorHeight - s(6),
                Math.max(s(20), debugMonitorWidth - s(14)), s(6));
        debugMonitorResizeCornerBounds = new Rectangle(debugMonitorX + debugMonitorWidth - s(10),
                debugMonitorY + debugMonitorHeight - s(10), s(10), s(10));
    }

    private List<String> buildDebugMonitorLines() {
        List<String> lines = new ArrayList<>();
        PathSequenceEventListener.DebugSnapshot snapshot = PathSequenceEventListener.instance.getDebugSnapshot();
        String selectedSequenceName = selectedSequence == null ? "" : safeText(selectedSequence.getName());
        boolean sameSequence = selectedSequence != null
                && snapshot.isTracking()
                && selectedSequence.getName().equals(snapshot.getSequenceName());

        lines.add(selectedSequence == null ? "§7当前未选中序列" : "§b当前选中: §f" + selectedSequenceName);

        if (!snapshot.isTracking()) {
            lines.add("§7当前没有前台调试中的序列。");
            if (selectedAction != null && selectedStepIndex >= 0 && selectedActionIndex >= 0
                    && selectedSequence != null) {
                boolean hasBreakpoint = PathSequenceEventListener.hasDebugBreakpoint(selectedSequence.getName(),
                        selectedStepIndex, selectedActionIndex);
                lines.add(hasBreakpoint ? "§c当前动作已设置断点" : "§7当前动作未设置断点");
            }
            lines.add("§7提示: 可先运行一个序列，再从这里观察状态、轨迹和变量。");
            return lines;
        }

        lines.add("§b前台追踪: §f" + safeText(snapshot.getSequenceName()));
        if (!sameSequence && selectedSequence != null) {
            lines.add("§e提示: 当前追踪对象不是你选中的序列。");
        }
        lines.add("§b状态: §f" + safeText(snapshot.getStatus()));
        lines.add("§b步骤: §e" + snapshot.getStepIndex() + " §7| §b动作: §e" + snapshot.getActionIndex()
                + " §7| §b暂停: " + (snapshot.isPaused() ? "§e是" : "§a否"));

        String actionText = snapshot.getCurrentActionDescription().isEmpty()
                ? "§7当前动作: (无)"
                : "§b当前动作: §f" + snapshot.getCurrentActionDescription();
        lines.add(actionText);

        if (selectedAction != null && selectedStepIndex >= 0 && selectedActionIndex >= 0 && selectedSequence != null) {
            boolean hasBreakpoint = PathSequenceEventListener.hasDebugBreakpoint(selectedSequence.getName(),
                    selectedStepIndex, selectedActionIndex);
            lines.add(hasBreakpoint ? "§c当前选中动作断点: 已启用" : "§7当前选中动作断点: 未启用");
        }

        List<String> traceLines = snapshot.getTraceLines();
        if (traceLines == null || traceLines.isEmpty()) {
            lines.add("§7最近轨迹: (无)");
        } else {
            lines.add("§e最近轨迹:");
            int start = Math.max(0, traceLines.size() - 4);
            for (int i = start; i < traceLines.size(); i++) {
                lines.add("§7- §f" + safeText(traceLines.get(i)));
            }
        }

        Map<String, String> variables = snapshot.getVariablePreview();
        if (variables == null || variables.isEmpty()) {
            lines.add("§7变量预览: (空)");
        } else {
            lines.add("§e变量预览:");
            int count = 0;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                lines.add("§7- §f" + entry.getKey() + "§7=§f" + safeText(entry.getValue()));
                if (++count >= 6) {
                    break;
                }
            }
        }

        lines.add(formatLockPreview());
        lines.add("§7拖动标题栏可移动窗口，右上角可收起/关闭。");
        return lines;
    }

    private void drawDebugMonitorWindow(int mouseX, int mouseY, float partialTicks) {
        ensureDebugMonitorWindowBounds();
        layoutDebugMonitorButtons();
        drawCardShell(debugMonitorX, debugMonitorY, debugMonitorWidth, debugMonitorHeight, 0xFF5C8FBD, mouseX, mouseY);
        drawRect(debugMonitorX + 1, debugMonitorY + 1, debugMonitorX + debugMonitorWidth - 1,
                debugMonitorHeaderBounds.y + debugMonitorHeaderBounds.height, 0x99303F51);
        drawString(fontRenderer, "调试面板", debugMonitorX + s(8), debugMonitorY + s(7), 0xFFF4FAFF);

        boolean hoverMinimize = debugMonitorMinimizeBounds != null
                && debugMonitorMinimizeBounds.contains(mouseX, mouseY);
        boolean hoverClose = debugMonitorCloseBounds != null && debugMonitorCloseBounds.contains(mouseX, mouseY);
        if (debugMonitorMinimizeBounds != null) {
            GuiTheme.drawButtonFrameSafe(debugMonitorMinimizeBounds.x, debugMonitorMinimizeBounds.y,
                    debugMonitorMinimizeBounds.width, debugMonitorMinimizeBounds.height,
                    hoverMinimize ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, debugMonitorMinimized ? "+" : "-", debugMonitorMinimizeBounds.x
                    + debugMonitorMinimizeBounds.width / 2,
                    debugMonitorMinimizeBounds.y + (debugMonitorMinimizeBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                    0xFFFFFFFF);
        }
        if (debugMonitorCloseBounds != null) {
            GuiTheme.drawButtonFrameSafe(debugMonitorCloseBounds.x, debugMonitorCloseBounds.y,
                    debugMonitorCloseBounds.width, debugMonitorCloseBounds.height,
                    hoverClose ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, "x", debugMonitorCloseBounds.x + debugMonitorCloseBounds.width / 2,
                    debugMonitorCloseBounds.y + (debugMonitorCloseBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                    0xFFFFFFFF);
        }

        if (debugMonitorMinimized) {
            return;
        }

        int contentX = debugMonitorContentBounds.x;
        int contentY = debugMonitorContentBounds.y;
        int contentWidth = Math.max(s(100), debugMonitorContentBounds.width - s(8));
        int footerTop = debugMonitorY + debugMonitorHeight - s(8) - getDebugMonitorButtonAreaHeight();
        drawRect(debugMonitorX + s(8), footerTop - s(6), debugMonitorX + debugMonitorWidth - s(8), footerTop - s(5),
                0x335C8FBD);
        List<String> wrappedLines = new ArrayList<>();
        for (String line : buildDebugMonitorLines()) {
            List<String> split = fontRenderer.listFormattedStringToWidth(line, Math.max(s(100), contentWidth));
            if (split == null || split.isEmpty()) {
                wrappedLines.add(line);
            } else {
                wrappedLines.addAll(split);
            }
        }

        int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        int maxVisibleLines = Math.max(1, debugMonitorContentBounds.height / lineHeight);
        debugMonitorMaxScroll = Math.max(0, wrappedLines.size() - maxVisibleLines);
        debugMonitorScrollOffset = MathHelper.clamp(debugMonitorScrollOffset, 0, debugMonitorMaxScroll);

        int drawY = contentY;
        for (int i = debugMonitorScrollOffset; i < wrappedLines.size() && i < debugMonitorScrollOffset + maxVisibleLines; i++) {
            drawString(fontRenderer, wrappedLines.get(i), contentX, drawY, 0xFFFFFFFF);
            drawY += lineHeight;
        }
        drawScrollbar(debugMonitorX + debugMonitorWidth - s(10), debugMonitorContentBounds.y, debugMonitorContentBounds.height,
                debugMonitorScrollOffset, debugMonitorMaxScroll, Math.max(1, wrappedLines.size()), maxVisibleLines);
        drawRect(debugMonitorX + debugMonitorWidth - s(12), debugMonitorY + debugMonitorHeight - s(4),
                debugMonitorX + debugMonitorWidth - s(4), debugMonitorY + debugMonitorHeight - s(2), 0xFF6EAED9);
        drawRect(debugMonitorX + debugMonitorWidth - s(8), debugMonitorY + debugMonitorHeight - s(8),
                debugMonitorX + debugMonitorWidth - s(6), debugMonitorY + debugMonitorHeight - s(4), 0xFF6EAED9);

        for (GuiButton button : Arrays.asList(
                btnDebugPauseResume, btnDebugClearTrace, btnDebugStep, btnToggleActionBreakpoint)) {
            if (button != null && button.visible) {
                button.drawButton(this.mc, mouseX, mouseY, partialTicks);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (sequenceSearchField != null) {
            sequenceSearchField.updateCursorCounter();
        }
        if (stepNotePopupVisible && stepNotePopupField != null) {
            stepNotePopupField.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        persistDebugMonitorLayout();
        super.onGuiClosed();
    }

    private void drawScrollbar(int x, int y, int height, int scrollOffset, int maxScroll, int totalItems,
            int visibleItems) {
        if (maxScroll > 0) {
            int thumbHeight = Math.max(10, (int) ((float) visibleItems / totalItems * height));
            int thumbY = y + (int) ((float) scrollOffset / maxScroll * (height - thumbHeight));
            GuiTheme.drawScrollbar(x, y, 6, height, thumbY, thumbHeight);
        }
    }

    private void drawPathDebugPanel(int mouseX, int mouseY) {
        if (!stepHeaderExpanded) {
            int collapsedX = stepPaneX + s(4);
            int collapsedY = stepHeaderCardY;
            int collapsedHeight = getCollapsedStepHeaderHeight();
            int collapsedWidth = stepPaneWidth - s(8);
            drawRect(collapsedX, collapsedY, collapsedX + collapsedWidth, collapsedY + collapsedHeight, 0x55324458);
            GuiTheme.drawInputFrameSafe(collapsedX, collapsedY, collapsedWidth, collapsedHeight, false, true);
            String text = selectedSequence == null
                    ? "§7请选择序列后展开顶部控制"
                    : "§7顶部控制已隐藏，点击右上角 ▼ 展开执行设置";
            this.drawString(fontRenderer, trimDisplayTextWithEllipsis(text, collapsedWidth - 34), collapsedX + 6,
                    collapsedY + 8,
                    0xFFFFFFFF);
        }
    }

    private String formatVariablePreview(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return "§7变量预览: (空)";
        }
        StringBuilder builder = new StringBuilder("§7变量预览: §f");
        int count = 0;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (count > 0) {
                builder.append(" §7| §f");
            }
            builder.append(entry.getKey()).append('=').append(safeText(entry.getValue()));
            if (++count >= 3) {
                break;
            }
        }
        return builder.toString();
    }

    private String formatLockPreview() {
        List<ResourceLockManager.LockSnapshot> snapshots = ResourceLockManager.getSnapshots();
        if (snapshots == null || snapshots.isEmpty()) {
            return "§7资源锁: (空)";
        }
        StringBuilder builder = new StringBuilder("§7资源锁: §f");
        int count = 0;
        for (ResourceLockManager.LockSnapshot snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            if (count > 0) {
                builder.append(" §7| §f");
            }
            builder.append(snapshot.getResource().name().toLowerCase())
                    .append('@')
                    .append(safeText(
                            snapshot.getSequenceName().isEmpty() ? snapshot.getOwnerId() : snapshot.getSequenceName()));
            if (snapshot.isBackground()) {
                builder.append("(bg)");
            }
            if (++count >= 3) {
                break;
            }
        }
        return builder.toString();
    }

    private String trimDisplayText(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        return this.fontRenderer.trimStringToWidth(text, Math.max(20, maxWidth));
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<String> getHeaderButtonTooltip(int mouseX, int mouseY) {
        if (btnToggleStepHeader != null && btnToggleStepHeader.visible
                && isHoveringButton(btnToggleStepHeader, mouseX, mouseY)) {
            return Collections.singletonList(stepHeaderExpanded ? "收起顶部控制区" : "展开顶部控制区");
        }
        if (!stepHeaderExpanded) {
            return null;
        }
        if (btnCloseGui != null && btnCloseGui.visible && isHoveringButton(btnCloseGui, mouseX, mouseY)) {
            return Arrays.asList("关闭菜单", "启动序列后是否自动关闭当前菜单。");
        }
        if (btnSingleExec != null && btnSingleExec.visible && isHoveringButton(btnSingleExec, mouseX, mouseY)) {
            return Arrays.asList("单次执行", "开启后本序列只执行一轮，不按全局循环次数继续。");
        }
        if (btnLoopDelay != null && btnLoopDelay.visible && isHoveringButton(btnLoopDelay, mouseX, mouseY)) {
            return Arrays.asList("循环延迟", "设置每轮序列结束到下一轮开始之间的延迟 tick。");
        }
        if (btnSetNote != null && btnSetNote.visible && isHoveringButton(btnSetNote, mouseX, mouseY)) {
            return Arrays.asList("设置备注", "给当前序列写备注，主界面悬浮卡片时会显示。");
        }
        if (btnNonInterruptingExecution != null && btnNonInterruptingExecution.visible
                && isHoveringButton(btnNonInterruptingExecution, mouseX, mouseY)) {
            return Arrays.asList("不打断其他序列", "开启后当前序列会按后台序列方式运行，不会顶掉正在前台执行的其他序列。");
        }
        if (btnLockConflictPolicy != null && btnLockConflictPolicy.visible
                && isHoveringButton(btnLockConflictPolicy, mouseX, mouseY)) {
            return Arrays.asList("锁冲突", "等待: 冲突时等待资源释放", "失败退出: 冲突立即停止", "抢占后台: 仅前台序列可抢占后台持有的资源");
        }
        if (btnDebugPauseResume != null && btnDebugPauseResume.visible
                && isHoveringButton(btnDebugPauseResume, mouseX, mouseY)) {
            return Arrays.asList("调试暂停/继续", "暂停或继续当前选中序列的调试执行。");
        }
        if (btnDebugClearTrace != null && btnDebugClearTrace.visible
                && isHoveringButton(btnDebugClearTrace, mouseX, mouseY)) {
            return Arrays.asList("清空追踪", "清空调试面板中的最近轨迹记录。");
        }
        if (btnDebugStep != null && btnDebugStep.visible && isHoveringButton(btnDebugStep, mouseX, mouseY)) {
            return Arrays.asList("单步", "在断点暂停时只继续执行一步，然后再次暂停。");
        }
        if (btnToggleActionBreakpoint != null && btnToggleActionBreakpoint.visible
                && isHoveringButton(btnToggleActionBreakpoint, mouseX, mouseY)) {
            return Arrays.asList("动作断点", "给当前选中的动作添加或移除断点。");
        }
        return null;
    }

    private List<String> getBottomButtonTooltip(int mouseX, int mouseY) {
        List<String> tooltip = tooltipForButton(btnManageCategories, mouseX, mouseY,
                "分类管理",
                "管理路径分类与子分类。",
                "可用于新增、重命名、删除和整理分类结构。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnAddSeq, mouseX, mouseY,
                "新增序列",
                "在当前分类或子分类下创建一个新的路径序列。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnCopySeq, mouseX, mouseY,
                "复制序列",
                "复制当前选中的路径序列，包含其中的步骤与动作。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnRenameSeq, mouseX, mouseY,
                "重命名序列",
                "修改当前选中序列的名称。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnDeleteSeq, mouseX, mouseY,
                "删除序列",
                "删除当前选中的路径序列。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnMoveSeq, mouseX, mouseY,
                "移动到分类",
                "把当前自定义序列移动到其他分类或子分类。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnActionTemplates, mouseX, mouseY,
                "动作模板",
                "打开内置动作模板库，查看备注、使用场景与动作链。",
                "可把模板动作填入当前步骤，或作为新步骤加入当前序列。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnExecutionLogs, mouseX, mouseY,
                "执行日志",
                "查看路径序列的运行记录、步骤轨迹与执行结果。",
                "适合排查为什么没执行、执行到哪一步、哪里失败。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnDebugPanel, mouseX, mouseY,
                "调试面板",
                "在编辑器内显示或隐藏可拖动的调试悬浮窗口。",
                "窗口支持拖动、缩小和关闭，不再固定占用中间顶部区域。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnLegacyTriggerRules, mouseX, mouseY,
                "触发器规则",
                "配置事件触发后自动运行路径序列。",
                "例如聊天、界面、Boss血条、定时器、站立不动等事件都能触发。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnNodeEditor, mouseX, mouseY,
                "节点编辑器",
                "打开节点式路径编辑器。",
                "适合做更复杂的条件、变量和流程编排。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnActionVariableManager, mouseX, mouseY,
                "动作全局变量",
                "查看和管理动作变量注册表。",
                "可重命名或删除变量，方便整理旧变量和排查冲突。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnAddStep, mouseX, mouseY,
                "新增步骤",
                "在当前序列末尾添加一个新步骤。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnDeleteStep, mouseX, mouseY,
                "删除步骤",
                "删除当前选中的步骤，支持多选后批量删除。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnCopyStep, mouseX, mouseY,
                "复制步骤",
                "复制当前步骤及其动作列表。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnMoveStepUp, mouseX, mouseY,
                "步骤上移",
                "把当前步骤向前移动一位。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnMoveStepDown, mouseX, mouseY,
                "步骤下移",
                "把当前步骤向后移动一位。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnEditStepFailure, mouseX, mouseY,
                "寻路重试",
                "配置当前步骤或多选步骤的寻路失败重试与后续处理逻辑。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnGetCoords, mouseX, mouseY,
                "获取坐标",
                "把你当前所在位置填入 GOTO X / Y / Z。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnClearCoords, mouseX, mouseY,
                "清空坐标",
                "清空当前步骤的 GOTO 坐标。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnEditAction, mouseX, mouseY,
                "编辑动作",
                "编辑当前选中动作的参数与行为。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnCopyAction, mouseX, mouseY,
                "复制动作",
                "复制当前选中的动作。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnAddAction, mouseX, mouseY,
                "新增动作",
                "给当前步骤添加一个新动作。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnDeleteAction, mouseX, mouseY,
                "删除动作",
                "删除当前选中的动作。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnMoveActionUp, mouseX, mouseY,
                "动作上移",
                "把当前动作向前移动一位。");
        if (tooltip != null) {
            return tooltip;
        }
        tooltip = tooltipForButton(btnMoveActionDown, mouseX, mouseY,
                "动作下移",
                "把当前动作向后移动一位。");
        if (tooltip != null) {
            return tooltip;
        }

        GuiButton discardButton = findButtonById(101);
        tooltip = tooltipForButton(discardButton, mouseX, mouseY,
                "放弃修改",
                "关闭编辑器并丢弃本次未保存的更改。");
        if (tooltip != null) {
            return tooltip;
        }
        GuiButton saveButton = findButtonById(100);
        return tooltipForButton(saveButton, mouseX, mouseY,
                "保存并关闭",
                "先校验当前配置，再保存所有路径序列并关闭编辑器。");
    }

    private List<String> tooltipForButton(GuiButton button, int mouseX, int mouseY, String... lines) {
        if (button == null || lines == null || lines.length == 0 || !isHoveringButton(button, mouseX, mouseY)) {
            return null;
        }
        return Arrays.asList(lines);
    }

    private GuiButton findButtonById(int id) {
        for (GuiButton button : this.buttonList) {
            if (button != null && button.id == id) {
                return button;
            }
        }
        return null;
    }

    private boolean isHoveringButton(GuiButton button, int mouseX, int mouseY) {
        return button != null && button.visible
                && mouseX >= button.x && mouseX <= button.x + button.width
                && mouseY >= button.y && mouseY <= button.y + button.height;
    }

    private int getNavigationItemHeight() {
        return s(20);
    }

    private int getStepActionCardItemHeight() {
        return s(STEP_ACTION_CARD_BASE_HEIGHT);
    }

    private int getCategoryHeaderHeight() {
        return s(26);
    }

    private int getCollapsedCategoryPanelHeight() {
        return getCategoryHeaderHeight() + s(8);
    }

    private int getCategoryListTop() {
        return categoryListY + getCategoryHeaderHeight();
    }

    private int getCategoryListContentHeight() {
        return Math.max(getNavigationItemHeight(), categoryListHeight - getCategoryHeaderHeight() - s(6));
    }

    private int getSequenceSearchX() {
        return sequenceListX + s(4);
    }

    private int getSequenceSearchY() {
        return sequenceListY + s(20);
    }

    private int getSequenceSearchWidth() {
        return Math.max(s(40), sequenceListWidth - s(10));
    }

    private int getSequenceSearchHeight() {
        return s(18);
    }

    private int getSequenceListTop() {
        return getSequenceSearchY() + getSequenceSearchHeight() + s(6);
    }

    private int getSequenceListContentHeight() {
        return Math.max(getNavigationItemHeight(), sequenceFooterCardY - getSequenceListTop() - s(6));
    }

    private int getSequenceVisibleItemCount() {
        return Math.max(1, getSequenceListContentHeight() / getNavigationItemHeight());
    }

    private void ensureSequenceSearchField() {
        int fieldX = getSequenceSearchX();
        int fieldY = getSequenceSearchY();
        int fieldWidth = getSequenceSearchWidth();
        int fieldHeight = getSequenceSearchHeight();
        if (sequenceSearchField == null) {
            sequenceSearchField = new GuiTextField(SEQUENCE_SEARCH_FIELD_ID, this.fontRenderer, fieldX, fieldY,
                    fieldWidth, fieldHeight);
            sequenceSearchField.setMaxStringLength(120);
            sequenceSearchField.setCanLoseFocus(true);
            sequenceSearchField.setText(sequenceSearchQuery);
        } else {
            sequenceSearchField.x = fieldX;
            sequenceSearchField.y = fieldY;
            sequenceSearchField.width = fieldWidth;
            sequenceSearchField.height = fieldHeight;
            if (!sequenceSearchQuery.equals(sequenceSearchField.getText())) {
                sequenceSearchField.setText(sequenceSearchQuery);
            }
        }
    }

    private String getSequenceDisplayName(PathSequence sequence) {
        if (sequence == null) {
            return "";
        }
        String displayName = normalize(sequence.getName());
        if (!sequence.isCustom()) {
            displayName = I18n.format("gui.path.builtin_name", displayName,
                    I18n.format("gui.path.manager.builtin_suffix"));
        }
        return displayName;
    }

    private List<CategoryTreeRow> buildVisibleCategoryRows() {
        List<CategoryTreeRow> rows = new ArrayList<>();
        for (String category : categories) {
            boolean customCategory = isCustomCategory(category);
            rows.add(new CategoryTreeRow(category, "", customCategory));
            if (customCategory && !MainUiLayoutManager.isCollapsed(category)) {
                for (String subCategory : MainUiLayoutManager.getSubCategories(category)) {
                    rows.add(new CategoryTreeRow(category, subCategory, true));
                }
            }
        }
        return rows;
    }

    private GuiTheme.UiState resolveNavigationState(boolean selected, boolean hovered) {
        if (selected) {
            return GuiTheme.UiState.SELECTED;
        }
        return hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
    }

    private int withAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private void drawEditorListCard(Rectangle bounds, boolean selected, boolean hovered, int accentColor) {
        GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                resolveNavigationState(selected, hovered));
        int outerFill = selected ? 0x5C314A68 : (hovered ? 0x4732465D : 0x36283849);
        int innerFill = selected ? 0x7A355273 : (hovered ? 0x56374D65 : 0x442E4154);
        int accentFill = selected ? accentColor : (hovered ? withAlpha(accentColor, 185) : withAlpha(accentColor, 125));
        drawRect(bounds.x + 1, bounds.y + 1, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1, outerFill);
        drawRect(bounds.x + 1, bounds.y + 1, bounds.x + 5, bounds.y + bounds.height - 1, accentFill);
        drawRect(bounds.x + 6, bounds.y + 2, bounds.x + bounds.width - 2, bounds.y + bounds.height - 2, innerFill);
        drawRect(bounds.x + 6, bounds.y + 2, bounds.x + bounds.width - 2, bounds.y + 3,
                hovered || selected ? 0x35FFFFFF : 0x18FFFFFF);
    }

    private String getActionTypeLabel(ActionData action) {
        String type = action == null ? "" : normalize(action.type);
        if (type.isEmpty()) {
            return "动作";
        }
        String[] parts = type.split("[_\\-\\s]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.length() == 0 ? type.toUpperCase(Locale.ROOT) : builder.toString();
    }

    private String getActionCardDetail(ActionData action, boolean hasBreakpoint) {
        String detail = action == null ? "" : normalize(action.getDescription());
        detail = TextFormatting.getTextWithoutFormattingCodes(detail);
        if (detail.isEmpty()) {
            detail = "当前动作无额外说明";
        }
        return hasBreakpoint ? "断点已启用 | " + detail : detail;
    }

    private String getStepNoteCompat(PathStep step) {
        if (step == null) {
            return "";
        }
        try {
            if (cachedPathStepGetNoteMethod == null) {
                cachedPathStepGetNoteMethod = step.getClass().getMethod("getNote");
                cachedPathStepGetNoteMethod.setAccessible(true);
            }
            Object value = cachedPathStepGetNoteMethod.invoke(step);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void setStepNoteCompat(PathStep step, String note) {
        if (step == null) {
            return;
        }
        try {
            if (cachedPathStepSetNoteMethod == null) {
                cachedPathStepSetNoteMethod = step.getClass().getMethod("setNote", String.class);
                cachedPathStepSetNoteMethod.setAccessible(true);
            }
            cachedPathStepSetNoteMethod.invoke(step, note == null ? "" : note);
        } catch (Exception ignored) {
        }
    }

    private void drawCategoryList(int mouseX, int mouseY) {
        visibleCategoryRows.clear();
        categoryPanelToggleBounds = new Rectangle(categoryListX + s(4), categoryListY + s(4), s(18), s(18));

        drawRect(categoryListX, categoryListY, categoryListX + categoryListWidth, categoryListY + categoryListHeight,
                0x66324458);
        GuiTheme.drawInputFrameSafe(categoryListX, categoryListY, categoryListWidth, categoryListHeight, false, true);

        boolean hoveredToggle = categoryPanelToggleBounds.contains(mouseX, mouseY);
        GuiTheme.drawButtonFrameSafe(categoryPanelToggleBounds.x, categoryPanelToggleBounds.y,
                categoryPanelToggleBounds.width, categoryPanelToggleBounds.height,
                hoveredToggle ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawCenteredString(fontRenderer, categoryPanelCollapsed ? "▼" : "▲",
                categoryPanelToggleBounds.x + categoryPanelToggleBounds.width / 2,
                categoryPanelToggleBounds.y
                        + (categoryPanelToggleBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                0xFFFFFFFF);

        int titleX = categoryPanelToggleBounds.x + categoryPanelToggleBounds.width + s(6);
        drawString(fontRenderer, "分类导航", titleX, categoryListY + s(6), 0xFFEAF7FF);

        if (categoryPanelCollapsed) {
            maxCategoryScroll = 0;
            return;
        }

        List<CategoryTreeRow> allRows = buildVisibleCategoryRows();
        int itemHeight = getNavigationItemHeight();
        int listTop = getCategoryListTop();
        int listContentHeight = getCategoryListContentHeight();
        int visibleItemCount = Math.max(1, listContentHeight / itemHeight);
        maxCategoryScroll = Math.max(0, allRows.size() - visibleItemCount);
        categoryScrollOffset = MathHelper.clamp(categoryScrollOffset, 0, maxCategoryScroll);

        drawRect(categoryListX + 2, listTop, categoryListX + categoryListWidth - 2, listTop + listContentHeight,
                0x22293A4D);

        for (int i = 0; i < visibleItemCount; i++) {
            int index = i + categoryScrollOffset;
            if (index >= allRows.size()) {
                break;
            }
            CategoryTreeRow row = allRows.get(index);
            int itemY = listTop + i * itemHeight;
            int indent = row.isSubCategory() ? s(12) : 0;
            int rowX = categoryListX + 4 + indent;
            int rowWidth = categoryListWidth - s(12) - indent;
            Rectangle bounds = new Rectangle(rowX, itemY + 1, rowWidth, itemHeight - 2);
            row.bounds = bounds;
            row.toggleBounds = null;
            visibleCategoryRows.add(row);

            boolean selected = row.category.equals(selectedCategory)
                    && (row.isSubCategory()
                            ? normalize(row.subCategory).equalsIgnoreCase(normalize(selectedSubCategory))
                            : isBlank(selectedSubCategory));
            boolean hovered = bounds.contains(mouseX, mouseY);
            GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                    resolveNavigationState(selected, hovered));

            int labelX = bounds.x + s(6);
            if (row.isCustomRoot()) {
                row.toggleBounds = new Rectangle(bounds.x + s(3), bounds.y + 2, s(12), Math.max(8, bounds.height - 4));
                drawString(fontRenderer, MainUiLayoutManager.isCollapsed(row.category) ? ">" : "v",
                        row.toggleBounds.x + 1, itemY + (itemHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFB8CCE0);
                labelX += s(10);
            }

            String label = row.isSubCategory() ? row.subCategory : row.category;
            drawString(fontRenderer,
                    trimDisplayText(label, Math.max(s(20), bounds.width - (labelX - bounds.x) - s(4))),
                    labelX, itemY + (itemHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        drawScrollbar(categoryListX + categoryListWidth - 7, listTop, listContentHeight, categoryScrollOffset,
                maxCategoryScroll, allRows.size(), visibleItemCount);
    }

    private void drawSequenceList(int mouseX, int mouseY) {
        drawRect(sequenceListX, sequenceListY, sequenceListX + sequenceListWidth, sequenceListY + sequenceListHeight,
                0x66324458);
        GuiTheme.drawInputFrameSafe(sequenceListX, sequenceListY, sequenceListWidth, sequenceListHeight, false, true);

        drawString(fontRenderer, "序列列表", sequenceListX + s(6), sequenceListY + s(6), 0xFFEAF7FF);
        String statusText = isRootCategoryMode()
                ? "按子分类分组"
                : (isBlank(selectedSubCategory) ? "当前分类视图" : "当前子分类视图");
        drawString(fontRenderer, trimDisplayText(statusText, sequenceListWidth - s(16)), sequenceListX + s(58),
                sequenceListY + s(6), 0xFFB8C7D9);

        ensureSequenceSearchField();
        sequenceSearchClearBounds = null;
        drawThemedTextField(sequenceSearchField);
        if (sequenceSearchField.getText().trim().isEmpty() && !sequenceSearchField.isFocused()) {
            drawString(fontRenderer, SEQUENCE_SEARCH_PLACEHOLDER, sequenceSearchField.x + s(4),
                    sequenceSearchField.y + (sequenceSearchField.height - fontRenderer.FONT_HEIGHT) / 2, 0xFF8A96A8);
        }
        if (!isBlank(sequenceSearchQuery)) {
            int clearSize = Math.max(12, sequenceSearchField.height - 4);
            int clearX = sequenceSearchField.x + sequenceSearchField.width - clearSize - 3;
            int clearY = sequenceSearchField.y + (sequenceSearchField.height - clearSize) / 2;
            sequenceSearchClearBounds = new Rectangle(clearX, clearY, clearSize, clearSize);
            boolean hoveredClear = sequenceSearchClearBounds.contains(mouseX, mouseY);
            drawRect(clearX, clearY, clearX + clearSize, clearY + clearSize, hoveredClear ? 0x99556F84 : 0x663C5366);
            drawCenteredString(fontRenderer, "x", clearX + clearSize / 2,
                    clearY + (clearSize - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        int itemHeight = getNavigationItemHeight();
        int listTop = getSequenceListTop();
        int listContentHeight = getSequenceListContentHeight();
        int visibleItemCount = Math.max(1, listContentHeight / itemHeight);
        maxSequenceScroll = Math.max(0, sequenceRows.size() - visibleItemCount);
        sequenceScrollOffset = MathHelper.clamp(sequenceScrollOffset, 0, maxSequenceScroll);

        drawRect(sequenceListX + 2, listTop, sequenceListX + sequenceListWidth - 2, listTop + listContentHeight,
                0x22293A4D);

        if (sequenceRows.isEmpty()) {
            String emptyText = isBlank(sequenceSearchQuery)
                    ? (isBlank(selectedSubCategory) ? "当前分类下还没有路径序列" : "当前子分类下还没有路径序列")
                    : "没有匹配当前搜索条件的路径序列";
            drawCenteredString(fontRenderer, emptyText, sequenceListX + sequenceListWidth / 2,
                    listTop + listContentHeight / 2 - fontRenderer.FONT_HEIGHT / 2, 0xFFBBBBBB);
            return;
        }

        for (int i = 0; i < visibleItemCount; i++) {
            int index = i + sequenceScrollOffset;
            if (index >= sequenceRows.size()) {
                break;
            }
            SequenceListRow row = sequenceRows.get(index);
            int itemY = listTop + i * itemHeight;
            Rectangle bounds = new Rectangle(sequenceListX + 4, itemY + 1, sequenceListWidth - s(12), itemHeight - 2);
            row.bounds = bounds;
            boolean hovered = bounds.contains(mouseX, mouseY);

            if (row.header) {
                GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                        hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawRect(bounds.x + 1, bounds.y + 1, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1,
                        hovered ? 0x55355872 : 0x44324458);
                String prefix = row.collapsed ? ">" : "v";
                drawString(fontRenderer, prefix + " " + trimDisplayText(row.title, bounds.width - s(12)),
                        bounds.x + s(6), bounds.y + (bounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFB8D6F0);
                continue;
            }

            PathSequence seq = row.sequence;
            boolean selected = selectedSequence != null
                    && selectedSequence.getName().equals(seq.getName());
            GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                    resolveNavigationState(selected, hovered));
            if (PathSequenceEventListener.isSequenceRunningInBackground(seq.getName())) {
                drawSequenceRunStripe(bounds.x, bounds.y, bounds.width, true);
            } else if (PathSequenceEventListener.isSequenceRunningInForeground(seq.getName())) {
                drawSequenceRunStripe(bounds.x, bounds.y, bounds.width, false);
            }
            drawString(fontRenderer, trimDisplayText(getSequenceDisplayName(seq), bounds.width - s(10)),
                    bounds.x + s(6), bounds.y + (bounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);

            if (hovered) {
                hoveredSequenceIndex = sequencesInCategory.indexOf(seq);
                hoveredSequenceNote = seq.getNote();
            }
        }

        drawScrollbar(sequenceListX + sequenceListWidth - 7, listTop, listContentHeight, sequenceScrollOffset,
                maxSequenceScroll, sequenceRows.size(), visibleItemCount);
    }

    private void drawSequenceRunStripe(int x, int y, int width, boolean background) {
        double phase = (System.currentTimeMillis() % 900L) / 900.0D;
        double pulse = 0.45D + 0.55D * (0.5D + 0.5D * Math.sin(phase * Math.PI * 2.0D));
        int red = background ? (int) Math.round(110 + 145 * pulse) : (int) Math.round(40 + 45 * pulse);
        int green = background ? (int) Math.round(35 + 45 * pulse) : (int) Math.round(120 + 120 * pulse);
        int blue = background ? (int) Math.round(35 + 35 * pulse) : (int) Math.round(55 + 65 * pulse);
        int color = 0xFF000000
                | (MathHelper.clamp(red, 0, 255) << 16)
                | (MathHelper.clamp(green, 0, 255) << 8)
                | MathHelper.clamp(blue, 0, 255);
        drawRect(x + 1, y + 1, x + width - 1, y + 4, color);
    }

    private void drawStepList(int mouseX, int mouseY) {
        drawRect(stepListX, stepListY, stepListX + stepListWidth, stepListY + stepListHeight, 0x66324458);
        GuiTheme.drawInputFrameSafe(stepListX, stepListY, stepListWidth, stepListHeight, false, true);
        visibleStepNoteButtons.clear();
        if (selectedSequence == null) {
            this.drawCenteredString(fontRenderer, I18n.format("gui.path.manager.select_sequence_hint"),
                    stepListX + stepListWidth / 2, stepListY + 20, 0xAAAAAA);
            return;
        }
        int itemHeight = getStepActionCardItemHeight();
        int listTop = stepListY + 5;
        int listContentHeight = Math.max(itemHeight, stepListHeight - 10);
        int visibleItemCount = Math.max(1, listContentHeight / itemHeight);
        maxStepScroll = Math.max(0, selectedSequence.getSteps().size() - visibleItemCount);
        stepScrollOffset = MathHelper.clamp(stepScrollOffset, 0, maxStepScroll);

        for (int i = 0; i < visibleItemCount; i++) {
            int index = i + stepScrollOffset;
            if (index >= selectedSequence.getSteps().size())
                break;

            if (index == draggingStepIndex)
                continue;

            PathStep step = selectedSequence.getSteps().get(index);
            int itemY = listTop + i * itemHeight;
            Rectangle itemBounds = new Rectangle(stepListX + 3, itemY, stepListWidth - 11, itemHeight - 1);
            boolean isHovered = itemBounds.contains(mouseX, mouseY);
            boolean selected = selectedStepIndices.contains(index);
            int accentColor = index == selectedStepIndex ? 0xFF86D8FF : (selected ? 0xFF6BB5E8 : 0xFF6E93B8);
            drawEditorListCard(itemBounds, index == selectedStepIndex || selected, isHovered, accentColor);
            int noteButtonSize = Math.max(s(14), itemHeight - s(10));
            int noteButtonX = itemBounds.x + itemBounds.width - noteButtonSize - s(5);
            Rectangle noteBounds = new Rectangle(noteButtonX, itemBounds.y + (itemBounds.height - noteButtonSize) / 2,
                    noteButtonSize, noteButtonSize);
            visibleStepNoteButtons.put(index, noteBounds);
            boolean noteHovered = noteBounds.contains(mouseX, mouseY);
            GuiTheme.drawButtonFrameSafe(noteBounds.x, noteBounds.y, noteBounds.width, noteBounds.height,
                    noteHovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            String note = getStepNoteCompat(step);
            drawCenteredString(fontRenderer, isBlank(note) ? "○" : "●", noteBounds.x + noteBounds.width / 2,
                    noteBounds.y + (noteBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                    isBlank(note) ? 0xFFB8C4D0 : 0xFFFFD77A);
            int contentX = itemBounds.x + s(11);
            int textWidth = Math.max(s(36), noteBounds.x - contentX - s(7));
            drawString(fontRenderer, trimDisplayText("步骤 " + (index + 1), textWidth), contentX,
                    itemBounds.y + s(4), 0xFFF7FBFF);
            drawString(fontRenderer, trimDisplayTextWithEllipsis(getStepDescription(step, index), textWidth),
                    contentX, itemBounds.y + itemBounds.height - s(11),
                    selected ? 0xFFD2E6FA : (isHovered ? 0xFFC2D6EA : 0xFFAEC1D6));
        }

        if (draggingStepIndex != -1) {
            if (stepDropIndex != -1) {
                int dropLineY = listTop + (stepDropIndex - stepScrollOffset) * itemHeight - 1;
                drawRect(stepListX + 2, dropLineY, stepListX + stepListWidth - 8, dropLineY + 2, 0xFF00FFFF);
            }

            PathStep draggedStep = selectedSequence.getSteps().get(draggingStepIndex);
            String desc = getStepDescription(draggedStep, draggingStepIndex);
            int draggedY = mouseY
                    - (stepDragMouseStartY - (listTop + (draggingStepIndex - stepScrollOffset) * itemHeight));
            Rectangle draggedBounds = new Rectangle(stepListX + 3, draggedY, stepListWidth - 11, itemHeight - 1);
            drawEditorListCard(draggedBounds, true, true, 0xFF86D8FF);
            drawString(fontRenderer, trimDisplayText("步骤 " + (draggingStepIndex + 1), stepListWidth - s(26)),
                    draggedBounds.x + s(11), draggedBounds.y + s(4), 0xFFF7FBFF);
            drawString(fontRenderer, trimDisplayTextWithEllipsis(desc, stepListWidth - s(34)),
                    draggedBounds.x + s(11), draggedBounds.y + draggedBounds.height - s(11), 0xFFD2E6FA);
        }

        if (draggingStepIndex == -1 && clipboardPayloadType == ClipboardPayloadType.STEPS && !stepClipboard.isEmpty()) {
            List<Integer> selectedIndices = getSelectedStepIndicesForCopy();
            int insertIndex = selectedIndices.isEmpty()
                    ? selectedSequence.getSteps().size()
                    : selectedIndices.get(selectedIndices.size() - 1) + 1;
            int previewLineY = listTop + (insertIndex - stepScrollOffset) * itemHeight - 1;
            if (previewLineY >= listTop - 2 && previewLineY <= listTop + listContentHeight + 2) {
                drawRect(stepListX + 2, previewLineY, stepListX + stepListWidth - 8, previewLineY + 2, 0xFF7CD9FF);
            }
        }

        drawScrollbar(stepListX + stepListWidth - 7, listTop, listContentHeight, stepScrollOffset, maxStepScroll,
                selectedSequence.getSteps().size(), visibleItemCount);
    }

    private String getStepDescription(PathStep step, int index) {
        double[] pos = step.getGotoPoint();
        String posStr = Double.isNaN(pos[0]) ? I18n.format("gui.path.manager.no_movement")
                : String.format("GOTO [%.0f, %.0f, %.0f]", pos[0], pos[1], pos[2]);
        String failureSuffix = formatStepFailureSummary(step);
        return I18n.format("gui.path.manager.step_desc", index + 1, posStr, step.getActions().size()) + failureSuffix;
    }

    private int getStepHeaderTop() {
        return stepHeaderCardY;
    }

    private int getCollapsedStepHeaderHeight() {
        return s(28);
    }

    private int getStepListTop() {
        return stepListY;
    }

    private String formatStepFailureSummary(PathStep step) {
        if (step == null) {
            return "";
        }
        StringBuilder suffix = new StringBuilder();
        if (step.getRetryCount() > 0 && step.getPathRetryTimeoutSeconds() > 0) {
            suffix.append(" §7| §6重试").append(step.getRetryCount());
        } else if (step.getPathRetryTimeoutSeconds() <= 0 || step.getRetryCount() <= 0) {
            suffix.append(" §7| §8不重试");
        }
        suffix.append(" §7| §b到达±").append(step.getArrivalToleranceBlocks());
        if ("RESTART_SEQUENCE".equalsIgnoreCase(step.getRetryExhaustedPolicy())) {
            suffix.append(" §7| §b耗尽后重头");
        } else if ("RUN_SEQUENCE".equalsIgnoreCase(step.getRetryExhaustedPolicy())) {
            String target = step.getRetryExhaustedSequenceName();
            suffix.append(" §7| §d失败转序列");
            if (target != null && !target.trim().isEmpty()) {
                suffix.append("§f:")
                        .append(target.trim().length() > 8 ? target.trim().substring(0, 8) + "..." : target.trim());
            }
        }
        return suffix.toString();
    }

    private void drawActionList(int mouseX, int mouseY) {
        drawRect(actionListX, actionListY, actionListX + actionListWidth, actionListY + actionListHeight, 0x66324458);
        GuiTheme.drawInputFrameSafe(actionListX, actionListY, actionListWidth, actionListHeight, false, true);
        if (selectedStep == null)
            return;
        int itemHeight = getStepActionCardItemHeight();
        int listTop = actionListY + 5;
        int listContentHeight = Math.max(itemHeight, actionListHeight - 10);
        int visibleItemCount = Math.max(1, listContentHeight / itemHeight);
        maxActionScroll = Math.max(0, selectedStep.getActions().size() - visibleItemCount);
        actionScrollOffset = MathHelper.clamp(actionScrollOffset, 0, maxActionScroll);

        for (int i = 0; i < visibleItemCount; i++) {
            int index = i + actionScrollOffset;
            if (index >= selectedStep.getActions().size())
                break;

            // --- 核心新增：如果正在拖拽此项，跳过绘制 ---
            if (index == draggingActionIndex)
                continue;
            // --- 新增结束 ---

            ActionData action = selectedStep.getActions().get(index);
            int itemY = listTop + i * itemHeight;
            Rectangle itemBounds = new Rectangle(actionListX + 3, itemY, actionListWidth - 11, itemHeight - 1);
            boolean isHovered = itemBounds.contains(mouseX, mouseY);
            boolean selected = selectedActionIndices.contains(index);
            TextFormatting color = ACTION_COLORS.getOrDefault(normalize(action.type).toLowerCase(Locale.ROOT),
                    TextFormatting.WHITE);
            int accentColor = 0xFF9EC9E9;
            if (TextFormatting.RED.equals(color) || TextFormatting.DARK_RED.equals(color)) {
                accentColor = 0xFFE38D8D;
            } else if (TextFormatting.GREEN.equals(color) || TextFormatting.DARK_GREEN.equals(color)) {
                accentColor = 0xFF94D9A2;
            } else if (TextFormatting.GOLD.equals(color) || TextFormatting.YELLOW.equals(color)) {
                accentColor = 0xFFE2C778;
            }
            drawEditorListCard(itemBounds, index == selectedActionIndex || selected, isHovered, accentColor);
            boolean hasBreakpoint = selectedSequence != null
                    && PathSequenceEventListener.hasDebugBreakpoint(selectedSequence.getName(), selectedStepIndex,
                            index);
            int contentX = itemBounds.x + s(11);
            int textWidth = Math.max(s(40), itemBounds.width - s(20));
            String title = (hasBreakpoint ? "● " : "") + (index + 1) + ". " + getActionTypeLabel(action);
            int titleColor = hasBreakpoint ? 0xFFFFDF95 : accentColor;
            drawString(fontRenderer, trimDisplayText(title, textWidth), contentX, itemBounds.y + s(4), titleColor);
            drawString(fontRenderer, trimDisplayTextWithEllipsis(getActionCardDetail(action, hasBreakpoint), textWidth),
                    contentX, itemBounds.y + itemBounds.height - s(11),
                    selected ? 0xFFD9E8F5 : (isHovered ? 0xFFC8D7E6 : 0xFFB4C2D2));
        }

        // --- 核心新增：绘制拖拽指示器和被拖拽的动作项 ---
        if (draggingActionIndex != -1) {
            if (actionDropIndex != -1) {
                int dropLineY = listTop + (actionDropIndex - actionScrollOffset) * itemHeight - 1;
                drawRect(actionListX + 2, dropLineY, actionListX + actionListWidth - 8, dropLineY + 2, 0xFF00FFFF);
            }

            ActionData draggedAction = selectedStep.getActions().get(draggingActionIndex);
            TextFormatting color = ACTION_COLORS.getOrDefault(normalize(draggedAction.type).toLowerCase(Locale.ROOT),
                    TextFormatting.WHITE);
            int accentColor = 0xFF9EC9E9;
            if (TextFormatting.RED.equals(color) || TextFormatting.DARK_RED.equals(color)) {
                accentColor = 0xFFE38D8D;
            } else if (TextFormatting.GREEN.equals(color) || TextFormatting.DARK_GREEN.equals(color)) {
                accentColor = 0xFF94D9A2;
            } else if (TextFormatting.GOLD.equals(color) || TextFormatting.YELLOW.equals(color)) {
                accentColor = 0xFFE2C778;
            }
            int draggedY = mouseY
                    - (actionDragMouseStartY - (listTop + (draggingActionIndex - actionScrollOffset) * itemHeight));
            Rectangle draggedBounds = new Rectangle(actionListX + 3, draggedY, actionListWidth - 11, itemHeight - 1);
            drawEditorListCard(draggedBounds, true, true, accentColor);
            drawString(fontRenderer,
                    trimDisplayText((draggingActionIndex + 1) + ". " + getActionTypeLabel(draggedAction),
                            actionListWidth - s(28)),
                    draggedBounds.x + s(11), draggedBounds.y + s(4), accentColor);
            drawString(fontRenderer,
                    trimDisplayTextWithEllipsis(getActionCardDetail(draggedAction, false), actionListWidth - s(32)),
                    draggedBounds.x + s(11), draggedBounds.y + draggedBounds.height - s(11), 0xFFD9E8F5);
        }
        // --- 新增结束 ---

        if (draggingActionIndex == -1 && clipboardPayloadType == ClipboardPayloadType.ACTIONS && !actionClipboard.isEmpty()) {
            List<Integer> selectedIndices = getSelectedActionIndicesForCopy();
            int insertIndex = selectedIndices.isEmpty()
                    ? selectedStep.getActions().size()
                    : selectedIndices.get(selectedIndices.size() - 1) + 1;
            int previewLineY = listTop + (insertIndex - actionScrollOffset) * itemHeight - 1;
            if (previewLineY >= listTop - 2 && previewLineY <= listTop + listContentHeight + 2) {
                drawRect(actionListX + 2, previewLineY, actionListX + actionListWidth - 8, previewLineY + 2, 0xFF7CD9FF);
            }
        }

        drawScrollbar(actionListX + actionListWidth - 7, listTop, listContentHeight, actionScrollOffset,
                maxActionScroll, selectedStep.getActions().size(), visibleItemCount);
    }

    private Rectangle getVisibleStepNoteButtonAt(int mouseX, int mouseY) {
        for (Rectangle bounds : visibleStepNoteButtons.values()) {
            if (bounds != null && bounds.contains(mouseX, mouseY)) {
                return bounds;
            }
        }
        return null;
    }

    private int getVisibleStepNoteIndexAt(int mouseX, int mouseY) {
        for (Map.Entry<Integer, Rectangle> entry : visibleStepNoteButtons.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(mouseX, mouseY)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private List<String> buildStepNoteTooltipLines(int stepIndex) {
        List<String> lines = new ArrayList<>();
        if (selectedSequence == null || stepIndex < 0 || stepIndex >= selectedSequence.getSteps().size()) {
            return lines;
        }
        PathStep step = selectedSequence.getSteps().get(stepIndex);
        String note = normalize(getStepNoteCompat(step));
        lines.add("步骤备注");
        if (note.isEmpty()) {
            lines.add("当前无备注，请点击添加备注");
            return lines;
        }
        lines.addAll(fontRenderer.listFormattedStringToWidth(note, Math.max(120, s(220))));
        return lines;
    }

    private void openStepNotePopup(int stepIndex) {
        if (selectedSequence == null || stepIndex < 0 || stepIndex >= selectedSequence.getSteps().size()) {
            return;
        }
        editingStepNoteIndex = stepIndex;
        stepNotePopupVisible = true;
        if (stepNotePopupField == null) {
            stepNotePopupField = new GuiTextField(9301, this.fontRenderer, 0, 0, 0, 20);
            stepNotePopupField.setMaxStringLength(Integer.MAX_VALUE);
            stepNotePopupField.setEnableBackgroundDrawing(false);
        }
        stepNotePopupField.setText(getStepNoteCompat(selectedSequence.getSteps().get(stepIndex)));
        stepNotePopupField.setFocused(true);
        ensureStepNotePopupLayout();
    }

    private void saveStepNotePopup() {
        if (selectedSequence != null
                && editingStepNoteIndex >= 0
                && editingStepNoteIndex < selectedSequence.getSteps().size()
                && stepNotePopupField != null) {
            pushUndoHistory("step-note");
            setStepNoteCompat(selectedSequence.getSteps().get(editingStepNoteIndex), stepNotePopupField.getText());
            markDirty();
        }
        closeStepNotePopup();
    }

    private void closeStepNotePopup() {
        stepNotePopupVisible = false;
        editingStepNoteIndex = -1;
        if (stepNotePopupField != null) {
            stepNotePopupField.setFocused(false);
        }
    }

    private void ensureStepNotePopupLayout() {
        if (!stepNotePopupVisible) {
            return;
        }
        int popupWidth = Math.min(s(420), panelWidth - s(24));
        int popupHeight = Math.min(s(230), panelHeight - s(40));
        int popupX = panelX + (panelWidth - popupWidth) / 2;
        int popupY = panelY + (panelHeight - popupHeight) / 2;
        stepNotePopupBounds = new Rectangle(popupX, popupY, popupWidth, popupHeight);
        stepNotePopupCloseBounds = new Rectangle(popupX + popupWidth - s(18), popupY + s(4), s(14), s(14));
        stepNotePopupSaveBounds = new Rectangle(popupX + popupWidth - s(168), popupY + popupHeight - s(28), s(74), s(18));
        stepNotePopupCancelBounds = new Rectangle(popupX + popupWidth - s(86), popupY + popupHeight - s(28), s(74), s(18));
        if (stepNotePopupField != null) {
            stepNotePopupField.x = popupX + s(12);
            stepNotePopupField.y = popupY + s(36);
            stepNotePopupField.width = popupWidth - s(24);
            stepNotePopupField.height = s(20);
        }

        stepNotePopupColorBounds.clear();
        int swatchSize = s(16);
        int colorGap = s(4);
        int colorStartX = popupX + s(12);
        int colorStartY = popupY + popupHeight - s(92);
        for (int i = 0; i < NOTE_COLOR_CODES.length; i++) {
            int row = i / 8;
            int column = i % 8;
            stepNotePopupColorBounds.add(new Rectangle(colorStartX + column * (swatchSize + colorGap),
                    colorStartY + row * (swatchSize + colorGap), swatchSize, swatchSize));
        }

        stepNotePopupFormatBounds.clear();
        int formatWidth = s(28);
        int formatStartX = popupX + s(172);
        int formatY = popupY + popupHeight - s(92);
        for (int i = 0; i < NOTE_FORMAT_CODES.length; i++) {
            stepNotePopupFormatBounds.add(new Rectangle(formatStartX + i * (formatWidth + colorGap), formatY,
                    formatWidth, swatchSize));
        }
    }

    private void insertIntoStepNoteField(String text) {
        if (stepNotePopupField == null || text == null || text.isEmpty()) {
            return;
        }
        stepNotePopupField.setFocused(true);
        String currentText = stepNotePopupField.getText();
        if (currentText == null) {
            currentText = "";
        }
        int cursorPos = MathHelper.clamp(stepNotePopupField.getCursorPosition(), 0, currentText.length());
        int selectionPos = MathHelper.clamp(stepNotePopupField.getSelectionEnd(), 0, currentText.length());
        int start = Math.min(cursorPos, selectionPos);
        int end = Math.max(cursorPos, selectionPos);
        String newText = currentText.substring(0, start) + text + currentText.substring(end);
        stepNotePopupField.setText(newText);
        stepNotePopupField.setCursorPosition(start + text.length());
        stepNotePopupField.setSelectionPos(start + text.length());
    }

    private void drawStepNotePopup(int mouseX, int mouseY) {
        ensureStepNotePopupLayout();
        if (stepNotePopupBounds == null) {
            return;
        }
        drawRect(0, 0, this.width, this.height, 0x88000000);
        GuiTheme.drawPanel(stepNotePopupBounds.x, stepNotePopupBounds.y, stepNotePopupBounds.width, stepNotePopupBounds.height);
        GuiTheme.drawTitleBar(stepNotePopupBounds.x, stepNotePopupBounds.y, stepNotePopupBounds.width, "", this.fontRenderer);
        String title = editingStepNoteIndex >= 0 ? "步骤备注 - Step " + (editingStepNoteIndex + 1) : "步骤备注";
        drawString(fontRenderer, title, stepNotePopupBounds.x + s(10), stepNotePopupBounds.y + s(6), 0xFFF4FAFF);
        if (stepNotePopupCloseBounds != null) {
            boolean closeHovered = stepNotePopupCloseBounds.contains(mouseX, mouseY);
            GuiTheme.drawButtonFrameSafe(stepNotePopupCloseBounds.x, stepNotePopupCloseBounds.y,
                    stepNotePopupCloseBounds.width, stepNotePopupCloseBounds.height,
                    closeHovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, "x", stepNotePopupCloseBounds.x + stepNotePopupCloseBounds.width / 2,
                    stepNotePopupCloseBounds.y + (stepNotePopupCloseBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                    0xFFFFFFFF);
        }

        drawString(fontRenderer, "备注内容:", stepNotePopupBounds.x + s(12), stepNotePopupBounds.y + s(24), 0xFFE4EEF7);
        if (stepNotePopupField != null) {
            GuiTheme.drawInputFrameSafe(stepNotePopupField.x - 2, stepNotePopupField.y - 2, stepNotePopupField.width + 4,
                    stepNotePopupField.height + 4, stepNotePopupField.isFocused(), true);
            drawThemedTextField(stepNotePopupField);
        }

        int previewX = stepNotePopupBounds.x + s(12);
        int previewY = stepNotePopupBounds.y + s(68);
        int previewW = stepNotePopupBounds.width - s(24);
        int previewH = s(56);
        GuiTheme.drawInputFrameSafe(previewX, previewY, previewW, previewH, false, true);
        String previewText = stepNotePopupField == null ? "" : normalize(stepNotePopupField.getText());
        List<String> previewLines = fontRenderer.listFormattedStringToWidth(
                previewText.isEmpty() ? "§7当前无备注预览" : previewText, Math.max(s(80), previewW - s(8)));
        int previewLineY = previewY + s(6);
        for (int i = 0; i < previewLines.size() && previewLineY <= previewY + previewH - fontRenderer.FONT_HEIGHT - s(2); i++) {
            drawString(fontRenderer, previewLines.get(i), previewX + s(4), previewLineY, 0xFFFFFFFF);
            previewLineY += fontRenderer.FONT_HEIGHT + 2;
        }

        drawString(fontRenderer, "颜色快捷:", previewX, stepNotePopupBounds.y + stepNotePopupBounds.height - s(108), 0xFFE4EEF7);
        for (int i = 0; i < stepNotePopupColorBounds.size(); i++) {
            Rectangle bounds = stepNotePopupColorBounds.get(i);
            boolean hovered = bounds.contains(mouseX, mouseY);
            GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                    hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, NOTE_COLOR_CODES[i] + "A", bounds.x + bounds.width / 2,
                    bounds.y + (bounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        drawString(fontRenderer, "格式快捷:", previewX + s(160), stepNotePopupBounds.y + stepNotePopupBounds.height - s(108), 0xFFE4EEF7);
        for (int i = 0; i < stepNotePopupFormatBounds.size(); i++) {
            Rectangle bounds = stepNotePopupFormatBounds.get(i);
            boolean hovered = bounds.contains(mouseX, mouseY);
            GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                    hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, NOTE_FORMAT_BUTTON_TEXTS[i], bounds.x + bounds.width / 2,
                    bounds.y + (bounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        if (stepNotePopupSaveBounds != null) {
            GuiTheme.drawButtonFrameSafe(stepNotePopupSaveBounds.x, stepNotePopupSaveBounds.y,
                    stepNotePopupSaveBounds.width, stepNotePopupSaveBounds.height,
                    stepNotePopupSaveBounds.contains(mouseX, mouseY) ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, "保存", stepNotePopupSaveBounds.x + stepNotePopupSaveBounds.width / 2,
                    stepNotePopupSaveBounds.y + (stepNotePopupSaveBounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFB8FFD2);
        }
        if (stepNotePopupCancelBounds != null) {
            GuiTheme.drawButtonFrameSafe(stepNotePopupCancelBounds.x, stepNotePopupCancelBounds.y,
                    stepNotePopupCancelBounds.width, stepNotePopupCancelBounds.height,
                    stepNotePopupCancelBounds.contains(mouseX, mouseY) ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, "取消", stepNotePopupCancelBounds.x + stepNotePopupCancelBounds.width / 2,
                    stepNotePopupCancelBounds.y + (stepNotePopupCancelBounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }
    }

    private boolean handleStepNotePopupClick(int mouseX, int mouseY, int mouseButton) {
        if (!stepNotePopupVisible) {
            return false;
        }
        ensureStepNotePopupLayout();
        if (stepNotePopupBounds == null) {
            return true;
        }
        if (!stepNotePopupBounds.contains(mouseX, mouseY)) {
            if (stepNotePopupField != null) {
                stepNotePopupField.setFocused(false);
            }
            return true;
        }
        if (mouseButton == 0 && stepNotePopupCloseBounds != null && stepNotePopupCloseBounds.contains(mouseX, mouseY)) {
            closeStepNotePopup();
            return true;
        }
        if (mouseButton == 0 && stepNotePopupSaveBounds != null && stepNotePopupSaveBounds.contains(mouseX, mouseY)) {
            saveStepNotePopup();
            return true;
        }
        if (mouseButton == 0 && stepNotePopupCancelBounds != null && stepNotePopupCancelBounds.contains(mouseX, mouseY)) {
            closeStepNotePopup();
            return true;
        }
        if (mouseButton == 0) {
            for (int i = 0; i < stepNotePopupColorBounds.size(); i++) {
                Rectangle bounds = stepNotePopupColorBounds.get(i);
                if (bounds.contains(mouseX, mouseY)) {
                    insertIntoStepNoteField(NOTE_COLOR_CODES[i]);
                    return true;
                }
            }
            for (int i = 0; i < stepNotePopupFormatBounds.size(); i++) {
                Rectangle bounds = stepNotePopupFormatBounds.get(i);
                if (bounds.contains(mouseX, mouseY)) {
                    insertIntoStepNoteField(NOTE_FORMAT_CODES[i]);
                    return true;
                }
            }
        }
        if (stepNotePopupField != null) {
            stepNotePopupField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return true;
    }

    private List<Integer> getSelectedStepIndicesForCopy() {
        List<Integer> indices = new ArrayList<>(selectedStepIndices);
        if (indices.isEmpty() && selectedStepIndex >= 0) {
            indices.add(selectedStepIndex);
        }
        Collections.sort(indices);
        return indices;
    }

    private List<PathStep> getSelectedStepTargets() {
        List<PathStep> targets = new ArrayList<>();
        if (selectedSequence == null) {
            return targets;
        }
        for (int index : getSelectedStepIndicesForCopy()) {
            if (index >= 0 && index < selectedSequence.getSteps().size()) {
                targets.add(selectedSequence.getSteps().get(index));
            }
        }
        return targets;
    }

    private List<Integer> getSelectedActionIndicesForCopy() {
        List<Integer> indices = new ArrayList<>(selectedActionIndices);
        if (indices.isEmpty() && selectedActionIndex >= 0) {
            indices.add(selectedActionIndex);
        }
        Collections.sort(indices);
        return indices;
    }

    private boolean deleteSelectedActions() {
        if (selectedStep == null) {
            return false;
        }
        List<Integer> indices = getSelectedActionIndicesForCopy();
        if (indices.isEmpty()) {
            return false;
        }
        int nextSelectionIndex = Math.min(indices.get(0), selectedStep.getActions().size() - indices.size() - 1);
        for (int i = indices.size() - 1; i >= 0; i--) {
            int index = indices.get(i);
            if (index < 0 || index >= selectedStep.getActions().size()) {
                continue;
            }
            ActionData deletingAction = selectedStep.getActions().get(index);
            PathSequenceManager.removePersistentActionRecord(deletingAction);
            selectedStep.getActions().remove(index);
        }
        selectedActionIndices.clear();
        if (selectedStep.getActions().isEmpty()) {
            selectAction(-1);
        } else {
            selectAction(MathHelper.clamp(nextSelectionIndex, 0, selectedStep.getActions().size() - 1));
        }
        actionDetailPopupAction = null;
        actionDetailPopupIndex = -1;
        actionDetailPopupBounds = null;
        markDirty();
        return true;
    }

    private boolean deleteSelectedSteps() {
        if (selectedSequence == null) {
            return false;
        }
        List<Integer> indices = getSelectedStepIndicesForCopy();
        if (indices.isEmpty()) {
            return false;
        }
        int nextSelectionIndex = indices.get(0);
        for (int i = indices.size() - 1; i >= 0; i--) {
            int index = indices.get(i);
            if (index < 0 || index >= selectedSequence.getSteps().size()) {
                continue;
            }
            PathStep deletingStep = selectedSequence.getSteps().get(index);
            if (deletingStep != null) {
                for (ActionData action : deletingStep.getActions()) {
                    PathSequenceManager.removePersistentActionRecord(action);
                }
            }
            selectedSequence.getSteps().remove(index);
        }
        selectedStepIndices.clear();
        if (selectedSequence.getSteps().isEmpty()) {
            selectStep(-1);
        } else {
            selectStep(MathHelper.clamp(nextSelectionIndex, 0, selectedSequence.getSteps().size() - 1));
        }
        actionDetailPopupAction = null;
        actionDetailPopupIndex = -1;
        actionDetailPopupBounds = null;
        markDirty();
        return true;
    }

    void applyStepFailureSettings(List<PathStep> steps, int retryCount, int timeoutSeconds, int arrivalToleranceBlocks,
            String policy, String targetSequenceName) {
        List<PathStep> targets = steps == null ? Collections.emptyList() : steps;
        if (targets.isEmpty() && selectedStep != null) {
            targets = Collections.singletonList(selectedStep);
        }
        if (targets.isEmpty()) {
            return;
        }
        pushUndoHistory("step-failure");
        for (PathStep target : targets) {
            if (target == null) {
                continue;
            }
            target.setRetryCount(retryCount);
            target.setPathRetryTimeoutSeconds(timeoutSeconds);
            target.setArrivalToleranceBlocks(arrivalToleranceBlocks);
            target.setRetryExhaustedPolicy(policy);
            target.setRetryExhaustedSequenceName(targetSequenceName);
        }
        markDirty();
    }

    private void copySelectedStepsToClipboard() {
        if (selectedSequence == null) {
            return;
        }
        List<Integer> indices = getSelectedStepIndicesForCopy();
        if (indices.isEmpty()) {
            return;
        }
        stepClipboard.clear();
        actionClipboard.clear();
        for (int index : indices) {
            if (index >= 0 && index < selectedSequence.getSteps().size()) {
                stepClipboard.add(new PathStep(selectedSequence.getSteps().get(index)));
            }
        }
        clipboardPayloadType = ClipboardPayloadType.STEPS;
        clipboardStatusText = "已复制 " + stepClipboard.size() + " 个步骤，Ctrl+V 可插入到当前步骤后";
        clipboardStatusColor = 0xFF8CFF9E;
    }

    private void copySelectedActionsToClipboard() {
        if (selectedStep == null) {
            return;
        }
        List<Integer> indices = getSelectedActionIndicesForCopy();
        if (indices.isEmpty()) {
            return;
        }
        actionClipboard.clear();
        stepClipboard.clear();
        for (int index : indices) {
            if (index >= 0 && index < selectedStep.getActions().size()) {
                actionClipboard.add(new ActionData(selectedStep.getActions().get(index)));
            }
        }
        clipboardPayloadType = ClipboardPayloadType.ACTIONS;
        clipboardStatusText = "已复制 " + actionClipboard.size() + " 个动作，Ctrl+V 可插入到当前动作后";
        clipboardStatusColor = 0xFF8CFF9E;
    }

    private void pasteClipboardIntoSelection() {
        if (clipboardPayloadType == ClipboardPayloadType.STEPS && selectedSequence != null && !stepClipboard.isEmpty()) {
            pushUndoHistory("paste-steps");
            List<Integer> selectedIndices = getSelectedStepIndicesForCopy();
            int insertIndex = selectedIndices.isEmpty()
                    ? selectedSequence.getSteps().size()
                    : Math.min(selectedSequence.getSteps().size(), selectedIndices.get(selectedIndices.size() - 1) + 1);
            selectedStepIndices.clear();
            for (PathStep copiedStep : stepClipboard) {
                selectedSequence.getSteps().add(insertIndex, new PathStep(copiedStep));
                selectedStepIndices.add(insertIndex);
                insertIndex++;
            }
            int lastIndex = selectedStepIndices.isEmpty() ? -1 : new ArrayList<>(selectedStepIndices).get(selectedStepIndices.size() - 1);
            if (lastIndex >= 0) {
                setActiveStepPreservingSelection(lastIndex);
                selectionAnchorStepIndex = lastIndex;
            }
            clipboardStatusText = "已粘贴 " + stepClipboard.size() + " 个步骤";
            clipboardStatusColor = 0xFF8CFF9E;
            markDirty();
            return;
        }
        if (clipboardPayloadType == ClipboardPayloadType.ACTIONS && selectedStep != null && !actionClipboard.isEmpty()) {
            pushUndoHistory("paste-actions");
            List<Integer> selectedIndices = getSelectedActionIndicesForCopy();
            int insertIndex = selectedIndices.isEmpty()
                    ? selectedStep.getActions().size()
                    : Math.min(selectedStep.getActions().size(), selectedIndices.get(selectedIndices.size() - 1) + 1);
            selectedActionIndices.clear();
            for (ActionData copiedAction : actionClipboard) {
                selectedStep.getActions().add(insertIndex, new ActionData(copiedAction));
                selectedActionIndices.add(insertIndex);
                insertIndex++;
            }
            int lastIndex = selectedActionIndices.isEmpty() ? -1 : new ArrayList<>(selectedActionIndices).get(selectedActionIndices.size() - 1);
            if (lastIndex >= 0) {
                setActiveActionPreservingSelection(lastIndex);
                selectionAnchorActionIndex = lastIndex;
            }
            clipboardStatusText = "已粘贴 " + actionClipboard.size() + " 个动作";
            clipboardStatusColor = 0xFF8CFF9E;
            markDirty();
        }
    }

    private List<String> getClipboardHoverHint(int mouseX, int mouseY) {
        if (clipboardPayloadType == ClipboardPayloadType.STEPS
                && isInside(mouseX, mouseY, stepListX, stepListY, stepListWidth, stepListHeight)
                && !stepClipboard.isEmpty()) {
            return Arrays.asList("已复制步骤，待粘贴插入", clipboardStatusText);
        }
        if (clipboardPayloadType == ClipboardPayloadType.ACTIONS
                && isInside(mouseX, mouseY, actionListX, actionListY, actionListWidth, actionListHeight)
                && !actionClipboard.isEmpty()) {
            return Arrays.asList("已复制动作，待粘贴插入", clipboardStatusText);
        }
        return null;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private CategoryTreeRow findCategoryRowAt(int mouseX, int mouseY) {
        for (CategoryTreeRow row : visibleCategoryRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    private SequenceListRow findSequenceRowAt(int mouseX, int mouseY) {
        for (SequenceListRow row : sequenceRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    private void clearSequenceSearch(boolean keepFocus) {
        sequenceSearchQuery = "";
        if (sequenceSearchField != null) {
            sequenceSearchField.setText("");
            sequenceSearchField.setFocused(keepFocus);
        }
        filterSequencesByCategory();
    }

    private void updateSequenceSearchFromField() {
        String newQuery = sequenceSearchField == null ? "" : normalize(sequenceSearchField.getText());
        if (!sequenceSearchQuery.equals(newQuery)) {
            sequenceSearchQuery = newQuery;
            filterSequencesByCategory();
        } else {
            sequenceSearchQuery = newQuery;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (handleStepNotePopupClick(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (actionDetailPopupAction != null) {
            if (mouseButton == 0 || mouseButton == 1) {
                if (actionDetailPopupBounds == null || !actionDetailPopupBounds.contains(mouseX, mouseY)) {
                    actionDetailPopupAction = null;
                    actionDetailPopupIndex = -1;
                    actionDetailPopupBounds = null;
                }
            }
            return;
        }

        if (debugMonitorVisible) {
            ensureDebugMonitorWindowBounds();
            if (debugMonitorBounds != null && debugMonitorBounds.contains(mouseX, mouseY)) {
                if (mouseButton == 0 && debugMonitorCloseBounds != null
                        && debugMonitorCloseBounds.contains(mouseX, mouseY)) {
                    debugMonitorVisible = false;
                    draggingDebugMonitor = false;
                    persistDebugMonitorLayout();
                    updateButtonStates();
                    return;
                }
                if (mouseButton == 0 && debugMonitorMinimizeBounds != null
                        && debugMonitorMinimizeBounds.contains(mouseX, mouseY)) {
                    debugMonitorMinimized = !debugMonitorMinimized;
                    ensureDebugMonitorWindowBounds();
                    persistDebugMonitorLayout();
                    updateButtonStates();
                    return;
                }
                if (mouseButton == 0 && debugMonitorResizeCornerBounds != null
                        && debugMonitorResizeCornerBounds.contains(mouseX, mouseY)) {
                    resizingDebugMonitorCorner = true;
                    debugMonitorResizeStartMouseX = mouseX;
                    debugMonitorResizeStartMouseY = mouseY;
                    debugMonitorResizeStartWidth = debugMonitorPreferredWidth;
                    debugMonitorResizeStartHeight = debugMonitorPreferredHeight;
                    return;
                }
                if (mouseButton == 0 && debugMonitorResizeRightBounds != null
                        && debugMonitorResizeRightBounds.contains(mouseX, mouseY)) {
                    resizingDebugMonitorRight = true;
                    debugMonitorResizeStartMouseX = mouseX;
                    debugMonitorResizeStartMouseY = mouseY;
                    debugMonitorResizeStartWidth = debugMonitorPreferredWidth;
                    debugMonitorResizeStartHeight = debugMonitorPreferredHeight;
                    return;
                }
                if (mouseButton == 0 && debugMonitorResizeBottomBounds != null
                        && debugMonitorResizeBottomBounds.contains(mouseX, mouseY)) {
                    resizingDebugMonitorBottom = true;
                    debugMonitorResizeStartMouseX = mouseX;
                    debugMonitorResizeStartMouseY = mouseY;
                    debugMonitorResizeStartWidth = debugMonitorPreferredWidth;
                    debugMonitorResizeStartHeight = debugMonitorPreferredHeight;
                    return;
                }
                if (mouseButton == 0 && debugMonitorHeaderBounds != null
                        && debugMonitorHeaderBounds.contains(mouseX, mouseY)) {
                    draggingDebugMonitor = true;
                    debugMonitorDragOffsetX = mouseX - debugMonitorX;
                    debugMonitorDragOffsetY = mouseY - debugMonitorY;
                    return;
                }
                if (mouseButton == 0 && handleDebugMonitorButtonClick(mouseX, mouseY)) {
                    return;
                }
                return;
            }
        }

        if (mouseButton == 0 && leftDividerBounds != null && leftDividerBounds.contains(mouseX, mouseY)) {
            draggingLeftDivider = true;
            return;
        }
        if (mouseButton == 0 && rightDividerBounds != null && rightDividerBounds.contains(mouseX, mouseY)) {
            draggingRightDivider = true;
            return;
        }
        if (mouseButton == 0 && categoryDividerBounds != null && categoryDividerBounds.contains(mouseX, mouseY)) {
            draggingCategoryDivider = true;
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mc.currentScreen != this) {
            return;
        }
        gotoX.mouseClicked(mouseX, mouseY, mouseButton);
        gotoY.mouseClicked(mouseX, mouseY, mouseButton);
        gotoZ.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && sequenceSearchClearBounds != null
                && sequenceSearchClearBounds.contains(mouseX, mouseY)) {
            clearSequenceSearch(true);
            return;
        }
        if (sequenceSearchField != null) {
            sequenceSearchField.mouseClicked(mouseX, mouseY, mouseButton);
            if (isInside(mouseX, mouseY, sequenceSearchField.x, sequenceSearchField.y, sequenceSearchField.width,
                    sequenceSearchField.height)) {
                return;
            }
        }

        if (mouseButton == 0 && categoryPanelToggleBounds != null
                && categoryPanelToggleBounds.contains(mouseX, mouseY)) {
            categoryPanelCollapsed = !categoryPanelCollapsed;
            initGui();
            return;
        }

        if (!categoryPanelCollapsed && tryClickScrollbar(mouseX, mouseY, categoryListX + categoryListWidth - 7,
                getCategoryListTop(), getCategoryListContentHeight(), maxCategoryScroll)) {
            isDraggingCategory = true;
            return;
        }
        if (tryClickScrollbar(mouseX, mouseY, sequenceListX + sequenceListWidth - 7, getSequenceListTop(),
                getSequenceListContentHeight(), maxSequenceScroll)) {
            isDraggingSequence = true;
            return;
        }
        if (tryClickScrollbar(mouseX, mouseY, stepListX + stepListWidth - 7, stepListY + 5, stepListHeight - 10,
                maxStepScroll)) {
            isDraggingStep = true;
            return;
        }
        if (tryClickScrollbar(mouseX, mouseY, actionListX + actionListWidth - 7, actionListY + 5,
                actionListHeight - 10, maxActionScroll)) {
            isDraggingAction = true;
            return;
        }

        if (mouseButton == 0 && !categoryPanelCollapsed) {
            CategoryTreeRow clickedCategoryRow = findCategoryRowAt(mouseX, mouseY);
            if (clickedCategoryRow != null) {
                boolean clickedArrow = clickedCategoryRow.isCustomRoot()
                        && clickedCategoryRow.toggleBounds != null
                        && clickedCategoryRow.toggleBounds.contains(mouseX, mouseY);
                if (clickedArrow) {
                    MainUiLayoutManager.toggleCollapsed(clickedCategoryRow.category);
                    if (clickedCategoryRow.category.equals(selectedCategory)
                            && MainUiLayoutManager.isCollapsed(clickedCategoryRow.category)
                            && !isBlank(selectedSubCategory)) {
                        selectedSubCategory = "";
                    }
                    filterSequencesByCategory();
                    return;
                }
                selectedCategory = clickedCategoryRow.category;
                selectedSubCategory = clickedCategoryRow.isSubCategory() ? clickedCategoryRow.subCategory : "";
                filterSequencesByCategory();
                return;
            }
        }

        if (mouseButton == 0) {
            SequenceListRow clickedSequenceRow = findSequenceRowAt(mouseX, mouseY);
            if (clickedSequenceRow != null) {
                if (clickedSequenceRow.header) {
                    if (isRootCategoryMode() && isBlank(sequenceSearchQuery)
                            && !clickedSequenceRow.groupKey.isEmpty()) {
                        toggleSequenceGroupCollapsed(clickedSequenceRow.groupKey);
                        rebuildSequenceRows();
                        sequenceScrollOffset = MathHelper.clamp(sequenceScrollOffset, 0,
                                Math.max(0, sequenceRows.size() - getSequenceVisibleItemCount()));
                    }
                    return;
                }
                if (clickedSequenceRow.sequence != null) {
                    int selectedIndex = findSequenceIndex(clickedSequenceRow.sequence,
                            clickedSequenceRow.sequence.getName());
                    if (selectedIndex >= 0) {
                        selectSequence(selectedIndex);
                    }
                    updateButtonStates();
                    return;
                }
            }
        }

        if (selectedSequence != null && mouseX >= stepListX && mouseX < stepListX + stepListWidth - 7
                && mouseY >= stepListY && mouseY <= stepListY + stepListHeight) {
            int noteIndex = getVisibleStepNoteIndexAt(mouseX, mouseY);
            if (mouseButton == 0 && noteIndex >= 0) {
                openStepNotePopup(noteIndex);
                return;
            }
            int clickedItemIndex = (mouseY - (stepListY + 5)) / getStepActionCardItemHeight();
            int actualIndex = clickedItemIndex + stepScrollOffset;

            if (actualIndex >= 0 && actualIndex < selectedSequence.getSteps().size() && mouseButton == 0) {
                boolean ctrl = isCtrlDown();
                boolean shift = isShiftDown();
                applyStepSelectionClick(actualIndex, ctrl, shift);
                if (!ctrl && !shift && selectedStepIndices.size() <= 1) {
                    draggingStepIndex = actualIndex;
                    stepDragMouseStartY = mouseY;
                }
            }
        } else if (selectedStep != null && mouseX >= actionListX && mouseX < actionListX + actionListWidth - 7
                && mouseY >= actionListY && mouseY <= actionListY + actionListHeight) {
            int clickedItemIndex = (mouseY - (actionListY + 5)) / getStepActionCardItemHeight();
            int actualIndex = clickedItemIndex + actionScrollOffset;
            if (actualIndex >= 0 && actualIndex < selectedStep.getActions().size() && mouseButton == 0) {
                boolean ctrl = isCtrlDown();
                boolean shift = isShiftDown();
                applyActionSelectionClick(actualIndex, ctrl, shift);
                if (!ctrl && !shift && selectedActionIndices.size() <= 1) {
                    draggingActionIndex = actualIndex;
                    actionDragMouseStartY = mouseY;
                }
            } else if (actualIndex >= 0 && actualIndex < selectedStep.getActions().size() && mouseButton == 1) {
                applyActionSelectionClick(actualIndex, false, false);
                actionDetailPopupAction = selectedStep.getActions().get(actualIndex);
                actionDetailPopupIndex = actualIndex;
                actionDetailPopupBounds = null;
                updateButtonStates();
                return;
            }
        }

        updateButtonStates();
    }

    private boolean tryClickScrollbar(int mouseX, int mouseY, int x, int y, int height, int maxScroll) {
        if (maxScroll > 0 && mouseX >= x && mouseX <= x + 6 && mouseY >= y && mouseY <= y + height) {
            scrollClickY = mouseY;
            return true;
        }
        return false;
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        if (draggingDebugMonitor) {
            debugMonitorX = mouseX - debugMonitorDragOffsetX;
            debugMonitorY = mouseY - debugMonitorDragOffsetY;
            ensureDebugMonitorWindowBounds();
            return;
        }
        if (resizingDebugMonitorCorner || resizingDebugMonitorRight || resizingDebugMonitorBottom) {
            if (resizingDebugMonitorCorner || resizingDebugMonitorRight) {
                debugMonitorPreferredWidth = debugMonitorResizeStartWidth + (mouseX - debugMonitorResizeStartMouseX);
            }
            if (resizingDebugMonitorCorner || resizingDebugMonitorBottom) {
                debugMonitorPreferredHeight = debugMonitorResizeStartHeight + (mouseY - debugMonitorResizeStartMouseY);
            }
            ensureDebugMonitorWindowBounds();
            return;
        }
        if (draggingLeftDivider) {
            applyLeftDividerDrag(mouseX);
            return;
        }
        if (draggingRightDivider) {
            applyRightDividerDrag(mouseX);
            return;
        }
        if (draggingCategoryDivider) {
            applyCategoryDividerDrag(mouseY);
            return;
        }

        if (draggingStepIndex != -1) {
            int listTop = stepListY + 5;
            int itemHeight = getStepActionCardItemHeight();
            int hoverIndex = (mouseY - listTop + itemHeight / 2) / itemHeight + stepScrollOffset;
            stepDropIndex = MathHelper.clamp(hoverIndex, 0, selectedSequence.getSteps().size());
        }

        // --- 核心新增：处理动作列表的拖拽移动 ---
        if (draggingActionIndex != -1) {
            int listTop = actionListY + 5;
            int itemHeight = getStepActionCardItemHeight();
            int hoverIndex = (mouseY - listTop + itemHeight / 2) / itemHeight + actionScrollOffset;
            actionDropIndex = MathHelper.clamp(hoverIndex, 0, selectedStep.getActions().size());
        }
        // --- 新增结束 ---

        if (isDraggingCategory) {
            categoryScrollOffset = updateScrollOffset(mouseY, scrollClickY, categoryScrollOffset, getCategoryListTop(),
                    getCategoryListContentHeight(), maxCategoryScroll, Math.max(1, buildVisibleCategoryRows().size()));
        }
        if (isDraggingSequence) {
            sequenceScrollOffset = updateScrollOffset(mouseY, scrollClickY, sequenceScrollOffset, getSequenceListTop(),
                    getSequenceListContentHeight(), maxSequenceScroll, Math.max(1, sequenceRows.size()));
        }
        if (isDraggingStep) {
            stepScrollOffset = updateScrollOffset(mouseY, scrollClickY, stepScrollOffset, stepListY + 5,
                    stepListHeight - 10, maxStepScroll, selectedSequence.getSteps().size());
        }
        if (isDraggingAction) {
            actionScrollOffset = updateScrollOffset(mouseY, scrollClickY, actionScrollOffset, actionListY + 5,
                    actionListHeight - 10, maxActionScroll, selectedStep.getActions().size());
        }
    }

    private int updateScrollOffset(int mouseY, int startY, int startOffset, int listTop, int listHeight, int maxScroll,
            int totalItems) {
        if (maxScroll <= 0)
            return 0;
        int visibleItems = Math.max(1, listHeight / getNavigationItemHeight());
        int thumbHeight = Math.max(10, (int) ((float) visibleItems / totalItems * listHeight));
        int scrollableHeight = listHeight - thumbHeight;
        if (scrollableHeight <= 0) {
            return 0;
        }

        float percent = (float) (mouseY - listTop) / scrollableHeight;
        int newOffset = (int) (percent * maxScroll);

        return Math.max(0, Math.min(maxScroll, newOffset));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        boolean changedDebugMonitorLayout = draggingDebugMonitor
                || resizingDebugMonitorRight
                || resizingDebugMonitorBottom
                || resizingDebugMonitorCorner;

        if (state == 0) { // 左键释放
            if (draggingStepIndex != -1) {
                if (stepDropIndex != -1 && draggingStepIndex != stepDropIndex) {
                    pushUndoHistory("drag-step");
                    PathStep draggedStep = selectedSequence.getSteps().remove(draggingStepIndex);
                    if (stepDropIndex > draggingStepIndex) {
                        stepDropIndex--;
                    }
                    selectedSequence.getSteps().add(stepDropIndex, draggedStep);
                    selectStep(stepDropIndex);
                }
                draggingStepIndex = -1;
                stepDragMouseStartY = -1;
                stepDropIndex = -1;
            }

            // --- 核心新增：处理动作列表的拖拽释放 ---
            if (draggingActionIndex != -1) {
                if (actionDropIndex != -1 && draggingActionIndex != actionDropIndex) {
                    pushUndoHistory("drag-action");
                    ActionData draggedAction = selectedStep.getActions().remove(draggingActionIndex);
                    if (actionDropIndex > draggingActionIndex) {
                        actionDropIndex--;
                    }
                    selectedStep.getActions().add(actionDropIndex, draggedAction);
                    selectAction(actionDropIndex);
                }
                draggingActionIndex = -1;
                actionDragMouseStartY = -1;
                actionDropIndex = -1;
            }
            // --- 新增结束 ---
        }

        draggingDebugMonitor = false;
        resizingDebugMonitorRight = false;
        resizingDebugMonitorBottom = false;
        resizingDebugMonitorCorner = false;
        if (changedDebugMonitorLayout) {
            persistDebugMonitorLayout();
        }
        draggingLeftDivider = false;
        draggingRightDivider = false;
        draggingCategoryDivider = false;
        isDraggingCategory = isDraggingSequence = isDraggingStep = isDraggingAction = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (stepNotePopupVisible) {
            if (stepNotePopupField != null) {
                stepNotePopupField.textboxKeyTyped(typedChar, keyCode);
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                closeStepNotePopup();
            } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                saveStepNotePopup();
            }
            return;
        }
        boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (ctrlDown) {
            if (keyCode == Keyboard.KEY_Z) {
                if (isShiftDown()) {
                    performRedo();
                } else {
                    performUndo();
                }
                return;
            }
            if (keyCode == Keyboard.KEY_Y) {
                performRedo();
                return;
            }
        }
        if (actionDetailPopupAction != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                actionDetailPopupAction = null;
                actionDetailPopupIndex = -1;
                actionDetailPopupBounds = null;
            }
            return;
        }
        if (debugMonitorVisible && keyCode == Keyboard.KEY_ESCAPE) {
            debugMonitorVisible = false;
            draggingDebugMonitor = false;
            persistDebugMonitorLayout();
            updateButtonStates();
            return;
        }
        if (keyCode == Keyboard.KEY_F
                && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            if (sequenceSearchField != null) {
                sequenceSearchField.setFocused(true);
            }
            return;
        }
        if (sequenceSearchField != null && sequenceSearchField.textboxKeyTyped(typedChar, keyCode)) {
            updateSequenceSearchFromField();
            return;
        }
        if (!gotoX.isFocused() && !gotoY.isFocused() && !gotoZ.isFocused() && ctrlDown) {
            if (keyCode == Keyboard.KEY_C) {
                if (!selectedActionIndices.isEmpty() || selectedActionIndex >= 0) {
                    copySelectedActionsToClipboard();
                } else if (!selectedStepIndices.isEmpty() || selectedStepIndex >= 0) {
                    copySelectedStepsToClipboard();
                }
                return;
            }
            if (keyCode == Keyboard.KEY_V) {
                pasteClipboardIntoSelection();
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
        if (gotoX.isFocused()) {
            gotoX.textboxKeyTyped(typedChar, keyCode);
            updateStepFromFields();
        }
        if (gotoY.isFocused()) {
            gotoY.textboxKeyTyped(typedChar, keyCode);
            updateStepFromFields();
        }
        if (gotoZ.isFocused()) {
            gotoZ.textboxKeyTyped(typedChar, keyCode);
            updateStepFromFields();
        }
    }

    private void updateStepFromFields() {
        if (selectedStep == null)
            return;
        try {
            double x = gotoX.getText().isEmpty() ? Double.NaN : Double.parseDouble(gotoX.getText());
            double y = gotoY.getText().isEmpty() ? Double.NaN : Double.parseDouble(gotoY.getText());
            double z = gotoZ.getText().isEmpty() ? Double.NaN : Double.parseDouble(gotoZ.getText());
            double[] old = selectedStep.getGotoPoint();
            boolean changed = old == null || old.length < 3
                    || Double.compare(old[0], x) != 0
                    || Double.compare(old[1], y) != 0
                    || Double.compare(old[2], z) != 0;
            if (changed) {
                pushUndoHistory("step-goto");
                selectedStep.setGotoPoint(new double[] { x, y, z });
                markDirty();
            }
        } catch (NumberFormatException e) {
        }
    }

    private int findSequenceRowIndex(PathSequence targetSequence) {
        if (targetSequence == null) {
            return -1;
        }
        for (int i = 0; i < sequenceRows.size(); i++) {
            SequenceListRow row = sequenceRows.get(i);
            if (!row.header && row.sequence != null && targetSequence.getName().equals(row.sequence.getName())) {
                return i;
            }
        }
        return -1;
    }

    private void selectSequence(int index) {
        if (index < 0 || index >= sequencesInCategory.size()) {
            selectedSequenceIndex = -1;
            selectedSequence = null;
        } else {
            selectedSequenceIndex = index;
            selectedSequence = sequencesInCategory.get(index);
            if (selectedSequence != null && isRootCategoryMode()) {
                collapsedSequenceGroups.remove(
                        buildSequenceGroupKey(selectedCategory, selectedSequence.getSubCategory()));
                rebuildSequenceRows();
            }
        }
        stepScrollOffset = 0;
        actionScrollOffset = 0;
        selectStep(-1); // 重置步骤选择
    }

    private void selectStep(int index) {
        if (selectedSequence == null || index < 0 || index >= selectedSequence.getSteps().size()) {
            selectedStepIndex = -1;
            selectedStep = null;
            selectedStepIndices.clear();
        } else {
            selectedStepIndex = index;
            selectedStep = selectedSequence.getSteps().get(index);
            selectedStepIndices.clear();
            selectedStepIndices.add(index);
            selectionAnchorStepIndex = index;
        }
        actionScrollOffset = 0;
        updateGotoFields();
        selectAction(-1); // 重置动作选择
    }

    private void selectAction(int index) {
        if (selectedStep == null || index < 0 || index >= selectedStep.getActions().size()) {
            selectedActionIndex = -1;
            selectedAction = null;
            selectedActionIndices.clear();
        } else {
            selectedActionIndex = index;
            selectedAction = selectedStep.getActions().get(index);
            selectedActionIndices.clear();
            selectedActionIndices.add(index);
            selectionAnchorActionIndex = index;
        }
    }

    private void setActiveStepPreservingSelection(int index) {
        if (selectedSequence == null || index < 0 || index >= selectedSequence.getSteps().size()) {
            selectedStepIndex = -1;
            selectedStep = null;
            actionScrollOffset = 0;
            updateGotoFields();
            selectAction(-1);
            return;
        }
        selectedStepIndex = index;
        selectedStep = selectedSequence.getSteps().get(index);
        actionScrollOffset = 0;
        updateGotoFields();
        selectAction(-1);
    }

    private void setActiveActionPreservingSelection(int index) {
        if (selectedStep == null || index < 0 || index >= selectedStep.getActions().size()) {
            selectedActionIndex = -1;
            selectedAction = null;
            return;
        }
        selectedActionIndex = index;
        selectedAction = selectedStep.getActions().get(index);
    }

    private void applyStepSelectionClick(int index, boolean ctrl, boolean shift) {
        if (selectedSequence == null || index < 0 || index >= selectedSequence.getSteps().size()) {
            return;
        }
        if (shift && selectionAnchorStepIndex >= 0 && selectionAnchorStepIndex < selectedSequence.getSteps().size()) {
            selectedStepIndices.clear();
            int start = Math.min(selectionAnchorStepIndex, index);
            int end = Math.max(selectionAnchorStepIndex, index);
            for (int i = start; i <= end; i++) {
                selectedStepIndices.add(i);
            }
            setActiveStepPreservingSelection(index);
            return;
        }
        if (ctrl) {
            if (selectedStepIndices.contains(index) && selectedStepIndices.size() > 1) {
                selectedStepIndices.remove(index);
                int fallback = selectedStepIndices.iterator().next();
                setActiveStepPreservingSelection(fallback);
            } else {
                selectedStepIndices.add(index);
                selectionAnchorStepIndex = index;
                setActiveStepPreservingSelection(index);
            }
            return;
        }
        selectStep(index);
    }

    private void applyActionSelectionClick(int index, boolean ctrl, boolean shift) {
        if (selectedStep == null || index < 0 || index >= selectedStep.getActions().size()) {
            return;
        }
        if (shift && selectionAnchorActionIndex >= 0
                && selectionAnchorActionIndex < selectedStep.getActions().size()) {
            selectedActionIndices.clear();
            int start = Math.min(selectionAnchorActionIndex, index);
            int end = Math.max(selectionAnchorActionIndex, index);
            for (int i = start; i <= end; i++) {
                selectedActionIndices.add(i);
            }
            setActiveActionPreservingSelection(index);
            return;
        }
        if (ctrl) {
            if (selectedActionIndices.contains(index) && selectedActionIndices.size() > 1) {
                selectedActionIndices.remove(index);
                int fallback = selectedActionIndices.iterator().next();
                setActiveActionPreservingSelection(fallback);
            } else {
                selectedActionIndices.add(index);
                selectionAnchorActionIndex = index;
                setActiveActionPreservingSelection(index);
            }
            return;
        }
        selectAction(index);
    }

    private void updateGotoFields() {
        if (selectedStep != null) {
            double[] pos = selectedStep.getGotoPoint();
            gotoX.setText(Double.isNaN(pos[0]) ? "" : String.valueOf(pos[0]));
            gotoY.setText(Double.isNaN(pos[1]) ? "" : String.valueOf(pos[1]));
            gotoZ.setText(Double.isNaN(pos[2]) ? "" : String.valueOf(pos[2]));
        } else {
            if (gotoX != null)
                gotoX.setText("");
            if (gotoY != null)
                gotoY.setText("");
            if (gotoZ != null)
                gotoZ.setText("");
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;

            if (debugMonitorVisible && debugMonitorBounds != null && debugMonitorBounds.contains(mouseX, mouseY)) {
                if (!debugMonitorMinimized && debugMonitorMaxScroll > 0) {
                    if (dWheel > 0) {
                        debugMonitorScrollOffset = Math.max(0, debugMonitorScrollOffset - 2);
                    } else {
                        debugMonitorScrollOffset = Math.min(debugMonitorMaxScroll, debugMonitorScrollOffset + 2);
                    }
                }
                return;
            }
            if (headerToolbarViewportBounds != null && headerToolbarViewportBounds.contains(mouseX, mouseY)
                    && headerToolbarMaxScrollIndex > 0) {
                headerToolbarScrollIndex = MathHelper.clamp(headerToolbarScrollIndex + (dWheel > 0 ? -1 : 1), 0,
                        headerToolbarMaxScrollIndex);
                layoutHeaderToolbarButtons();
            } else if (!categoryPanelCollapsed
                    && isInside(mouseX, mouseY, categoryListX, categoryListY, categoryListWidth, categoryListHeight)) {
                if (dWheel > 0)
                    categoryScrollOffset = Math.max(0, categoryScrollOffset - 1);
                else
                    categoryScrollOffset = Math.min(maxCategoryScroll, categoryScrollOffset + 1);
            } else if (isInside(mouseX, mouseY, sequenceListX, sequenceListY, sequenceListWidth, sequenceListHeight)) {
                if (dWheel > 0)
                    sequenceScrollOffset = Math.max(0, sequenceScrollOffset - 1);
                else
                    sequenceScrollOffset = Math.min(maxSequenceScroll, sequenceScrollOffset + 1);
            } else if (isInside(mouseX, mouseY, stepListX, stepListY, stepListWidth, stepListHeight)) {
                if (dWheel > 0)
                    stepScrollOffset = Math.max(0, stepScrollOffset - 1);
                else
                    stepScrollOffset = Math.min(maxStepScroll, stepScrollOffset + 1);
            } else if (isInside(mouseX, mouseY, actionListX, actionListY, actionListWidth, actionListHeight)) {
                if (dWheel > 0)
                    actionScrollOffset = Math.max(0, actionScrollOffset - 1);
                else
                    actionScrollOffset = Math.min(maxActionScroll, actionScrollOffset + 1);
            }
        }
    }

    private void applyLeftDividerDrag(int mouseX) {
        int availableWidth = Math.max(1, contentWidth - LAYOUT_DIVIDER_WIDTH * 2);
        int minLeftWidth = Math.max(s(132), Math.min(MIN_LEFT_COLUMN_WIDTH, Math.max(s(132), availableWidth / 3)));
        int minActionWidth = Math.max(s(128), Math.min(MIN_ACTION_COLUMN_WIDTH, Math.max(s(128), availableWidth / 3)));
        int minStepWidth = Math.max(s(168),
                Math.min(MIN_STEP_COLUMN_WIDTH, Math.max(s(168), availableWidth - minLeftWidth - minActionWidth)));
        int leftWidth = MathHelper.clamp(mouseX - contentX - LAYOUT_DIVIDER_WIDTH / 2, minLeftWidth,
                Math.max(minLeftWidth, availableWidth - minStepWidth - minActionWidth));
        leftDividerRatio = leftWidth / (double) Math.max(1, availableWidth);
        if (rightDividerRatio <= leftDividerRatio) {
            rightDividerRatio = Math.min(0.95D,
                    leftDividerRatio + (minStepWidth / (double) Math.max(1, availableWidth)));
        }
        persistCurrentLayoutRatios();
        initGui();
    }

    private void applyRightDividerDrag(int mouseX) {
        int availableWidth = Math.max(1, contentWidth - LAYOUT_DIVIDER_WIDTH * 2);
        int minActionWidth = Math.max(s(128), Math.min(MIN_ACTION_COLUMN_WIDTH, Math.max(s(128), availableWidth / 3)));
        int minStepWidth = Math.max(s(168),
                Math.min(MIN_STEP_COLUMN_WIDTH, Math.max(s(168), availableWidth - leftColumnWidth - minActionWidth)));
        int minBoundary = leftColumnWidth + minStepWidth;
        int boundary = MathHelper.clamp(mouseX - contentX - LAYOUT_DIVIDER_WIDTH / 2, minBoundary,
                Math.max(minBoundary, availableWidth - minActionWidth));
        rightDividerRatio = boundary / (double) Math.max(1, availableWidth);
        persistCurrentLayoutRatios();
        initGui();
    }

    private void applyCategoryDividerDrag(int mouseY) {
        if (categoryPanelCollapsed) {
            return;
        }
        int availableHeight = Math.max(1, contentHeight - LAYOUT_DIVIDER_WIDTH);
        int minCategoryHeight = Math.max(s(88), Math.min(MIN_CATEGORY_HEIGHT, Math.max(s(88), contentHeight / 3)));
        int minSequenceHeight = Math.max(s(120), Math.min(MIN_SEQUENCE_HEIGHT, Math.max(s(120), contentHeight / 3)));
        int categoryHeight = MathHelper.clamp(mouseY - contentY - LAYOUT_DIVIDER_WIDTH / 2, minCategoryHeight,
                Math.max(minCategoryHeight, availableHeight - minSequenceHeight));
        categoryHeightRatio = categoryHeight / (double) Math.max(1, availableHeight);
        persistCurrentLayoutRatios();
        initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return !GuiPathingPolicy.shouldKeepPathingDuringGui(this.mc);
    }
}

