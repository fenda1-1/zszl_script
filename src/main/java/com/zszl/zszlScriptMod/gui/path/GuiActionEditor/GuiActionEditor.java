// File: src/main/java/com/zszl/zszlScriptMod/gui/path/GuiActionEditor/GuiActionEditor.java
// Includes Packet ID input support
package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.library.ActionDisplayCatalog;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.library.ActionLibraryTreeFactory;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.library.ActionLibraryViewSupport;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ActionLibraryNode;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ActionLibraryVisibleRow;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.BlockEntry;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ConditionExpressionEditorBinding;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.EntityEntry;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionEditorBinding;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionTemplateCard;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionTemplateLayoutEntry;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.GroupedVariableSelectorBinding;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.IndexedHitRegion;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ParamSectionCard;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ScopedVariableEditorBinding;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.prefs.GuiActionEditorPreferences;
import com.zszl.zszlScriptMod.gui.path.GuiCapturedIdSelector;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.feedback.ActionEditorFeedbackSupport;
import com.zszl.zszlScriptMod.gui.path.GuiOtherFeatureSelector;
import com.zszl.zszlScriptMod.gui.path.GuiPathValidationReport;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager.FeatureDef;
import com.zszl.zszlScriptMod.handlers.ItemFilterHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.ActionVariableRegistry;
import com.zszl.zszlScriptMod.path.template.LegacyActionTemplateManager;
import com.zszl.zszlScriptMod.path.validation.PathConfigValidator;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.utils.locator.ActionTargetLocator;
import com.zszl.zszlScriptMod.system.ProfileManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.Rectangle;
import java.util.*;
import java.util.function.Consumer;

import static com.zszl.zszlScriptMod.gui.path.GuiActionEditor.util.ActionEditorDisplayConverters.*;

public class GuiActionEditor extends ThemedGuiScreen {
    private static final class MoveChestFilterRuleDraft {
        private final String itemName;
        private final String nbtText;

        private MoveChestFilterRuleDraft(String itemName, String nbtText) {
            this.itemName = itemName == null ? "" : itemName.trim();
            this.nbtText = nbtText == null ? "" : nbtText.trim();
        }

        private boolean isEmpty() {
            return itemName.isEmpty() && nbtText.isEmpty();
        }
    }

    private static final int BTN_ID_FILL_NEAREST_CHEST = 300;
    static final int BTN_ID_SCAN_NEARBY_ENTITIES = 301;
    private static final int BTN_ID_SCAN_NEARBY_BLOCKS = 302;
    static final int BTN_ID_SELECT_RUN_SEQUENCE = 303;
    static final int BTN_ID_SELECT_CAPTURED_ID = 304;
    private static final int BTN_ID_ADD_MOVE_CHEST_NBT_TAG = 305;
    static final int BTN_ID_ADD_HUNT_WHITELIST = 306;
    static final int BTN_ID_ADD_HUNT_BLACKLIST = 307;
    static final int BTN_ID_FILL_FOLLOW_ENTITY_NAME = 312;
    static final int BTN_ID_SELECT_HUNT_ATTACK_SEQUENCE = 313;
    private static final int BTN_ID_TOGGLE_SEQUENCE_BUILTIN_DELAY = 308;
    static final int BTN_ID_ADD_CONDITION_INV_NBT_TAG = 309;
    static final int BTN_ID_SELECT_OTHER_FEATURE = 311;
    static final int BTN_ID_ADD_BOOLEAN_EXPRESSION = 314;
    static final int BTN_ID_EDIT_BOOLEAN_EXPRESSION = 315;
    static final int BTN_ID_DELETE_BOOLEAN_EXPRESSION = 316;
    static final int BTN_ID_MOVE_BOOLEAN_EXPRESSION_UP = 317;
    static final int BTN_ID_MOVE_BOOLEAN_EXPRESSION_DOWN = 318;
    private static final int BTN_ID_TOGGLE_SUMMARY_CARD = 610;
    private static final int BTN_ID_TOGGLE_VALIDATION_CARD = 611;
    private static final int BTN_ID_TOGGLE_HELP_CARD = 612;
    private static final int BTN_ID_DOCK_INFO_FLOAT = 613;
    private static final int BTN_ID_DOCK_INFO_MIDDLE = 614;
    private static final int BTN_ID_DOCK_INFO_BOTTOM = 615;
    private static final int BTN_ID_DOCK_INFO_RIGHT = 616;
    private static final int BTN_ID_SEQUENCE_BUILTIN_DELAY_TICKS = 617;
    private static final int BTN_ID_HEADER_TOOLBAR_PREV = 618;
    private static final int BTN_ID_HEADER_TOOLBAR_NEXT = 619;
    static final int BTN_ID_APPLY_HUNT_PRESET_BASE = 390;
    static final int BTN_ID_APPLY_RUN_SEQUENCE_PRESET_BASE = 400;
    private static final int BTN_ID_TEST_CURRENT_ACTION = 102;
    private static final int BTN_ID_COLOR_BASE = 320;
    private static final int BTN_ID_FORMAT_BASE = 360;
    private static final int BTN_ID_TOGGLE_BASE = 500;
    private static final String ACTION_MOVE_INVENTORY_ITEMS_TO_CHEST_SLOTS = "move_inventory_items_to_chest_slots";
    private static final String[] SYSTEM_MESSAGE_COLOR_CODES = new String[] {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
    };
    private static final String[] SYSTEM_MESSAGE_FORMAT_CODES = new String[] {
            "§l", "§n", "§o", "§m", "§k", "§r"
    };
    private static final String[] SYSTEM_MESSAGE_FORMAT_BUTTON_TEXTS = new String[] {
            "§l粗", "§n下", "§o斜", "§m删", "§k乱", "§r重"
    };
    private static final String[] CLICK_TYPE_DISPLAY_OPTIONS = new String[] {
            "普通点击", "Shift快速移动", "数字键交换", "丢弃", "收集同类", "拖拽分发", "创造复制"
    };
    static final String CONDITION_EXPRESSION_OPERATOR_CUSTOM = "自定义表达式";
    static final String CONDITION_EXPRESSION_OPERATOR_RANGE_CLOSED = "区间 [a,b]";
    static final String CONDITION_EXPRESSION_OPERATOR_RANGE_OPEN = "区间 (a,b)";
    static final String CONDITION_EXPRESSION_OPERATOR_RANGE_LEFT_CLOSED = "区间 [a,b)";
    static final String CONDITION_EXPRESSION_OPERATOR_RANGE_RIGHT_CLOSED = "区间 (a,b]";
    static final String[] SIMPLE_EXPRESSION_OPERATORS = new String[] {
            "==", "!=", ">", ">=", "<", "<=",
            CONDITION_EXPRESSION_OPERATOR_RANGE_CLOSED,
            CONDITION_EXPRESSION_OPERATOR_RANGE_OPEN,
            CONDITION_EXPRESSION_OPERATOR_RANGE_LEFT_CLOSED,
            CONDITION_EXPRESSION_OPERATOR_RANGE_RIGHT_CLOSED,
            CONDITION_EXPRESSION_OPERATOR_CUSTOM
    };
    static final String[] VARIABLE_SCOPE_DISPLAY_OPTIONS = new String[] {
            "序列变量(sequence)", "全局变量(global)", "局部变量(local)", "临时变量(temp)"
    };
    static final String SET_VAR_EXPRESSION_TEMPLATE_CUSTOM = "自定义";
    static final String VARIABLE_SELECTION_CLEAR = "（清空）";
    private static final int ACTION_LIST_COLLAPSED_WIDTH = 44;
    private static final int ACTION_LIST_MIN_WIDTH = 112;
    private static final int ACTION_LIST_MAX_WIDTH = 360;
    private static final int ACTION_PANE_DIVIDER_WIDTH = 12;
    private static final int ACTION_PANE_GAP = 6;
    private static final int ACTION_RIGHT_PANE_MIN_WIDTH = 176;
    static final int PARAM_INLINE_GAP = 8;
    static final int EXPRESSION_POPUP_SCROLL_STEP = 18;
    private static final int INFO_CARD_PANE_GAP = 10;
    private static final int INFO_CARD_PANE_MIN_WIDTH = 180;
    private static final int INFO_CARD_PANE_MAX_WIDTH = 260;
    private static final int EDITOR_PANE_MIN_WIDTH_WITH_INFO = 220;
    private static final int INFO_DOCK_DIVIDER_SIZE = 12;
    private static final int INFO_DOCK_DIVIDER_HIT_THICKNESS = 24;
    private static final String INFO_PANEL_DOCK_FLOAT = "float";
    private static final String INFO_PANEL_DOCK_MIDDLE = "middle";
    private static final String INFO_PANEL_DOCK_BOTTOM = "bottom";
    private static final String INFO_PANEL_DOCK_RIGHT = "right";
    static final int BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT = 152;
    static final int BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT = 36;
    static final int BOOLEAN_EXPRESSION_CARD_ROW_GAP = 6;
    static final int BOOLEAN_EXPRESSION_EDIT_NONE = Integer.MIN_VALUE;
    static final int BOOLEAN_EXPRESSION_EDIT_NEW = -1;
    private static final String PREFERENCES_FILE_NAME = "gui_action_editor_preferences.json";
    private static boolean preferencesLoaded = false;
    private static int savedActionListPreferredWidth = 220;
    private static boolean savedActionLibraryCollapsed = false;
    private static final LinkedHashSet<String> savedFavoriteActionTypes = new LinkedHashSet<>();
    private static final List<String> savedRecentActionTypes = new ArrayList<>();
    private static boolean savedShowSummaryInfoCard = false;
    private static boolean savedShowValidationInfoCard = false;
    private static boolean savedShowHelpInfoCard = false;
    private static int savedInfoPopupX = Integer.MIN_VALUE;
    private static int savedInfoPopupY = Integer.MIN_VALUE;
    private static int savedInfoPopupPreferredWidth = 280;
    private static int savedInfoPopupPreferredHeight = 232;
    private static String savedInfoPanelDockMode = "float";
    private static int savedInfoDockPreferredWidth = 240;
    private static int savedInfoDockPreferredHeight = 180;
    private static final int MAX_RECENT_ACTIONS = 8;
    private static final int[] BUILTIN_DELAY_TICK_PRESETS = new int[] { 0, 1, 3, 5, 10, 20, 40, 60 };
    private static final int SUMMARY_CARD_HEIGHT = 58;
    private static final int VALIDATION_CARD_HEIGHT = 46;
    private static final int HELP_CARD_HEIGHT = 38;
    private static final int FEEDBACK_CARD_GAP = 6;

    private final GuiScreen parentScreen;
    private final ActionData actionToEdit;
    private final Consumer<ActionData> onSave;
    private final String currentSequenceName;
    private final boolean isNew;

    private static final Map<String, String> ACTION_DISPLAY_KEYS = ActionDisplayCatalog.getActionDisplayKeys();
    private static final int ACTION_LIBRARY_ROW_HEIGHT = 20;
    private static final int ACTION_LIBRARY_INDENT = 12;


    List<GuiTextField> paramFields = new ArrayList<>();
    List<String> paramFieldKeys = new ArrayList<>();
    List<String> paramLabels = new ArrayList<>();
    private List<String> paramHelpTexts = new ArrayList<>();
    List<EnumDropdown> paramDropdowns = new ArrayList<>();
    List<String> paramDropdownKeys = new ArrayList<>();
    private List<String> paramDropdownLabels = new ArrayList<>();
    private List<String> paramDropdownHelpTexts = new ArrayList<>();
    List<GuiButton> skillButtons = new ArrayList<>();
    private List<GuiButton> messageColorButtons = new ArrayList<>();
    List<ToggleOptionButton> toggleButtons = new ArrayList<>();
    List<String> toggleKeys = new ArrayList<>();
    private List<String> toggleLabels = new ArrayList<>();
    private List<String> toggleHelpTexts = new ArrayList<>();
    private List<Integer> toggleBaseY = new ArrayList<>();
    private GuiButton btnFillNearestChest;
    GuiButton btnScanNearbyEntities;
    private GuiButton btnScanNearbyBlocks;
    GuiButton btnAddSelectedHuntWhitelist;
    GuiButton btnAddSelectedHuntBlacklist;
    EnumDropdown nearbyEntityDropdown;
    private EnumDropdown nearbyBlockDropdown;
    private final Map<String, String> nearbyEntityPosMap = new LinkedHashMap<>();
    private final Map<String, String> nearbyEntityNameMap = new LinkedHashMap<>();
    private final Map<String, String> nearbyBlockPosMap = new LinkedHashMap<>();
    private String selectedSkill = "R";
    private GuiTextField systemMessageField;
    GuiButton btnSelectRunSequence;
    GuiButton btnSelectHuntAttackSequence;
    GuiButton btnSelectOtherFeature;
    GuiButton btnSelectCapturedId;
    private GuiButton btnAddMoveChestNbtTag;
    GuiButton btnAddConditionInventoryNbtTag;
    GuiButton btnAddBooleanExpression;
    GuiButton btnEditBooleanExpression;
    GuiButton btnDeleteBooleanExpression;
    GuiButton btnMoveBooleanExpressionUp;
    GuiButton btnMoveBooleanExpressionDown;
    private GuiButton btnToggleSummaryCard;
    private GuiButton btnToggleValidationCard;
    private GuiButton btnToggleHelpCard;
    private GuiButton btnToggleSequenceBuiltinDelay;
    private GuiButton btnCycleSequenceBuiltinDelayTicks;
    private GuiButton btnHeaderToolbarPrev;
    private GuiButton btnHeaderToolbarNext;
    String selectedRunSequenceName = "";
    String selectedHuntAttackSequenceName = "";
    String selectedOtherFeatureId = "";
    String selectedCapturedIdName = "";
    String[] availableRuntimeVariableOptions = new String[] { "(选择变量)" };
    final Map<String, ScopedVariableEditorBinding> scopedVariableBindings = new LinkedHashMap<>();
    final Map<String, GroupedVariableSelectorBinding> groupedVariableBindings = new LinkedHashMap<>();
    final Map<String, LinkedHashMap<String, String>> expressionTemplateBindings = new LinkedHashMap<>();
    final Map<String, ExpressionEditorBinding> expressionEditorBindings = new LinkedHashMap<>();
    private GuiTextField moveChestItemNameInputField;
    private GuiTextField moveChestNbtTagInputField;
    GuiTextField conditionInventoryNbtTagInputField;
    private String moveChestDraftItemName = "";
    private String moveChestDraftNbtText = "";
    private final List<MoveChestFilterRuleDraft> moveChestFilterRules = new ArrayList<>();
    private final List<String> conditionInventoryRequiredNbtTags = new ArrayList<>();
    private final LinkedHashSet<Integer> moveChestSelectedChestSlots = new LinkedHashSet<>();
    private final LinkedHashSet<Integer> moveChestSelectedInventorySlots = new LinkedHashSet<>();
    private final LinkedHashSet<Integer> conditionInventorySelectedSlots = new LinkedHashSet<>();
    private final LinkedHashSet<Integer> moveChestDragSelectionSnapshot = new LinkedHashSet<>();
    private final LinkedHashSet<Integer> conditionInventoryDragSelectionSnapshot = new LinkedHashSet<>();
    private final List<IndexedHitRegion> moveChestTagRemoveRegions = new ArrayList<>();
    private final List<IndexedHitRegion> conditionInventoryTagRemoveRegions = new ArrayList<>();
    private final List<IndexedHitRegion> moveChestTargetSlotRegions = new ArrayList<>();
    private final List<IndexedHitRegion> moveChestInventorySlotRegions = new ArrayList<>();
    private final List<IndexedHitRegion> conditionInventorySlotRegions = new ArrayList<>();
    private boolean moveChestGridDragging = false;
    private boolean moveChestDraggingChestGrid = false;
    private boolean moveChestDragAddMode = true;
    private int moveChestDragAnchorIndex = -1;
    private int moveChestDragCurrentIndex = -1;
    private boolean conditionInventoryGridDragging = false;
    private boolean conditionInventoryDragAddMode = true;
    private int conditionInventoryDragAnchorIndex = -1;
    private int conditionInventoryDragCurrentIndex = -1;
    private int systemMessageColorLabelX;
    private int systemMessageColorLabelY;
    private int systemMessageFormatLabelX;
    private int systemMessageFormatLabelY;
    int runSequenceStatusLabelY;
    private int moveChestItemInputBaseY = -1;
    private int moveChestTagInputBaseY = -1;
    private int moveChestRuleButtonBaseY = -1;
    int conditionInventoryNbtTagInputBaseY = -1;
    int booleanExpressionToolbarBaseY = -1;
    int booleanExpressionCardListBaseY = -1;
    private int conditionInventoryNbtTagScrollOffset = 0;
    int booleanExpressionCardScrollOffset = 0;
    int selectedBooleanExpressionIndex = -1;
    private List<String> availableSequenceNames = new ArrayList<>();
    GuiTextField actionSearchField;
    private boolean actionLibraryCollapsed = false;
    private boolean draggingActionDivider = false;
    private int actionListPreferredWidth = 220;
    private int actionDividerX = 0;
    private int actionDividerY = 0;
    private int actionDividerHeight = 0;
    private int actionCollapseButtonX = 0;
    private int actionCollapseButtonY = 0;
    private int actionCollapseButtonWidth = 0;
    private int actionCollapseButtonHeight = 0;
    ConditionExpressionEditorBinding conditionExpressionBinding;

    private final List<String> availableActionTypes = new ArrayList<>(ActionDisplayCatalog.getOrderedActionTypes());
    private final List<ActionLibraryNode> actionLibraryRoots = new ArrayList<>();
    private final LinkedHashSet<String> expandedActionLibraryGroupIds = new LinkedHashSet<>();
    private final List<ActionLibraryVisibleRow> visibleActionLibraryRows = new ArrayList<>();
    private int selectedTypeIndex;
    JsonObject currentParams;

    private int actionListWidth = 210;
    private int actionListHeight;

    private int actionListScrollOffset = 0;
    private int maxActionListScroll = 0;
    private boolean isDraggingActionScrollbar = false;
    private boolean isDraggingParamScrollbar = false;
    boolean isDraggingExpressionPopupScrollbar = false;
    private int scrollClickY = 0;
    private int initialScrollOffset = 0;

    int paramScrollOffset = 0;
    private int maxParamScroll = 0;
    private final List<Integer> paramFieldBaseY = new ArrayList<>();
    private final List<Integer> paramDropdownBaseY = new ArrayList<>();
    private final Map<Integer, Integer> scrollableButtonBaseY = new HashMap<>();
    int nearbyEntityDropdownBaseY = -1;
    private int nearbyBlockDropdownBaseY = -1;
    private int systemMessageColorLabelBaseY = 0;
    private int systemMessageFormatLabelBaseY = 0;
    int runSequenceStatusLabelBaseY = 0;
    int paramViewTop = 0;
    int paramViewBottom = 0;
    boolean hasUnsavedChanges = false;
    String pendingSwitchActionType = null;
    private boolean sequenceBuiltinDelayEnabledDraft;
    private String sequenceBuiltinDelayTicksDraft;
    ExpressionEditorBinding activeExpressionEditorBinding = null;
    GuiTextField expressionPopupSearchField = null;
    GuiTextField expressionPopupInputField = null;
    String expressionPopupOriginalValue = "";
    int expressionPopupScrollOffset = 0;
    int expressionPopupMaxScroll = 0;
    int activeBooleanExpressionEditIndex = BOOLEAN_EXPRESSION_EDIT_NONE;
    String activeExpressionPopupTitle = "";
    boolean activeExpressionPopupBooleanOnly = false;
    private List<PathConfigValidator.Issue> liveValidationIssues = Collections.emptyList();
    private int liveValidationErrorCount = 0;
    private int liveValidationWarningCount = 0;
    private String liveActionSummary = "";
    private String liveActionEffectHint = "";
    private String liveActionRiskHint = "";
    final List<IndexedHitRegion> booleanExpressionCardRegions = new ArrayList<>();
    private boolean showSummaryInfoCard = true;
    private boolean showValidationInfoCard = true;
    private boolean showHelpInfoCard = true;
    private String infoPanelDockMode = "float";
    private int infoDockPreferredWidth = 240;
    private int infoDockPreferredHeight = 180;
    private boolean draggingInfoDockDivider = false;
    private Rectangle infoDockDividerBounds = null;
    private boolean draggingInfoPopup = false;
    private int infoPopupX = Integer.MIN_VALUE;
    private int infoPopupY = Integer.MIN_VALUE;
    private int infoPopupLastFloatX = Integer.MIN_VALUE;
    private int infoPopupLastFloatY = Integer.MIN_VALUE;
    private int infoPopupWidth = 0;
    private int infoPopupHeight = 0;
    private int infoPopupDragOffsetX = 0;
    private int infoPopupDragOffsetY = 0;
    private int infoPopupPreferredWidth = 280;
    private int infoPopupPreferredHeight = 232;
    private int infoPopupScrollOffset = 0;
    private int infoPopupMaxScroll = 0;
    private Rectangle infoPopupBounds = null;
    private Rectangle infoPopupHeaderBounds = null;
    private Rectangle infoPopupCloseBounds = null;
    private Rectangle infoPopupDockFloatBounds = null;
    private Rectangle infoPopupDockMiddleBounds = null;
    private Rectangle infoPopupDockBottomBounds = null;
    private Rectangle infoPopupDockRightBounds = null;
    private Rectangle infoPopupContentBounds = null;
    private Rectangle infoPopupResizeRightBounds = null;
    private Rectangle infoPopupResizeBottomBounds = null;
    private Rectangle infoPopupResizeCornerBounds = null;
    private Rectangle headerToolbarViewportBounds = null;
    private boolean resizingInfoPopupRight = false;
    private boolean resizingInfoPopupBottom = false;
    private boolean resizingInfoPopupCorner = false;
    private int infoPopupResizeStartMouseX = 0;
    private int infoPopupResizeStartMouseY = 0;
    private int infoPopupResizeStartWidth = 0;
    private int infoPopupResizeStartHeight = 0;
    private int headerToolbarScrollIndex = 0;
    private int headerToolbarMaxScrollIndex = 0;

    private int getEditorPanelWidth() {
        return Math.min(960, this.width - 12);
    }

    private int getEditorPanelHeight() {
        return Math.min(540, this.height - 12);
    }

    private int getEditorPanelX() {
        return (this.width - getEditorPanelWidth()) / 2;
    }

    private int getEditorPanelY() {
        return (this.height - getEditorPanelHeight()) / 2;
    }

    private int getContentTop() {
        return getEditorPanelY() + 32;
    }

    private int getBottomButtonY() {
        return getEditorPanelY() + getEditorPanelHeight() - 28;
    }

    private int getBottomOptionY() {
        return getBottomButtonY() - 26;
    }

    private int getPaneHeight() {
        return getBottomButtonY() - getContentTop() - 8;
    }

    private int getActionListX() {
        return getEditorPanelX() + 10;
    }

    private int getActionListY() {
        return getContentTop();
    }

    private int getActionListTop() {
        return getActionListY() + 48;
    }

    private int getActionListContentHeight() {
        return Math.max(20, actionListHeight - 52);
    }

    private int getRightPaneX() {
        return getActionListX() + actionListWidth + ACTION_PANE_DIVIDER_WIDTH + ACTION_PANE_GAP;
    }

    private int getRightPaneY() {
        return getContentTop();
    }

    private int getRightPaneWidth() {
        return getEditorPanelX() + getEditorPanelWidth() - getRightPaneX() - 10;
    }

    private int getRightPaneHeight() {
        return getPaneHeight();
    }

    private boolean hasVisibleInfoCards() {
        return hasActiveInfoSections() && !INFO_PANEL_DOCK_FLOAT.equals(infoPanelDockMode);
    }

    private boolean hasActiveInfoSections() {
        return showSummaryInfoCard || showValidationInfoCard || showHelpInfoCard;
    }

    private boolean isInfoPanelDockedMiddle() {
        return hasActiveInfoSections() && INFO_PANEL_DOCK_MIDDLE.equals(infoPanelDockMode);
    }

    private boolean isInfoPanelDockedRight() {
        return hasActiveInfoSections() && INFO_PANEL_DOCK_RIGHT.equals(infoPanelDockMode);
    }

    private boolean isInfoPanelDockedBottom() {
        return hasActiveInfoSections() && INFO_PANEL_DOCK_BOTTOM.equals(infoPanelDockMode);
    }

    private boolean isInfoPanelFloating() {
        return hasActiveInfoSections() && !hasVisibleInfoCards();
    }

    private int getRequiredRightPaneMinWidth() {
        if (isInfoPanelDockedMiddle() || isInfoPanelDockedRight()) {
            return INFO_CARD_PANE_MIN_WIDTH + INFO_CARD_PANE_GAP + EDITOR_PANE_MIN_WIDTH_WITH_INFO;
        }
        return ACTION_RIGHT_PANE_MIN_WIDTH;
    }

    private int getHeaderToggleButtonGap() {
        return 6;
    }

    private int getHeaderToggleButtonWidth() {
        return 48;
    }

    private int getHeaderModeButtonWidth() {
        return 86;
    }

    private int getHeaderButtonsTotalWidth() {
        int total = 0;
        boolean first = true;
        for (GuiButton button : getHeaderToolbarButtons()) {
            if (button == null) {
                continue;
            }
            String text = TextFormatting.getTextWithoutFormattingCodes(button.displayString);
            int width = MathHelper.clamp(fontRenderer.getStringWidth(text == null ? "" : text) + 18, 46, 118);
            total += first ? width : width + getHeaderToggleButtonGap();
            first = false;
        }
        return total;
    }

    private boolean shouldWrapEditorHeaderButtons() {
        return false;
    }

    private int getEditorHeaderHeight() {
        return shouldWrapEditorHeaderButtons() ? 54 : 30;
    }

    private int getEditorColumnsTop() {
        return getRightPaneY();
    }

    private int getEditorColumnsHeight() {
        return getRightPaneHeight();
    }

    private int getInfoCardPaneX() {
        if (isInfoPanelDockedMiddle()) {
            return getRightPaneX();
        }
        if (isInfoPanelDockedRight()) {
            return getRightPaneX() + getEditorPaneWidth() + INFO_CARD_PANE_GAP;
        }
        return getRightPaneX();
    }

    private int getInfoCardPaneY() {
        if (isInfoPanelDockedBottom()) {
            return getRightPaneY() + getEditorPaneHeight() + INFO_CARD_PANE_GAP;
        }
        return getRightPaneY();
    }

    private int getInfoCardPaneWidth() {
        if (!hasVisibleInfoCards()) {
            return 0;
        }
        if (isInfoPanelDockedBottom()) {
            return Math.max(120, getRightPaneWidth());
        }
        int desired = MathHelper.clamp(infoDockPreferredWidth, INFO_CARD_PANE_MIN_WIDTH, INFO_CARD_PANE_MAX_WIDTH);
        int maxAllowed = Math.max(INFO_CARD_PANE_MIN_WIDTH,
                getRightPaneWidth() - INFO_CARD_PANE_GAP - EDITOR_PANE_MIN_WIDTH_WITH_INFO);
        return Math.min(desired, maxAllowed);
    }

    private int getInfoCardPaneHeight() {
        if (isInfoPanelDockedBottom()) {
            return Math.min(Math.max(120, infoDockPreferredHeight), Math.max(120, getRightPaneHeight() - 80));
        }
        return getRightPaneHeight();
    }

    private int getEditorPaneX() {
        if (isInfoPanelDockedMiddle()) {
            return getRightPaneX() + getInfoCardPaneWidth() + INFO_CARD_PANE_GAP;
        }
        return getRightPaneX();
    }

    private int getEditorPaneY() {
        return getRightPaneY();
    }

    private int getEditorPaneWidth() {
        if (isInfoPanelDockedMiddle()) {
            return Math.max(1, getRightPaneWidth() - getInfoCardPaneWidth() - INFO_CARD_PANE_GAP);
        }
        if (isInfoPanelDockedRight()) {
            return Math.max(1, getRightPaneWidth() - getInfoCardPaneWidth() - INFO_CARD_PANE_GAP);
        }
        return getRightPaneWidth();
    }

    private int getEditorPaneHeight() {
        if (isInfoPanelDockedBottom()) {
            return Math.max(80, getRightPaneHeight() - getInfoCardPaneHeight() - INFO_CARD_PANE_GAP);
        }
        return getRightPaneHeight();
    }

    private int getParamEditorTop() {
        return getEditorPaneY() + getEditorHeaderHeight() + 6;
    }

    int getParamContentX() {
        return getEditorPaneX() + 12;
    }

    int getParamFieldWidth() {
        return Math.max(1, getEditorPaneWidth() - 24);
    }

    boolean isCompactParamLayout(int width) {
        return width < 300;
    }

    private int clampActionListPreferredWidth(int preferredWidth) {
        int maxWidth = Math.min(ACTION_LIST_MAX_WIDTH, Math.max(72, getEditorPanelWidth()
                - 20 - ACTION_PANE_DIVIDER_WIDTH - ACTION_PANE_GAP - getRequiredRightPaneMinWidth()));
        int minWidth = Math.min(ACTION_LIST_MIN_WIDTH, maxWidth);
        return MathHelper.clamp(preferredWidth, minWidth, maxWidth);
    }

    private void updateActionPaneMetrics() {
        this.actionListPreferredWidth = clampActionListPreferredWidth(actionListPreferredWidth);
        this.actionListWidth = actionLibraryCollapsed ? ACTION_LIST_COLLAPSED_WIDTH : actionListPreferredWidth;
        this.actionDividerX = getActionListX() + actionListWidth;
        this.actionDividerY = getActionListY() + 4;
        this.actionDividerHeight = Math.max(40, getPaneHeight() - 8);
    }

    private static synchronized void ensurePreferencesLoaded() {
        if (preferencesLoaded) {
            return;
        }
        preferencesLoaded = true;
        GuiActionEditorPreferences preferences = GuiActionEditorPreferences.load(PREFERENCES_FILE_NAME,
                ACTION_LIST_MIN_WIDTH, 220, MAX_RECENT_ACTIONS);
        savedActionListPreferredWidth = preferences.actionListPreferredWidth;
        savedActionLibraryCollapsed = preferences.actionLibraryCollapsed;
        savedFavoriteActionTypes.clear();
        savedFavoriteActionTypes.addAll(preferences.favoriteActionTypes);
        savedRecentActionTypes.clear();
        savedRecentActionTypes.addAll(preferences.recentActionTypes);

        JsonObject root = loadActionEditorPreferencesRoot();
        if (root != null) {
            if (root.has("showSummaryInfoCard")) {
                savedShowSummaryInfoCard = root.get("showSummaryInfoCard").getAsBoolean();
            }
            if (root.has("showValidationInfoCard")) {
                savedShowValidationInfoCard = root.get("showValidationInfoCard").getAsBoolean();
            }
            if (root.has("showHelpInfoCard")) {
                savedShowHelpInfoCard = root.get("showHelpInfoCard").getAsBoolean();
            }
            if (root.has("infoPopupX")) {
                savedInfoPopupX = root.get("infoPopupX").getAsInt();
            }
            if (root.has("infoPopupY")) {
                savedInfoPopupY = root.get("infoPopupY").getAsInt();
            }
            if (root.has("infoPopupPreferredWidth")) {
                savedInfoPopupPreferredWidth = root.get("infoPopupPreferredWidth").getAsInt();
            }
            if (root.has("infoPopupPreferredHeight")) {
                savedInfoPopupPreferredHeight = root.get("infoPopupPreferredHeight").getAsInt();
            }
            if (root.has("infoPanelDockMode")) {
                savedInfoPanelDockMode = normalizeInfoPanelDockMode(root.get("infoPanelDockMode").getAsString());
            }
            if (root.has("infoDockPreferredWidth")) {
                savedInfoDockPreferredWidth = root.get("infoDockPreferredWidth").getAsInt();
            }
            if (root.has("infoDockPreferredHeight")) {
                savedInfoDockPreferredHeight = root.get("infoDockPreferredHeight").getAsInt();
            }
        }
    }

    private static synchronized void savePreferences() {
        GuiActionEditorPreferences.save(PREFERENCES_FILE_NAME, savedActionListPreferredWidth,
                savedActionLibraryCollapsed, true, savedFavoriteActionTypes, savedRecentActionTypes);
        JsonObject root = loadActionEditorPreferencesRoot();
        if (root == null) {
            root = new JsonObject();
        }
        root.addProperty("showSummaryInfoCard", savedShowSummaryInfoCard);
        root.addProperty("showValidationInfoCard", savedShowValidationInfoCard);
        root.addProperty("showHelpInfoCard", savedShowHelpInfoCard);
        root.addProperty("infoPopupX", savedInfoPopupX);
        root.addProperty("infoPopupY", savedInfoPopupY);
        root.addProperty("infoPopupPreferredWidth", savedInfoPopupPreferredWidth);
        root.addProperty("infoPopupPreferredHeight", savedInfoPopupPreferredHeight);
        root.addProperty("infoPanelDockMode", normalizeInfoPanelDockMode(savedInfoPanelDockMode));
        root.addProperty("infoDockPreferredWidth", savedInfoDockPreferredWidth);
        root.addProperty("infoDockPreferredHeight", savedInfoDockPreferredHeight);
        saveActionEditorPreferencesRoot(root);
    }

    private static JsonObject loadActionEditorPreferencesRoot() {
        try {
            Path path = getActionEditorPreferencesPath();
            if (path == null || !Files.exists(path)) {
                return null;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                return root == null ? null : root;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void saveActionEditorPreferencesRoot(JsonObject root) {
        try {
            Path path = getActionEditorPreferencesPath();
            if (path == null || root == null) {
                return;
            }
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private static Path getActionEditorPreferencesPath() {
        try {
            Path profileDir = ProfileManager.getCurrentProfileDir();
            return profileDir == null ? null : profileDir.resolve(PREFERENCES_FILE_NAME);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeInfoPanelDockMode(String dockMode) {
        String normalized = dockMode == null ? "" : dockMode.trim().toLowerCase(Locale.ROOT);
        if (INFO_PANEL_DOCK_MIDDLE.equals(normalized)
                || INFO_PANEL_DOCK_BOTTOM.equals(normalized)
                || INFO_PANEL_DOCK_RIGHT.equals(normalized)) {
            return normalized;
        }
        return INFO_PANEL_DOCK_FLOAT;
    }

    private void updateActionSearchFieldBounds() {
        if (actionSearchField == null) {
            return;
        }
        this.actionCollapseButtonWidth = 20;
        this.actionCollapseButtonHeight = 18;
        this.actionCollapseButtonX = getActionListX() + Math.max(8, actionListWidth - actionCollapseButtonWidth - 8);
        this.actionCollapseButtonY = getActionListY() + 20;
        if (actionLibraryCollapsed) {
            actionSearchField.x = getActionListX() + 8;
            actionSearchField.y = getActionListY() + 20;
            actionSearchField.width = 0;
            actionSearchField.height = 18;
            actionSearchField.setFocused(false);
            return;
        }
        actionSearchField.x = getActionListX() + 10;
        actionSearchField.y = getActionListY() + 20;
        actionSearchField.width = Math.max(72, actionListWidth - 40);
        actionSearchField.height = 18;
    }

    private boolean isPointInsideActionCollapseButton(int mouseX, int mouseY) {
        return isPointInside(mouseX, mouseY, actionCollapseButtonX, actionCollapseButtonY,
                actionCollapseButtonWidth, actionCollapseButtonHeight);
    }

    private int getEventMouseX() {
        return Mouse.getEventX() * this.width / this.mc.displayWidth;
    }

    private int getEventMouseY() {
        return this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
    }

    private boolean shouldHideInNewActionPanel(String actionType) {
        if (!isNew) {
            return false;
        }
        String normalized = actionType == null ? "" : actionType.trim().toLowerCase(Locale.ROOT);
        return "autoeat".equals(normalized)
                || "autoequip".equals(normalized)
                || "autopickup".equals(normalized);
    }

    private void rebuildAvailableActionTypes() {
        availableActionTypes.clear();
        for (String actionType : ACTION_DISPLAY_KEYS.keySet()) {
            if (shouldHideInNewActionPanel(actionType)) {
                continue;
            }
            if ("conditional_window_click".equalsIgnoreCase(actionType)
                    || "autochestclick".equalsIgnoreCase(actionType)) {
                continue;
            }
            availableActionTypes.add(actionType);
        }

        String currentType = this.actionToEdit == null || this.actionToEdit.type == null
                ? ""
                : this.actionToEdit.type.trim().toLowerCase(Locale.ROOT);
        if (!currentType.isEmpty()
                && !"run_template".equalsIgnoreCase(currentType)
                && !availableActionTypes.contains(currentType)) {
            availableActionTypes.add(currentType);
        }
    }

    private ActionData migrateLegacyTemplateAction(ActionData action) {
        if (action == null || action.type == null || !"run_template".equalsIgnoreCase(action.type)) {
            return action;
        }
        JsonObject migratedParams = action.params == null
                ? new JsonObject()
                : new JsonParser().parse(action.params.toString()).getAsJsonObject();
        String templateName = migratedParams.has("templateName") ? migratedParams.get("templateName").getAsString()
                : "";
        String targetSequence = LegacyActionTemplateManager.resolveTemplateTargetSequence(templateName);
        if (targetSequence != null && !targetSequence.trim().isEmpty()) {
            migratedParams.addProperty("sequenceName", targetSequence.trim());
        } else {
            migratedParams.remove("sequenceName");
        }
        migratedParams.remove("templateName");
        migratedParams.remove("paramsText");
        return new ActionData("run_sequence", migratedParams);
    }

    public GuiActionEditor(GuiScreen parent, ActionData action, Consumer<ActionData> onSaveCallback) {
        this(parent, action, onSaveCallback, null);
    }

    public GuiActionEditor(GuiScreen parent, ActionData action, Consumer<ActionData> onSaveCallback,
            String currentSequenceName) {
        this.parentScreen = parent;
        this.onSave = onSaveCallback;
        this.currentSequenceName = currentSequenceName;
        ensurePreferencesLoaded();
        this.actionListPreferredWidth = savedActionListPreferredWidth;
        this.actionLibraryCollapsed = savedActionLibraryCollapsed;
        this.showSummaryInfoCard = savedShowSummaryInfoCard;
        this.showValidationInfoCard = savedShowValidationInfoCard;
        this.showHelpInfoCard = savedShowHelpInfoCard;
        this.infoPopupX = savedInfoPopupX;
        this.infoPopupY = savedInfoPopupY;
        this.infoPopupLastFloatX = savedInfoPopupX;
        this.infoPopupLastFloatY = savedInfoPopupY;
        this.infoPopupPreferredWidth = savedInfoPopupPreferredWidth;
        this.infoPopupPreferredHeight = savedInfoPopupPreferredHeight;
        this.infoPanelDockMode = savedInfoPanelDockMode;
        this.infoDockPreferredWidth = savedInfoDockPreferredWidth;
        this.infoDockPreferredHeight = savedInfoDockPreferredHeight;

        if (action == null) {
            this.isNew = true;
            this.actionToEdit = new ActionData("delay", new JsonObject());
            this.actionToEdit.params.addProperty("ticks", 20);
        } else {
            this.isNew = false;
            this.actionToEdit = migrateLegacyTemplateAction(action);
        }

        rebuildAvailableActionTypes();

        String normalizedActionType = this.actionToEdit.type == null
                ? ""
                : this.actionToEdit.type.trim().toLowerCase(Locale.ROOT);
        this.selectedTypeIndex = availableActionTypes.indexOf(normalizedActionType);
        if (this.selectedTypeIndex == -1) {
            this.selectedTypeIndex = 0;
        }

        this.currentParams = new JsonParser().parse(this.actionToEdit.params.toString()).getAsJsonObject();

        if ("use_skill".equalsIgnoreCase(this.actionToEdit.type) && this.currentParams.has("skill")) {
            this.selectedSkill = this.currentParams.get("skill").getAsString();
        }

        this.sequenceBuiltinDelayEnabledDraft = PathSequenceEventListener.isBuiltinSequenceDelayEnabled();
        this.sequenceBuiltinDelayTicksDraft = String.valueOf(PathSequenceEventListener.getBuiltinSequenceDelayTicks());
        refreshAvailableSequenceNames();
        refreshAvailableRuntimeVariables();
    }

    @Override
    public void onGuiClosed() {
        syncInfoPanelPreferences();
        savePreferences();
        super.onGuiClosed();
    }

    private void refreshAvailableRuntimeVariables() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add("(选择变量)");
        names.add("trigger");
        names.add("triggerType");
        for (ActionVariableRegistry.VariableEntry entry : ActionVariableRegistry
                .collectVariables(PathSequenceManager.getAllSequences())) {
            if (entry == null) {
                continue;
            }
            String variableName = entry.getVariableName();
            if (variableName != null && !variableName.trim().isEmpty()) {
                names.add(variableName.trim());
            }
        }
        this.availableRuntimeVariableOptions = names.toArray(new String[0]);
    }

    private void refreshAvailableSequenceNames() {
        this.availableSequenceNames.clear();
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (sequence == null || sequence.getName() == null) {
                continue;
            }
            if (currentSequenceName != null && currentSequenceName.equalsIgnoreCase(sequence.getName())) {
                continue;
            }
            this.availableSequenceNames.add(sequence.getName());
        }
        this.availableSequenceNames.sort(String::compareToIgnoreCase);
        if (this.availableSequenceNames.isEmpty()) {
            this.availableSequenceNames.add(I18n.format("gui.path.action_editor.option.no_sequence_available"));
        }
    }

    private String getActionSearchText() {
        return actionSearchField == null ? "" : PinyinSearchHelper.normalizeQuery(actionSearchField.getText());
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.paramFields.clear();
        this.paramFieldKeys.clear();
        this.paramLabels.clear();
        this.paramHelpTexts.clear();
        this.paramDropdowns.clear();
        this.paramDropdownKeys.clear();
        this.paramDropdownLabels.clear();
        this.paramDropdownHelpTexts.clear();
        this.scopedVariableBindings.clear();
        this.groupedVariableBindings.clear();
        this.expressionTemplateBindings.clear();
        this.expressionEditorBindings.clear();
        this.skillButtons.clear();
        this.messageColorButtons.clear();
        this.toggleButtons.clear();
        this.toggleKeys.clear();
        this.toggleLabels.clear();
        this.toggleHelpTexts.clear();
        this.toggleBaseY.clear();
        this.btnFillNearestChest = null;
        this.btnScanNearbyEntities = null;
        this.btnScanNearbyBlocks = null;
        this.btnSelectRunSequence = null;
        this.btnSelectHuntAttackSequence = null;
        this.btnSelectOtherFeature = null;
        this.btnSelectCapturedId = null;
        this.btnAddMoveChestNbtTag = null;
        this.btnAddConditionInventoryNbtTag = null;
        this.btnAddBooleanExpression = null;
        this.btnEditBooleanExpression = null;
        this.btnDeleteBooleanExpression = null;
        this.btnMoveBooleanExpressionUp = null;
        this.btnMoveBooleanExpressionDown = null;
        this.btnToggleSummaryCard = null;
        this.btnToggleValidationCard = null;
        this.btnToggleHelpCard = null;
        this.btnToggleSequenceBuiltinDelay = null;
        this.btnCycleSequenceBuiltinDelayTicks = null;
        this.btnHeaderToolbarPrev = null;
        this.btnHeaderToolbarNext = null;
        this.headerToolbarViewportBounds = null;
        this.headerToolbarScrollIndex = 0;
        this.headerToolbarMaxScrollIndex = 0;
        this.btnAddSelectedHuntWhitelist = null;
        this.btnAddSelectedHuntBlacklist = null;
        this.nearbyEntityDropdown = null;
        this.nearbyBlockDropdown = null;
        this.nearbyEntityPosMap.clear();
        this.nearbyEntityNameMap.clear();
        this.activeExpressionEditorBinding = null;
        this.expressionPopupSearchField = null;
        this.expressionPopupInputField = null;
        this.expressionPopupOriginalValue = "";
        this.expressionPopupScrollOffset = 0;
        this.expressionPopupMaxScroll = 0;
        this.isDraggingExpressionPopupScrollbar = false;
        this.activeBooleanExpressionEditIndex = BOOLEAN_EXPRESSION_EDIT_NONE;
        this.activeExpressionPopupTitle = "";
        this.activeExpressionPopupBooleanOnly = false;
        this.nearbyBlockPosMap.clear();
        this.systemMessageField = null;
        this.moveChestItemNameInputField = null;
        this.moveChestNbtTagInputField = null;
        this.conditionInventoryNbtTagInputField = null;
        this.moveChestDraftItemName = "";
        this.moveChestDraftNbtText = "";
        this.moveChestFilterRules.clear();
        this.conditionInventoryRequiredNbtTags.clear();
        this.moveChestSelectedChestSlots.clear();
        this.moveChestSelectedInventorySlots.clear();
        this.conditionInventorySelectedSlots.clear();
        this.moveChestDragSelectionSnapshot.clear();
        this.conditionInventoryDragSelectionSnapshot.clear();
        this.moveChestTagRemoveRegions.clear();
        this.conditionInventoryTagRemoveRegions.clear();
        this.moveChestTargetSlotRegions.clear();
        this.moveChestInventorySlotRegions.clear();
        this.conditionInventorySlotRegions.clear();
        this.moveChestGridDragging = false;
        this.moveChestDraggingChestGrid = false;
        this.moveChestDragAddMode = true;
        this.moveChestDragAnchorIndex = -1;
        this.moveChestDragCurrentIndex = -1;
        this.conditionInventoryGridDragging = false;
        this.conditionInventoryDragAddMode = true;
        this.conditionInventoryDragAnchorIndex = -1;
        this.conditionInventoryDragCurrentIndex = -1;
        this.systemMessageColorLabelX = 0;
        this.systemMessageColorLabelY = 0;
        this.systemMessageFormatLabelX = 0;
        this.systemMessageFormatLabelY = 0;
        this.runSequenceStatusLabelY = 0;
        this.moveChestItemInputBaseY = -1;
        this.moveChestTagInputBaseY = -1;
        this.moveChestRuleButtonBaseY = -1;
        this.conditionInventoryNbtTagInputBaseY = -1;
        this.booleanExpressionToolbarBaseY = -1;
        this.booleanExpressionCardListBaseY = -1;
        this.conditionInventoryNbtTagScrollOffset = 0;
        this.booleanExpressionCardScrollOffset = 0;
        this.booleanExpressionCardRegions.clear();
        this.paramFieldBaseY.clear();
        this.paramDropdownBaseY.clear();
        this.scrollableButtonBaseY.clear();
        this.nearbyEntityDropdownBaseY = -1;
        this.nearbyBlockDropdownBaseY = -1;
        this.systemMessageColorLabelBaseY = 0;
        this.systemMessageFormatLabelBaseY = 0;
        this.runSequenceStatusLabelBaseY = 0;
        this.hasUnsavedChanges = false;
        this.pendingSwitchActionType = null;
        this.conditionExpressionBinding = null;

        int panelWidth = getEditorPanelWidth();
        int panelX = getEditorPanelX();
        int bottomButtonY = getBottomButtonY();

        int defaultActionWidth = Math.max(ACTION_LIST_MIN_WIDTH, Math.min(260, panelWidth / 3));
        if (this.actionListPreferredWidth <= 0) {
            this.actionListPreferredWidth = defaultActionWidth;
        }
        this.actionListPreferredWidth = clampActionListPreferredWidth(this.actionListPreferredWidth);
        updateActionPaneMetrics();

        int rightPaneX = getRightPaneX();
        int rightPaneY = getRightPaneY();
        int rightPaneWidth = getRightPaneWidth();

        int actionListX = getActionListX();
        int actionListY = getActionListY();
        String previousSearch = this.actionSearchField == null ? "" : this.actionSearchField.getText();

        this.actionSearchField = new GuiTextField(900, this.fontRenderer, actionListX + 10, actionListY + 20,
                Math.max(72, actionListWidth - 40), 18);
        this.actionSearchField.setMaxStringLength(120);
        this.actionSearchField.setEnableBackgroundDrawing(false);
        this.actionSearchField.setText(previousSearch);
        updateActionSearchFieldBounds();

        actionListHeight = getPaneHeight();

        this.paramViewTop = getParamEditorTop();
        this.paramViewBottom = getEditorPaneY() + getEditorPaneHeight() - 8;

        initializeActionLibraryTreeIfNeeded();
        rebuildActionLibraryAndScrollBounds();

        generateParamFields(getParamContentX(), paramViewTop + 6);
        refreshDynamicParamLayout();

        this.btnHeaderToolbarPrev = new ThemedButton(BTN_ID_HEADER_TOOLBAR_PREV, rightPaneX, rightPaneY, 18, 18, "<");
        this.btnHeaderToolbarNext = new ThemedButton(BTN_ID_HEADER_TOOLBAR_NEXT, rightPaneX, rightPaneY, 18, 18, ">");
        this.buttonList.add(this.btnHeaderToolbarPrev);
        this.buttonList.add(this.btnHeaderToolbarNext);
        this.btnToggleSequenceBuiltinDelay = new ThemedButton(BTN_ID_TOGGLE_SEQUENCE_BUILTIN_DELAY, rightPaneX, rightPaneY,
                96, 18, "");
        refreshSequenceBuiltinDelayToolbarText();
        this.buttonList.add(this.btnToggleSequenceBuiltinDelay);
        this.btnCycleSequenceBuiltinDelayTicks = new ThemedButton(BTN_ID_SEQUENCE_BUILTIN_DELAY_TICKS, rightPaneX, rightPaneY,
                78, 18, "");
        refreshSequenceBuiltinDelayTicksToolbarText();
        this.buttonList.add(this.btnCycleSequenceBuiltinDelayTicks);
        this.btnToggleSummaryCard = new ThemedButton(BTN_ID_TOGGLE_SUMMARY_CARD, rightPaneX, rightPaneY, 48, 18, "");
        this.btnToggleValidationCard = new ThemedButton(BTN_ID_TOGGLE_VALIDATION_CARD, rightPaneX, rightPaneY, 48, 18,
                "");
        this.btnToggleHelpCard = new ThemedButton(BTN_ID_TOGGLE_HELP_CARD, rightPaneX, rightPaneY, 48, 18, "");
        refreshInfoCardToggleButtonText();
        this.buttonList.add(this.btnToggleSummaryCard);
        this.buttonList.add(this.btnToggleValidationCard);
        this.buttonList.add(this.btnToggleHelpCard);
        updateRightHeaderButtonLayout();

        int footerGap = 10;
        int footerButtonWidth = (panelWidth - 20 - footerGap * 2) / 3;
        int footerButtonX = panelX + 10;
        this.buttonList.add(new ThemedButton(100, footerButtonX, bottomButtonY, footerButtonWidth, 20,
                "§a" + I18n.format("gui.path.action_editor.save")));
        footerButtonX += footerButtonWidth + footerGap;
        this.buttonList.add(new ThemedButton(BTN_ID_TEST_CURRENT_ACTION, footerButtonX, bottomButtonY, footerButtonWidth, 20,
                "§b测试当前动作"));
        footerButtonX += footerButtonWidth + footerGap;
        this.buttonList.add(new ThemedButton(101, footerButtonX, bottomButtonY, footerButtonWidth, 20,
                "§c" + I18n.format("gui.path.action_editor.cancel")));
    }

    void refreshDynamicParamLayout() {
        updateActionPaneMetrics();
        updateActionSearchFieldBounds();
        this.paramViewTop = getParamEditorTop();
        this.paramViewBottom = getEditorPaneY() + getEditorPaneHeight() - 8;
        updateRightHeaderButtonLayout();
        updateBooleanExpressionControlLayout();
        updateConditionInventoryNbtControlLayout();
        updateMoveChestCustomControlLayout();
        recomputeParamScrollBounds();
        paramScrollOffset = Math.max(0, Math.min(paramScrollOffset, maxParamScroll));
        updateScrollableControlPositions();
        updateBooleanExpressionControlLayout();
        updateConditionInventoryNbtControlLayout();
        updateMoveChestCustomControlLayout();
    }

    private void refreshSequenceBuiltinDelayToolbarText() {
        if (btnToggleSequenceBuiltinDelay != null) {
            String state = sequenceBuiltinDelayEnabledDraft ? "§a开" : "§c关";
            btnToggleSequenceBuiltinDelay.displayString = "§b内置延迟 " + state;
        }
    }

    private void refreshSequenceBuiltinDelayTicksToolbarText() {
        if (btnCycleSequenceBuiltinDelayTicks != null) {
            btnCycleSequenceBuiltinDelayTicks.displayString = "§7延迟 " + getSequenceBuiltinDelayTicksDraftValue() + "t";
        }
    }

    private void refreshInfoCardToggleButtonText() {
        if (btnToggleSummaryCard != null) {
            btnToggleSummaryCard.displayString = showSummaryInfoCard ? "§a摘要" : "§8摘要";
        }
        if (btnToggleValidationCard != null) {
            btnToggleValidationCard.displayString = showValidationInfoCard ? "§a检查" : "§8检查";
        }
        if (btnToggleHelpCard != null) {
            btnToggleHelpCard.displayString = showHelpInfoCard ? "§a提示" : "§8提示";
        }
    }

    private List<GuiButton> getHeaderToolbarButtons() {
        return Arrays.asList(btnToggleSummaryCard, btnToggleValidationCard, btnToggleHelpCard,
                btnToggleSequenceBuiltinDelay, btnCycleSequenceBuiltinDelayTicks);
    }

    private void updateRightHeaderButtonLayout() {
        if (fontRenderer == null || btnHeaderToolbarPrev == null || btnHeaderToolbarNext == null) {
            return;
        }
        int titleX = getEditorPanelX() + 12;
        String title = isNew ? I18n.format("gui.path.action_editor.title_add") : I18n.format("gui.path.action_editor.title_edit");
        int titleWidth = fontRenderer == null ? 120 : fontRenderer.getStringWidth(title);
        int toolbarX = titleX + titleWidth + 16;
        int toolbarY = getEditorPanelY() + 1;
        int toolbarHeight = 18;
        int toolbarRight = getEditorPanelX() + getEditorPanelWidth() - 8;
        int toolbarWidth = Math.max(0, toolbarRight - toolbarX);
        int arrowWidth = 18;
        int gap = getHeaderToggleButtonGap();

        List<GuiButton> buttons = getHeaderToolbarButtons();
        List<Integer> buttonWidths = new ArrayList<Integer>();
        int totalWidth = 0;
        for (GuiButton button : buttons) {
            String text = button == null ? "" : TextFormatting.getTextWithoutFormattingCodes(button.displayString);
            int width = MathHelper.clamp(fontRenderer.getStringWidth(text == null ? "" : text) + 18, 46, 118);
            buttonWidths.add(width);
            totalWidth += width;
        }
        totalWidth += gap * Math.max(0, buttons.size() - 1);

        boolean overflow = totalWidth > toolbarWidth;
        btnHeaderToolbarPrev.visible = overflow;
        btnHeaderToolbarNext.visible = overflow;
        btnHeaderToolbarPrev.enabled = overflow && headerToolbarScrollIndex > 0;
        int viewportX = toolbarX;
        int viewportWidth = toolbarWidth;
        if (overflow) {
            viewportX += arrowWidth + gap;
            viewportWidth = Math.max(60, toolbarWidth - (arrowWidth + gap) * 2);
            layoutHeaderButton(btnHeaderToolbarPrev, toolbarX, toolbarY, arrowWidth);
            layoutHeaderButton(btnHeaderToolbarNext, getEditorPanelX() + getEditorPanelWidth() - 8 - arrowWidth,
                    toolbarY, arrowWidth);
            btnHeaderToolbarPrev.displayString = "<";
            btnHeaderToolbarNext.displayString = ">";
        } else {
            btnHeaderToolbarPrev.visible = false;
            btnHeaderToolbarNext.visible = false;
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
        btnHeaderToolbarNext.enabled = overflow && headerToolbarScrollIndex < headerToolbarMaxScrollIndex;

        int currentX = viewportX;
        boolean hiddenAny = false;
        for (int i = 0; i < buttons.size(); i++) {
            GuiButton button = buttons.get(i);
            if (button == null) {
                continue;
            }
            int width = buttonWidths.get(i);
            if (i < headerToolbarScrollIndex) {
                button.visible = false;
                hiddenAny = true;
                continue;
            }
            if (currentX + width > viewportX + viewportWidth) {
                button.visible = false;
                hiddenAny = true;
                continue;
            }
            layoutHeaderButton(button, currentX, toolbarY, width);
            currentX += width + gap;
        }
        if (!overflow) {
            headerToolbarMaxScrollIndex = 0;
        } else if (!hiddenAny) {
            btnHeaderToolbarNext.enabled = false;
        }
    }

    private void layoutHeaderButton(GuiButton button, int x, int y, int width) {
        if (button == null) {
            return;
        }
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = 18;
        button.visible = true;
    }

    private void rebuildEditorLayoutPreservingState() {
        boolean dirty = this.hasUnsavedChanges;
        String pending = this.pendingSwitchActionType;
        int oldScroll = this.paramScrollOffset;
        captureCurrentEditorDraftParams();
        initGui();
        this.paramScrollOffset = Math.max(0, Math.min(oldScroll, maxParamScroll));
        refreshDynamicParamLayout();
        this.hasUnsavedChanges = dirty;
        this.pendingSwitchActionType = pending;
    }

    private void syncInfoPanelPreferences() {
        savedShowSummaryInfoCard = showSummaryInfoCard;
        savedShowValidationInfoCard = showValidationInfoCard;
        savedShowHelpInfoCard = showHelpInfoCard;
        savedInfoPopupX = infoPopupLastFloatX;
        savedInfoPopupY = infoPopupLastFloatY;
        savedInfoPopupPreferredWidth = infoPopupPreferredWidth;
        savedInfoPopupPreferredHeight = infoPopupPreferredHeight;
        savedInfoPanelDockMode = infoPanelDockMode == null ? "float" : infoPanelDockMode;
        savedInfoDockPreferredWidth = infoDockPreferredWidth;
        savedInfoDockPreferredHeight = infoDockPreferredHeight;
    }

    boolean shouldShowAdvancedOptions() {
        return true;
    }

    boolean shouldShowAdvancedWaitOptions(String actionType) {
        return true;
    }

    boolean shouldShowAdvancedCaptureOptions(String actionType) {
        return true;
    }

    private int getHiddenAdvancedOptionCount() {
        return 0;
    }

    private int getSequenceBuiltinDelayTicksDraftValue() {
        String raw = sequenceBuiltinDelayTicksDraft;
        if (raw == null || raw.trim().isEmpty()) {
            return 5;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return Math.max(0, PathSequenceEventListener.getBuiltinSequenceDelayTicks());
        }
    }

    private void cycleSequenceBuiltinDelayTicks() {
        int current = getSequenceBuiltinDelayTicksDraftValue();
        int next = BUILTIN_DELAY_TICK_PRESETS[0];
        for (int preset : BUILTIN_DELAY_TICK_PRESETS) {
            if (preset > current) {
                next = preset;
                break;
            }
            next = BUILTIN_DELAY_TICK_PRESETS[0];
        }
        sequenceBuiltinDelayTicksDraft = String.valueOf(next);
        refreshSequenceBuiltinDelayTicksToolbarText();
        refreshSequenceBuiltinDelayToolbarText();
    }

    private void updateInfoPanelVisibilityState(boolean wasDocked) {
        refreshInfoCardToggleButtonText();
        boolean dockedNow = hasVisibleInfoCards();
        if (dockedNow != wasDocked) {
            rebuildEditorLayoutPreservingState();
            return;
        }
        if (hasActiveInfoSections()) {
            ensureInfoPopupWindowBounds();
        }
    }

    private void generateParamFields(int x, int y) {
        int fieldWidth = getParamFieldWidth();
        int currentY = y;
        String selectedType = availableActionTypes.get(selectedTypeIndex);

        int currentWidth = mc.displayWidth;
        int currentHeight = mc.displayHeight;

        switch (selectedType.toLowerCase()) {
            case "command":
                addTextField(I18n.format("gui.path.action_editor.label.command"), "command",
                        I18n.format("gui.path.action_editor.help.command"), fieldWidth, x, currentY);
                break;
            case "system_message":
                addTextField(I18n.format("gui.path.action_editor.label.system_message"), "message",
                        I18n.format("gui.path.action_editor.help.system_message"), fieldWidth, x, currentY);
                if (!paramFields.isEmpty()) {
                    systemMessageField = paramFields.get(paramFields.size() - 1);
                }

                systemMessageColorLabelX = x;
                systemMessageColorLabelY = currentY + 36;
                systemMessageFormatLabelX = x;
                systemMessageFormatLabelY = currentY + 88;
                systemMessageColorLabelBaseY = systemMessageColorLabelY;
                systemMessageFormatLabelBaseY = systemMessageFormatLabelY;

                int colorBtnWidth = 20;
                int colorBtnHeight = 16;
                int gap = 2;
                for (int i = 0; i < SYSTEM_MESSAGE_COLOR_CODES.length; i++) {
                    int row = i / 8;
                    int col = i % 8;
                    int bx = x + col * (colorBtnWidth + gap);
                    int by = currentY + 48 + row * (colorBtnHeight + gap);
                    String code = SYSTEM_MESSAGE_COLOR_CODES[i];
                    GuiButton colorBtn = new ThemedButton(BTN_ID_COLOR_BASE + i, bx, by, colorBtnWidth, colorBtnHeight,
                            code + "■");
                    this.buttonList.add(colorBtn);
                    this.messageColorButtons.add(colorBtn);
                    registerScrollableButton(colorBtn, by);
                }

                int formatBtnWidth = 28;
                int formatBtnHeight = 16;
                int formatGap = 2;
                for (int i = 0; i < SYSTEM_MESSAGE_FORMAT_CODES.length; i++) {
                    int bx = x + i * (formatBtnWidth + formatGap);
                    int by = currentY + 100;
                    GuiButton formatBtn = new ThemedButton(BTN_ID_FORMAT_BASE + i, bx, by, formatBtnWidth,
                            formatBtnHeight, SYSTEM_MESSAGE_FORMAT_BUTTON_TEXTS[i]);
                    this.buttonList.add(formatBtn);
                    this.messageColorButtons.add(formatBtn);
                    registerScrollableButton(formatBtn, by);
                }
                break;
            case "disconnect":
                break;
            case "delay":
                addTextField(I18n.format("gui.path.action_editor.label.delay_ticks"), "ticks",
                        I18n.format("gui.path.action_editor.help.delay_ticks"), fieldWidth, x, currentY);
                currentY += 40;
                addToggle(I18n.format("gui.path.action_editor.label.normalize_delay_to_20tps"),
                        "normalizeDelayTo20Tps",
                        I18n.format("gui.path.action_editor.help.normalize_delay_to_20tps"),
                        fieldWidth, x, currentY,
                        !currentParams.has("normalizeDelayTo20Tps")
                                || currentParams.get("normalizeDelayTo20Tps").getAsBoolean(),
                        I18n.format("path.common.on"), I18n.format("path.common.off"));
                break;
            case "key":
                addTextField(I18n.format("gui.path.action_editor.label.key_name"), "key",
                        I18n.format("gui.path.action_editor.help.key_name"), fieldWidth, x, currentY);
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.key_state"), "state",
                        I18n.format("gui.path.action_editor.help.key_state"), fieldWidth, x, currentY,
                        new String[] { "单击", "按下", "抬起" },
                        stateToDisplay(currentParams.has("state") ? currentParams.get("state").getAsString() : "Press"));
                break;
            case "jump":
                addTextField(I18n.format("gui.path.action_editor.label.jump_count"), "count",
                        I18n.format("gui.path.action_editor.help.jump_count"), fieldWidth, x, currentY, "1");
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.jump_interval_ticks"), "intervalTicks",
                        I18n.format("gui.path.action_editor.help.jump_interval_ticks"), fieldWidth, x, currentY,
                        "0");
                break;
            case "click":
                addDropdown(I18n.format("gui.path.action_editor.label.screen_locator_mode"), "locatorMode",
                        I18n.format("gui.path.action_editor.help.screen_locator_mode"), fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.path.action_editor.option.locator.click.coordinate"),
                                I18n.format("gui.path.action_editor.option.locator.click.button_text"),
                                I18n.format("gui.path.action_editor.option.locator.click.slot_text"),
                                I18n.format("gui.path.action_editor.option.locator.click.element_path")
                        },
                        clickLocatorModeToDisplay(currentParams.has("locatorMode")
                                ? currentParams.get("locatorMode").getAsString()
                                : ActionTargetLocator.CLICK_MODE_COORDINATE));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.locator_text"), "locatorText",
                        I18n.format("gui.path.action_editor.help.locator_text"), fieldWidth, x, currentY);
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.locator_match_mode"), "locatorMatchMode",
                        I18n.format("gui.path.action_editor.help.locator_match_mode"), fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.autouseitem.match.contains"),
                                I18n.format("gui.autouseitem.match.exact")
                        },
                        matchModeToDisplay(currentParams.has("locatorMatchMode")
                                ? currentParams.get("locatorMatchMode").getAsString()
                                : ActionTargetLocator.MATCH_MODE_CONTAINS));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.x"), "x",
                        I18n.format("gui.path.action_editor.help.x", currentWidth), fieldWidth, x, currentY);
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.y"), "y",
                        I18n.format("gui.path.action_editor.help.y", currentHeight), fieldWidth, x, currentY);
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.left_click"), "left",
                        I18n.format("gui.path.action_editor.help.left_click"), fieldWidth, x, currentY,
                        new String[] { "左键", "右键" },
                        leftToDisplay(currentParams.has("left") ? currentParams.get("left").getAsString() : "true"));
                break;
            case "setview":
                addTextField(I18n.format("gui.path.action_editor.label.yaw"), "yaw",
                        I18n.format("gui.path.action_editor.help.yaw"), fieldWidth, x, currentY);
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.pitch"), "pitch",
                        I18n.format("gui.path.action_editor.help.pitch"), fieldWidth, x, currentY);
                break;
            case "window_click":
            case "conditional_window_click":
                String windowSlotLocatorMode = getDraftSlotLocatorMode();
                addDropdown(I18n.format("gui.path.action_editor.label.slot_locator_mode"), "locatorMode",
                        I18n.format("gui.path.action_editor.help.slot_locator_mode"), fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.path.action_editor.option.locator.slot.direct"),
                                I18n.format("gui.path.action_editor.option.locator.slot.item_text"),
                                I18n.format("gui.path.action_editor.option.locator.slot.empty"),
                                I18n.format("gui.path.action_editor.option.locator.slot.path")
                        },
                        slotLocatorModeToDisplay(windowSlotLocatorMode));
                currentY += 40;
                if (isDirectSlotLocatorMode(windowSlotLocatorMode)) {
                    addTextField(I18n.format("gui.path.action_editor.label.slot"), "slot",
                            I18n.format("gui.path.action_editor.help.slot"), fieldWidth, x, currentY);
                    currentY += 40;
                    addDropdown(I18n.format("gui.path.action_editor.label.slot_base"), "slotBase",
                            I18n.format("gui.path.action_editor.help.slot_base"), fieldWidth, x, currentY,
                            new String[] { I18n.format("gui.path.action_editor.option.decimal"),
                                    I18n.format("gui.path.action_editor.option.hex") },
                            currentParams.has("slotBase")
                                    ? ("HEX".equalsIgnoreCase(currentParams.get("slotBase").getAsString())
                                            ? I18n.format("gui.path.action_editor.option.hex")
                                            : I18n.format("gui.path.action_editor.option.decimal"))
                                    : I18n.format("gui.path.action_editor.option.decimal"));
                    currentY += 40;
                } else if (!ActionTargetLocator.SLOT_MODE_EMPTY.equalsIgnoreCase(windowSlotLocatorMode)) {
                    addTextField(I18n.format("gui.path.action_editor.label.locator_text"), "locatorText",
                            I18n.format("gui.path.action_editor.help.locator_text"), fieldWidth, x, currentY);
                    currentY += 40;
                    addDropdown(I18n.format("gui.path.action_editor.label.locator_match_mode"), "locatorMatchMode",
                            I18n.format("gui.path.action_editor.help.locator_match_mode"), fieldWidth, x, currentY,
                            new String[] {
                                    I18n.format("gui.autouseitem.match.contains"),
                                    I18n.format("gui.autouseitem.match.exact")
                            },
                            matchModeToDisplay(currentParams.has("locatorMatchMode")
                                    ? currentParams.get("locatorMatchMode").getAsString()
                                    : ActionTargetLocator.MATCH_MODE_CONTAINS));
                    currentY += 40;
                }
                addTextField(I18n.format("gui.path.action_editor.label.window_id_optional"), "windowId",
                        I18n.format("gui.path.action_editor.help.window_id_optional"), fieldWidth, x, currentY);
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.contains_text"), "contains",
                        I18n.format("gui.path.action_editor.help.contains_text"), fieldWidth, x, currentY);
                currentY += 40;
                addToggle(I18n.format("gui.path.action_editor.label.window_click_only_on_slot_change"),
                        "onlyOnSlotChange",
                        I18n.format("gui.path.action_editor.help.window_click_only_on_slot_change"), fieldWidth, x,
                        currentY,
                        currentParams.has("onlyOnSlotChange")
                                && currentParams.get("onlyOnSlotChange").getAsBoolean(),
                        I18n.format("path.common.on"), I18n.format("path.common.off"));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.button"), "button",
                        I18n.format("gui.path.action_editor.help.button"), fieldWidth, x, currentY, "0");
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.click_type"), "clickType",
                        I18n.format("gui.path.action_editor.help.click_type"), fieldWidth, x, currentY,
                        CLICK_TYPE_DISPLAY_OPTIONS,
                        clickTypeToDisplay(
                                currentParams.has("clickType") ? currentParams.get("clickType").getAsString()
                                        : "PICKUP"));
                break;
            case "rightclickblock":
                addDropdown(I18n.format("gui.path.action_editor.label.target_locator_mode"), "locatorMode",
                        I18n.format("gui.path.action_editor.help.target_locator_mode"), fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.path.action_editor.option.locator.world.position"),
                                I18n.format("gui.path.action_editor.option.locator.world.name")
                        },
                        worldLocatorModeToDisplay(currentParams.has("locatorMode")
                                ? currentParams.get("locatorMode").getAsString()
                                : ActionTargetLocator.TARGET_MODE_POSITION));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.locator_text"), "locatorText",
                        I18n.format("gui.path.action_editor.help.world_locator_text"), fieldWidth, x, currentY);
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.locator_match_mode"), "locatorMatchMode",
                        I18n.format("gui.path.action_editor.help.locator_match_mode"), fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.autouseitem.match.contains"),
                                I18n.format("gui.autouseitem.match.exact")
                        },
                        matchModeToDisplay(currentParams.has("locatorMatchMode")
                                ? currentParams.get("locatorMatchMode").getAsString()
                                : ActionTargetLocator.MATCH_MODE_CONTAINS));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.target_pos"), "pos",
                        I18n.format("gui.path.action_editor.help.target_pos"), fieldWidth, x, currentY);
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.range"), "range",
                        I18n.format("gui.path.action_editor.help.range"), fieldWidth, x, currentY, "10");
                currentY += 40;
                btnScanNearbyBlocks = new ThemedButton(BTN_ID_SCAN_NEARBY_BLOCKS, x, currentY, fieldWidth, 20,
                        "§a扫描附近可交互方块");
                this.buttonList.add(btnScanNearbyBlocks);
                registerScrollableButton(btnScanNearbyBlocks, currentY);
                currentY += 40;
                nearbyBlockDropdown = new EnumDropdown(x, currentY, fieldWidth, 20,
                        new String[] { "未找到范围内可交互方块" });
                nearbyBlockDropdownBaseY = currentY;
                currentY += 40;
                btnFillNearestChest = new ThemedButton(BTN_ID_FILL_NEAREST_CHEST, x, currentY, fieldWidth, 20,
                        "§a" + I18n.format("gui.path.action_editor.nearest_chest"));
                this.buttonList.add(btnFillNearestChest);
                registerScrollableButton(btnFillNearestChest, currentY);
                break;
            case "rightclickentity":
                addDropdown(I18n.format("gui.path.action_editor.label.target_locator_mode"), "locatorMode",
                        I18n.format("gui.path.action_editor.help.target_locator_mode"), fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.path.action_editor.option.locator.world.position"),
                                I18n.format("gui.path.action_editor.option.locator.world.name")
                        },
                        worldLocatorModeToDisplay(currentParams.has("locatorMode")
                                ? currentParams.get("locatorMode").getAsString()
                                : ActionTargetLocator.TARGET_MODE_POSITION));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.locator_text"), "locatorText",
                        I18n.format("gui.path.action_editor.help.world_locator_text"), fieldWidth, x, currentY);
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.locator_match_mode"), "locatorMatchMode",
                        I18n.format("gui.path.action_editor.help.locator_match_mode"), fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.autouseitem.match.contains"),
                                I18n.format("gui.autouseitem.match.exact")
                        },
                        matchModeToDisplay(currentParams.has("locatorMatchMode")
                                ? currentParams.get("locatorMatchMode").getAsString()
                                : ActionTargetLocator.MATCH_MODE_CONTAINS));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.target_pos"), "pos",
                        I18n.format("gui.path.action_editor.help.target_pos"), fieldWidth, x, currentY);
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.range"), "range",
                        I18n.format("gui.path.action_editor.help.range"), fieldWidth, x, currentY, "3");
                currentY += 40;
                btnScanNearbyEntities = new ThemedButton(BTN_ID_SCAN_NEARBY_ENTITIES, x, currentY, fieldWidth, 20,
                        "§a扫描附近生物");
                this.buttonList.add(btnScanNearbyEntities);
                registerScrollableButton(btnScanNearbyEntities, currentY);
                currentY += 40;
                nearbyEntityDropdown = new EnumDropdown(x, currentY, fieldWidth, 20,
                        new String[] { "未找到范围内生物" });
                nearbyEntityDropdownBaseY = currentY;
                break;
            case "autochestclick":
                String chestSlotLocatorMode = getDraftSlotLocatorMode();
                addDropdown(I18n.format("gui.path.action_editor.label.slot_locator_mode"), "locatorMode",
                        I18n.format("gui.path.action_editor.help.slot_locator_mode"), fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.path.action_editor.option.locator.slot.direct"),
                                I18n.format("gui.path.action_editor.option.locator.slot.item_text"),
                                I18n.format("gui.path.action_editor.option.locator.slot.empty"),
                                I18n.format("gui.path.action_editor.option.locator.slot.path")
                        },
                        slotLocatorModeToDisplay(chestSlotLocatorMode));
                currentY += 40;
                if (isDirectSlotLocatorMode(chestSlotLocatorMode)) {
                    addTextField(I18n.format("gui.path.action_editor.label.chest_slot"), "slot",
                            I18n.format("gui.path.action_editor.help.chest_slot"), fieldWidth, x, currentY);
                    currentY += 40;
                } else if (!ActionTargetLocator.SLOT_MODE_EMPTY.equalsIgnoreCase(chestSlotLocatorMode)) {
                    addTextField(I18n.format("gui.path.action_editor.label.locator_text"), "locatorText",
                            I18n.format("gui.path.action_editor.help.locator_text"), fieldWidth, x, currentY);
                    currentY += 40;
                    addDropdown(I18n.format("gui.path.action_editor.label.locator_match_mode"), "locatorMatchMode",
                            I18n.format("gui.path.action_editor.help.locator_match_mode"), fieldWidth, x, currentY,
                            new String[] {
                                    I18n.format("gui.autouseitem.match.contains"),
                                    I18n.format("gui.autouseitem.match.exact")
                            },
                            matchModeToDisplay(currentParams.has("locatorMatchMode")
                                    ? currentParams.get("locatorMatchMode").getAsString()
                                    : ActionTargetLocator.MATCH_MODE_CONTAINS));
                    currentY += 40;
                }
                addTextField(I18n.format("gui.path.action_editor.label.delay_ticks"), "delayTicks",
                        I18n.format("gui.path.action_editor.help.chest_click_delay_ticks"), fieldWidth, x, currentY,
                        "1");
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.click_type"), "clickType",
                        I18n.format("gui.path.action_editor.help.click_type"), fieldWidth, x, currentY,
                        CLICK_TYPE_DISPLAY_OPTIONS,
                        clickTypeToDisplay(
                                currentParams.has("clickType") ? currentParams.get("clickType").getAsString()
                                        : "PICKUP"));
                break;
            case ACTION_MOVE_INVENTORY_ITEMS_TO_CHEST_SLOTS:
                initializeMoveChestSelectionState();
                addSectionTitle("§b§l━━━ 基础设置 ━━━", x, currentY);
                currentY += 25;
                addTextField(I18n.format("gui.path.action_editor.label.delay_ticks"), "delayTicks",
                        I18n.format("gui.path.action_editor.help.move_chest_delay_ticks"), fieldWidth, x, currentY,
                        "2");
                currentY += 40;
                addToggle(I18n.format("gui.path.action_editor.label.normalize_delay_to_20tps"),
                        "normalizeDelayTo20Tps",
                        I18n.format("gui.path.action_editor.help.normalize_delay_to_20tps"),
                        fieldWidth, x, currentY,
                        !currentParams.has("normalizeDelayTo20Tps")
                                || currentParams.get("normalizeDelayTo20Tps").getAsBoolean(),
                        I18n.format("path.common.on"), I18n.format("path.common.off"));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.chest_rows"), "chestRows",
                        I18n.format("gui.path.action_editor.help.chest_rows"), fieldWidth, x, currentY, "6");
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.chest_cols"), "chestCols",
                        I18n.format("gui.path.action_editor.help.chest_cols"), fieldWidth, x, currentY, "9");
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.inventory_rows"), "inventoryRows",
                        I18n.format("gui.path.action_editor.help.inventory_rows"), fieldWidth, x, currentY, "4");
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.inventory_cols"), "inventoryCols",
                        I18n.format("gui.path.action_editor.help.inventory_cols"), fieldWidth, x, currentY, "9");
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.move_chest_direction"),
                        "moveDirection",
                        I18n.format("gui.path.action_editor.help.move_chest_direction"),
                        fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.path.action_editor.option.move_chest_direction.inventory_to_chest"),
                                I18n.format("gui.path.action_editor.option.move_chest_direction.chest_to_inventory")
                        },
                        moveChestDirectionToDisplay(currentParams.has("moveDirection")
                                ? currentParams.get("moveDirection").getAsString()
                                : ItemFilterHandler.MOVE_DIRECTION_INVENTORY_TO_CHEST));
                currentY += 40;
                addSectionTitle("§b§l━━━ 物品过滤 ━━━", x, currentY);
                currentY += 25;
                moveChestItemInputBaseY = currentY + 18;
                moveChestItemNameInputField = new GuiTextField(949, this.fontRenderer, x, currentY + 18, fieldWidth, 20);
                moveChestItemNameInputField.setMaxStringLength(256);
                moveChestItemNameInputField.setEnableBackgroundDrawing(false);
                moveChestItemNameInputField.setText(moveChestDraftItemName);
                currentY += 40;
                addDropdown(I18n.format("gui.path.action_editor.label.required_nbt_tags_mode"),
                        "requiredNbtTagsMode",
                        I18n.format("gui.path.action_editor.help.required_nbt_tags_mode"),
                        fieldWidth, x, currentY,
                        new String[] {
                                I18n.format("gui.path.action_editor.option.nbt_tag_mode.contains"),
                                I18n.format("gui.path.action_editor.option.nbt_tag_mode.not_contains")
                        },
                        moveChestNbtModeToDisplay(currentParams.has("requiredNbtTagsMode")
                                ? currentParams.get("requiredNbtTagsMode").getAsString()
                                : "CONTAINS"));
                currentY += 40;
                moveChestTagInputBaseY = currentY + 18;
                moveChestNbtTagInputField = new GuiTextField(950, this.fontRenderer, x, currentY + 18, fieldWidth, 20);
                moveChestNbtTagInputField.setMaxStringLength(256);
                moveChestNbtTagInputField.setEnableBackgroundDrawing(false);
                moveChestNbtTagInputField.setText(moveChestDraftNbtText);
                currentY += 40;
                moveChestRuleButtonBaseY = currentY;
                btnAddMoveChestNbtTag = new ThemedButton(BTN_ID_ADD_MOVE_CHEST_NBT_TAG, x, currentY, fieldWidth, 20,
                        "添加规则");
                this.buttonList.add(btnAddMoveChestNbtTag);
                registerScrollableButton(btnAddMoveChestNbtTag, moveChestRuleButtonBaseY);
                currentY += 28;
                addSectionTitle("§b§l━━━ 槽位选择 ━━━", x, currentY);
                break;
            case "blocknextgui":
                addTextField(I18n.format("gui.path.action_editor.label.block_count"), "count",
                        I18n.format("gui.path.action_editor.help.block_count"), fieldWidth, x, currentY, "1");
                currentY += 40;
                addToggle(I18n.format("gui.path.action_editor.label.block_current_gui"), "blockCurrentGui",
                        I18n.format("gui.path.action_editor.help.block_current_gui"), fieldWidth, x, currentY,
                        currentParams.has("blockCurrentGui")
                                && currentParams.get("blockCurrentGui").getAsBoolean(),
                        I18n.format("path.common.yes"), I18n.format("path.common.no"));
                break;
            case "close_container_window":
                break;
            case "hud_text_check":
                addTextField(I18n.format("gui.path.action_editor.label.contains_text"), "contains",
                        I18n.format("gui.path.action_editor.help.contains_text"), fieldWidth, x, currentY);
                currentY += 40;
                addToggle(I18n.format("gui.path.action_editor.label.match_block"), "matchBlock",
                        I18n.format("gui.path.action_editor.help.match_block"), fieldWidth, x, currentY,
                        currentParams.has("matchBlock")
                                && currentParams.get("matchBlock").getAsBoolean(),
                        I18n.format("path.common.on"), I18n.format("path.common.off"));
                currentY += 40;
                addTextField(I18n.format("gui.path.action_editor.label.separator"), "separator",
                        I18n.format("gui.path.action_editor.help.separator"), fieldWidth, x, currentY, " | ");
                break;
            case "condition_inventory_item":
            case "wait_until_inventory_item":
                ActionConditionWaitSections.buildInventoryConditionSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "condition_gui_title":
            case "wait_until_gui_title":
                ActionConditionWaitSections.buildGuiTitleSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "condition_player_in_area":
            case "wait_until_player_in_area":
                ActionConditionWaitSections.buildPlayerAreaSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "condition_entity_nearby":
            case "wait_until_entity_nearby":
                ActionConditionWaitSections.buildEntityNearbySection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "wait_until_hud_text":
                ActionConditionWaitSections.buildWaitHudTextSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "condition_expression":
            case "wait_until_expression":
                ActionConditionWaitSections.buildExpressionSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "wait_combined":
                ActionConditionWaitSections.buildWaitCombinedSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "wait_until_captured_id":
                ActionConditionWaitSections.buildWaitCapturedIdSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "wait_until_packet_text":
                ActionConditionWaitSections.buildWaitPacketTextSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "wait_until_screen_region":
                ActionConditionWaitSections.buildWaitScreenRegionSection(this, selectedType, x, currentY, fieldWidth);
                break;
            case "set_var":
                ActionUtilitySections.buildSetVarSection(this, x, currentY, fieldWidth);
                break;
            case "goto_action":
                ActionUtilitySections.buildGotoActionSection(this, x, currentY, fieldWidth);
                break;
            case "skip_actions":
                ActionUtilitySections.buildSkipActionsSection(this, x, currentY, fieldWidth);
                break;
            case "skip_steps":
                ActionUtilitySections.buildSkipStepsSection(this, x, currentY, fieldWidth);
                break;
            case "repeat_actions":
                ActionUtilitySections.buildRepeatActionsSection(this, x, currentY, fieldWidth);
                break;
            case "autoeat":
                ActionUtilitySections.buildAutoEatSection(this, x, currentY, fieldWidth);
                break;
            case "autoequip":
                ActionUtilitySections.buildAutoEquipSection(this, x, currentY, fieldWidth);
                break;
            case "autopickup":
                ActionUtilitySections.buildAutoPickupSection(this, x, currentY, fieldWidth);
                break;
            case "toggle_autoeat":
            case "toggle_autofishing":
            case "toggle_kill_aura":
            case "toggle_fly":
                ActionUtilitySections.buildSimpleToggleSection(this, x, currentY, fieldWidth);
                break;
            case "toggle_other_feature":
                ActionUtilitySections.buildOtherFeatureToggleSection(this, x, currentY, fieldWidth);
                break;
            case "takeallitems":
            case "take_all_items_safe":
                ActionUtilitySections.buildTakeAllItemsSection(this, x, currentY, fieldWidth);
                break;
            case "runlastsequence":
                break;
            case "hunt":
                ActionParameterSections.buildHuntSection(this, x, currentY, fieldWidth);
                break;
            case "follow_entity":
                ActionParameterSections.buildFollowEntitySection(this, x, currentY, fieldWidth);
                break;
            case "use_skill":
                ActionParameterSections.buildUseSkillSection(this, x, currentY, fieldWidth);
                break;
            case "use_hotbar_item":
                ActionParameterSections.buildUseHotbarItemSection(this, x, currentY, fieldWidth);
                break;
            case "move_inventory_item_to_hotbar":
                ActionParameterSections.buildMoveInventoryItemToHotbarSection(this, x, currentY, fieldWidth);
                break;
            case "silentuse":
                ActionParameterSections.buildSilentUseSection(this, x, currentY, fieldWidth);
                break;
            case "switch_hotbar_slot":
                ActionParameterSections.buildSwitchHotbarSlotSection(this, x, currentY, fieldWidth);
                break;
            case "use_held_item":
                ActionParameterSections.buildUseHeldItemSection(this, x, currentY, fieldWidth);
                break;
            case "send_packet":
                ActionParameterSections.buildSendPacketSection(this, x, currentY, fieldWidth);
                break;
            case "run_sequence":
                ActionParameterSections.buildRunSequenceSection(this, x, currentY, fieldWidth);
                break;
            case "stop_current_sequence":
                ActionParameterSections.buildStopCurrentSequenceSection(this, x, currentY, fieldWidth);
                break;
            // end send_packet extras
        }
    }

    private void initializeActionLibraryTreeIfNeeded() {
        actionLibraryRoots.clear();
        actionLibraryRoots.addAll(ActionLibraryTreeFactory.buildRoots(savedFavoriteActionTypes, savedRecentActionTypes,
                this::shouldHideInNewActionPanel, this::resolveActionDisplayName));
        for (ActionLibraryNode root : actionLibraryRoots) {
            expandActionLibraryGroups(root);
        }
    }

    private String resolveActionDisplayName(String actionType) {
        String key = ACTION_DISPLAY_KEYS.get(actionType);
        return key == null ? actionType : I18n.format(key);
    }

    private void expandActionLibraryGroups(ActionLibraryNode node) {
        if (node == null || !node.isGroup()) {
            return;
        }
        expandedActionLibraryGroupIds.add(node.id);
        for (ActionLibraryNode child : node.children) {
            expandActionLibraryGroups(child);
        }
    }

    private void rebuildActionLibraryAndScrollBounds() {
        rebuildVisibleActionLibraryRows();
        int visibleRows = Math.max(1, getActionListContentHeight() / ACTION_LIBRARY_ROW_HEIGHT);
        maxActionListScroll = Math.max(0, visibleActionLibraryRows.size() - visibleRows);
        actionListScrollOffset = Math.max(0, Math.min(actionListScrollOffset, maxActionListScroll));
    }

    private void rebuildVisibleActionLibraryRows() {
        visibleActionLibraryRows.clear();
        visibleActionLibraryRows.addAll(ActionLibraryViewSupport.buildVisibleRows(actionLibraryRoots,
                expandedActionLibraryGroupIds, getActionSearchText()));
    }

    private boolean isFavoriteActionType(String actionType) {
        String normalized = safe(actionType).trim().toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && savedFavoriteActionTypes.contains(normalized);
    }

    private void recordRecentActionType(String actionType) {
        String normalized = safe(actionType).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }
        savedRecentActionTypes.remove(normalized);
        savedRecentActionTypes.add(0, normalized);
        while (savedRecentActionTypes.size() > MAX_RECENT_ACTIONS) {
            savedRecentActionTypes.remove(savedRecentActionTypes.size() - 1);
        }
        savePreferences();
    }

    private void toggleFavoriteActionType(String actionType) {
        String normalized = safe(actionType).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }
        if (!savedFavoriteActionTypes.add(normalized)) {
            savedFavoriteActionTypes.remove(normalized);
        }
        savePreferences();
    }

    private void refreshEditorAfterActionLibraryPreferenceChange() {
        captureCurrentEditorDraftParams();
        int oldParamScroll = this.paramScrollOffset;
        int oldActionScroll = this.actionListScrollOffset;
        boolean dirty = this.hasUnsavedChanges;
        String pending = this.pendingSwitchActionType;
        initGui();
        this.paramScrollOffset = Math.max(0, Math.min(oldParamScroll, maxParamScroll));
        this.actionListScrollOffset = Math.max(0, Math.min(oldActionScroll, maxActionListScroll));
        refreshDynamicParamLayout();
        rebuildActionLibraryAndScrollBounds();
        this.hasUnsavedChanges = dirty;
        this.pendingSwitchActionType = pending;
    }

    private ActionLibraryVisibleRow hitTestActionLibraryRow(int mouseX, int mouseY) {
        int listX = getActionListX();
        int listTop = getActionListTop();
        int listWidth = actionListWidth;
        int listContentHeight = getActionListContentHeight();

        if (mouseX < listX || mouseX > listX + listWidth - 8 || mouseY < listTop
                || mouseY > listTop + listContentHeight) {
            return null;
        }

        int clickedRowIndex = (mouseY - listTop) / ACTION_LIBRARY_ROW_HEIGHT;
        int actualIndex = clickedRowIndex + actionListScrollOffset;
        if (actualIndex < 0 || actualIndex >= visibleActionLibraryRows.size()) {
            return null;
        }
        return visibleActionLibraryRows.get(actualIndex);
    }

    private void onActionLibraryRowLeftClick(ActionLibraryVisibleRow row) {
        if (row == null || row.node == null) {
            return;
        }
        if (row.node.isGroup()) {
            toggleActionLibraryGroup(row.node.id);
            return;
        }

        onActionLibraryActionSelected(row.node.actionType);
    }

    private void onActionLibraryActionSelected(String actionType) {
        if (actionType == null || actionType.trim().isEmpty()) {
            return;
        }
        int actualIndex = availableActionTypes.indexOf(actionType);
        if (actualIndex >= 0 && actualIndex != selectedTypeIndex) {
            if (hasUnsavedChanges) {
                if (actionType.equalsIgnoreCase(pendingSwitchActionType)) {
                    selectedTypeIndex = actualIndex;
                    currentParams = new JsonObject();
                    hasUnsavedChanges = false;
                    pendingSwitchActionType = null;
                    recordRecentActionType(actionType);
                    initGui();
                    return;
                }
                pendingSwitchActionType = actionType;
                return;
            }
            pendingSwitchActionType = null;
            selectedTypeIndex = actualIndex;
            currentParams = new JsonObject();
            recordRecentActionType(actionType);
            initGui();
        }
    }

    private void toggleActionLibraryGroup(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        if (expandedActionLibraryGroupIds.contains(groupId)) {
            expandedActionLibraryGroupIds.remove(groupId);
        } else {
            expandedActionLibraryGroupIds.add(groupId);
        }
        rebuildActionLibraryAndScrollBounds();
    }

    private boolean isActionLibraryGroupExpanded(String groupId) {
        return groupId != null && expandedActionLibraryGroupIds.contains(groupId);
    }

    String getSelectedActionType() {
        if (selectedTypeIndex < 0 || selectedTypeIndex >= availableActionTypes.size()) {
            selectedTypeIndex = 0;
        }
        return availableActionTypes.get(selectedTypeIndex);
    }

    private String getSelectedActionPreviewText() {
        try {
            return buildEditorActionData().getDescription();
        } catch (Exception e) {
            String selectedType = getSelectedActionType();
            String displayKey = ACTION_DISPLAY_KEYS.get(selectedType);
            return displayKey == null ? selectedType : I18n.format(displayKey);
        }
    }

    private JsonObject cloneJsonObject(JsonObject source) {
        return source == null ? new JsonObject() : new JsonParser().parse(source.toString()).getAsJsonObject();
    }

    private ActionData buildEditorActionData() {
        JsonObject newParams = cloneJsonObject(currentParams);
        String selectedType = getSelectedActionType();
        String actionTypeToSave = "conditional_window_click".equalsIgnoreCase(selectedType)
                ? "window_click"
                : selectedType;

        if ("click".equalsIgnoreCase(selectedType)) {
            newParams.addProperty("originalWidth", mc.displayWidth);
            newParams.addProperty("originalHeight", mc.displayHeight);
        } else if ("use_skill".equalsIgnoreCase(selectedType)) {
            newParams.addProperty("skill", this.selectedSkill);
        }

        for (int i = 0; i < paramFields.size(); i++) {
            String key = paramFieldKeys.get(i);
            if (key == null || key.startsWith("__ui_") || key.startsWith("_section_title_")) {
                continue;
            }
            newParams.remove(key);
            String value = paramFields.get(i).getText();
            if (value == null || value.trim().isEmpty()) {
                continue;
            }

            if ("message".equals(key) || "locatorText".equals(key) || "channel".equals(key) || "hex".equals(key)) {
                newParams.addProperty(key, value);
                continue;
            }

            try {
                if ("packetId".equals(key)) {
                    newParams.addProperty(key, Integer.decode(value));
                } else {
                    newParams.addProperty(key, Long.parseLong(value));
                }
            } catch (NumberFormatException e1) {
                try {
                    newParams.addProperty(key, Double.parseDouble(value));
                } catch (NumberFormatException e2) {
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        newParams.addProperty(key, Boolean.parseBoolean(value));
                    } else if (value.startsWith("[") && value.endsWith("]")) {
                        JsonArray array = new JsonArray();
                        String[] parts = value.replace("[", "").replace("]", "").split(",");
                        for (String part : parts) {
                            try {
                                array.add(Double.parseDouble(part.trim()));
                            } catch (NumberFormatException e) {
                                zszlScriptMod.LOGGER.error("Failed to parse coordinate array '{}' part '{}'", value,
                                        part);
                                array.add(0.0);
                            }
                        }
                        newParams.add(key, array);
                    } else {
                        newParams.addProperty(key, value);
                    }
                }
            }
        }

        for (int i = 0; i < toggleButtons.size(); i++) {
            String key = toggleKeys.get(i);
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            newParams.remove(key);
            newParams.addProperty(key, toggleButtons.get(i).getValue());
        }

        for (int i = 0; i < paramDropdowns.size(); i++) {
            String key = paramDropdownKeys.get(i);
            if (key == null || key.startsWith("__ui_")) {
                continue;
            }
            newParams.remove(key);
            String value = paramDropdowns.get(i).getValue();
            if ("state".equals(key)) {
                newParams.addProperty(key, displayToState(value));
            } else if ("left".equals(key)) {
                newParams.addProperty(key, "左键".equals(value));
            } else if ("direction".equals(key)) {
                newParams.addProperty(key, displayToDirection(value));
            } else if ("autoAttack".equals(key)) {
                newParams.addProperty(key, displayYesNoToBool(value));
            } else if ("backgroundExecution".equals(key)) {
                newParams.addProperty(key, displayYesNoToBool(value));
            } else if ("onlyOnSlotChange".equals(key)) {
                newParams.addProperty(key, displayOnOffToBool(value));
            } else if ("slotBase".equals(key)) {
                boolean isHex = I18n.format("gui.path.action_editor.option.hex").equals(value);
                newParams.addProperty(key, isHex ? "HEX" : "DEC");
            } else if ("clickType".equals(key)) {
                newParams.addProperty(key, displayToClickType(value));
            } else if ("enabled".equals(key)
                    || "autoMoveFoodEnabled".equals(key)
                    || "eatWithLookDown".equals(key)
                    || "smartActivation".equals(key)
                    || "matchBlock".equals(key)) {
                newParams.addProperty(key, displayOnOffToBool(value));
            } else if ("matchMode".equals(key)) {
                newParams.addProperty(key, displayToMatchMode(value));
            } else if ("useMode".equals(key)) {
                newParams.addProperty(key, displayToUseMode(value));
            } else if ("valueType".equals(key)) {
                newParams.addProperty(key, displayToValueType(value));
            } else if ("waitMode".equals(key)) {
                newParams.addProperty(key, displayToCapturedIdWaitMode(value));
            } else if ("combinedMode".equals(key)) {
                newParams.addProperty(key, displayToWaitCombinedMode(value));
            } else if ("executeMode".equals(key)) {
                newParams.addProperty(key, displayToRunSequenceExecuteMode(value));
            } else if ("targetScope".equals(key)) {
                newParams.addProperty(key, displayToStopCurrentSequenceScope(value));
            } else if ("moveDirection".equals(key)) {
                newParams.addProperty(key, displayToMoveChestDirection(value));
            } else if ("requiredNbtTagsMode".equals(key)) {
                newParams.addProperty(key, displayToMoveChestNbtMode(value));
            } else if ("slotArea".equals(key)) {
                newParams.addProperty(key, displayToCaptureSlotArea(value));
            } else if ("lookupMode".equals(key)) {
                newParams.addProperty(key, displayToPacketFieldLookupMode(value));
            } else if ("visionCompareMode".equals(key)) {
                newParams.addProperty(key, displayToVisionCompareMode(value));
            } else if ("locatorMode".equals(key)) {
                newParams.addProperty(key, displayToLocatorMode(value));
            } else if ("locatorMatchMode".equals(key)) {
                newParams.addProperty(key, displayToMatchMode(value));
            } else if ("elementType".equals(key)) {
                newParams.addProperty(key, displayToGuiElementType(value));
            } else if ("guiElementLocatorMode".equals(key)) {
                newParams.addProperty(key, displayToGuiElementLocatorMode(value));
            } else if ("entityType".equals(key)) {
                newParams.addProperty(key, displayToEntityType(value));
            } else if ("huntMode".equals(key)) {
                newParams.addProperty(key, displayToHuntMode(value));
            } else if ("attackMode".equals(key) && "hunt".equalsIgnoreCase(selectedType)) {
                newParams.addProperty(key, displayToHuntAttackMode(value));
            } else {
                newParams.addProperty(key, value);
            }
        }

        applyHuntActionSaveParams(selectedType, newParams);

        ActionVariableBindingSupport.applyResolvedBindingsToParams(this, newParams);

        if ("wait_until_captured_id".equalsIgnoreCase(selectedType)) {
            if (selectedCapturedIdName != null && !selectedCapturedIdName.trim().isEmpty()) {
                newParams.addProperty("capturedId", selectedCapturedIdName.trim());
            } else {
                newParams.remove("capturedId");
            }
        }

        if ("condition_expression".equalsIgnoreCase(selectedType)
                || "wait_until_expression".equalsIgnoreCase(selectedType)) {
            BooleanExpressionEditorSupport.writeExpressionsToParams(newParams, getBooleanExpressionList());
        }

        if ("run_sequence".equalsIgnoreCase(selectedType)) {
            if (selectedRunSequenceName != null && !selectedRunSequenceName.trim().isEmpty()) {
                newParams.addProperty("sequenceName", selectedRunSequenceName.trim());
            } else {
                newParams.remove("sequenceName");
            }
        }

        if (isConditionInventoryActionType(selectedType)) {
            pruneConditionInventorySelections();
            newParams.addProperty("inventoryRows", getConditionInventoryRows());
            newParams.addProperty("inventoryCols", getConditionInventoryCols());
            JsonArray requiredTags = new JsonArray();
            for (String tag : conditionInventoryRequiredNbtTags) {
                if (tag != null && !tag.trim().isEmpty()) {
                    requiredTags.add(tag.trim());
                }
            }
            newParams.add("requiredNbtTags", requiredTags);
            JsonArray inventorySlots = new JsonArray();
            for (Integer slotIndex : new TreeSet<>(conditionInventorySelectedSlots)) {
                inventorySlots.add(slotIndex);
            }
            newParams.add("inventorySlots", inventorySlots);
        }

        if (ACTION_MOVE_INVENTORY_ITEMS_TO_CHEST_SLOTS.equalsIgnoreCase(selectedType)) {
            pruneMoveChestSelections();
            newParams.addProperty("chestRows", getMoveChestChestRows());
            newParams.addProperty("chestCols", getMoveChestChestCols());
            newParams.addProperty("inventoryRows", getMoveChestInventoryRows());
            newParams.addProperty("inventoryCols", getMoveChestInventoryCols());

            JsonArray chestSlots = new JsonArray();
            for (Integer slotIndex : new TreeSet<>(moveChestSelectedChestSlots)) {
                chestSlots.add(slotIndex);
            }
            newParams.add("chestSlots", chestSlots);

            JsonArray inventorySlots = new JsonArray();
            for (Integer slotIndex : new TreeSet<>(moveChestSelectedInventorySlots)) {
                inventorySlots.add(slotIndex);
            }
            newParams.add("inventorySlots", inventorySlots);
            String moveChestItemName = moveChestItemNameInputField == null ? moveChestDraftItemName
                    : moveChestItemNameInputField.getText();
            moveChestItemName = moveChestItemName == null ? "" : moveChestItemName.trim();
            String moveChestNbtText = moveChestNbtTagInputField == null ? moveChestDraftNbtText
                    : moveChestNbtTagInputField.getText();
            moveChestNbtText = moveChestNbtText == null ? "" : moveChestNbtText.trim();
            MoveChestFilterRuleDraft pendingRule = new MoveChestFilterRuleDraft(moveChestItemName, moveChestNbtText);
            JsonArray moveChestRules = new JsonArray();
            for (MoveChestFilterRuleDraft rule : moveChestFilterRules) {
                if (rule == null || rule.isEmpty()) {
                    continue;
                }
                JsonObject ruleObject = new JsonObject();
                if (!rule.itemName.isEmpty()) {
                    ruleObject.addProperty("itemName", rule.itemName);
                }
                if (!rule.nbtText.isEmpty()) {
                    ruleObject.addProperty("requiredNbtTagsText", rule.nbtText);
                }
                moveChestRules.add(ruleObject);
            }
            if (!pendingRule.isEmpty()) {
                JsonObject ruleObject = new JsonObject();
                if (!pendingRule.itemName.isEmpty()) {
                    ruleObject.addProperty("itemName", pendingRule.itemName);
                }
                if (!pendingRule.nbtText.isEmpty()) {
                    ruleObject.addProperty("requiredNbtTagsText", pendingRule.nbtText);
                }
                moveChestRules.add(ruleObject);
            }
            newParams.remove("itemName");
            newParams.remove("requiredNbtTags");
            if (moveChestRules.size() > 0) {
                newParams.add("moveChestRules", moveChestRules);
            } else {
                newParams.remove("moveChestRules");
                newParams.remove("requiredNbtTagsText");
            }
        }

        if ("toggle_other_feature".equalsIgnoreCase(actionTypeToSave)) {
            String featureId = selectedOtherFeatureId == null ? "" : selectedOtherFeatureId.trim();
            if (!featureId.isEmpty()) {
                newParams.addProperty("featureId", featureId);
                String featureName = resolveOtherFeatureDisplayName(featureId,
                        currentParams.has("featureName") ? currentParams.get("featureName").getAsString() : "");
                if (!featureName.isEmpty()) {
                    newParams.addProperty("featureName", featureName);
                }
            } else {
                newParams.remove("featureId");
                newParams.remove("featureName");
            }
        }

        if ("window_click".equalsIgnoreCase(actionTypeToSave)
                || "run_sequence".equalsIgnoreCase(actionTypeToSave)) {
            String existingUuid = currentParams.has("uuid") ? currentParams.get("uuid").getAsString() : "";
            newParams.addProperty("uuid",
                    existingUuid != null && !existingUuid.trim().isEmpty()
                            ? existingUuid.trim()
                            : java.util.UUID.randomUUID().toString());
        }

        return new ActionData(actionTypeToSave, newParams);
    }

    private void refreshLiveEditorFeedback() {
        ActionData draft = null;
        try {
            draft = buildEditorActionData();
            liveActionSummary = draft == null ? "" : draft.getDescription();
            liveValidationIssues = draft == null
                    ? Collections.emptyList()
                    : PathConfigValidator.validateActionDraft(draft, currentSequenceName, PathSequenceManager.getAllSequences());
        } catch (Exception e) {
            liveActionSummary = getSelectedActionType();
            liveValidationIssues = Collections.emptyList();
        }

        liveValidationErrorCount = 0;
        liveValidationWarningCount = 0;
        for (PathConfigValidator.Issue issue : liveValidationIssues) {
            if (issue == null) {
                continue;
            }
            if (issue.getSeverity() == PathConfigValidator.Severity.ERROR) {
                liveValidationErrorCount++;
            } else {
                liveValidationWarningCount++;
            }
        }
        liveActionEffectHint = ActionEditorFeedbackSupport.buildActionEffectHint(draft);
        liveActionRiskHint = ActionEditorFeedbackSupport.buildActionRiskHint(draft, liveValidationErrorCount);
    }

    private int getInfoPopupMinWidth() {
        return 220;
    }

    private int getInfoPopupMinHeight() {
        return 156;
    }

    private void ensureInfoPopupWindowBounds() {
        int headerHeight = 22;
        int minWidth = getInfoPopupMinWidth();
        int minHeight = getInfoPopupMinHeight();
        if (isInfoPanelDockedMiddle() || isInfoPanelDockedRight()) {
            infoPopupWidth = getInfoCardPaneWidth();
            infoPopupHeight = getInfoCardPaneHeight();
            infoPopupX = getInfoCardPaneX();
            infoPopupY = getInfoCardPaneY();
            int dividerCenterX = isInfoPanelDockedMiddle()
                    ? infoPopupX + infoPopupWidth + INFO_CARD_PANE_GAP / 2
                    : infoPopupX - INFO_CARD_PANE_GAP / 2;
            infoDockDividerBounds = new Rectangle(
                    dividerCenterX - INFO_DOCK_DIVIDER_HIT_THICKNESS / 2,
                    infoPopupY + 4, INFO_DOCK_DIVIDER_HIT_THICKNESS, Math.max(24, infoPopupHeight - 8));
        } else if (isInfoPanelDockedBottom()) {
            infoPopupWidth = getInfoCardPaneWidth();
            infoPopupHeight = getInfoCardPaneHeight();
            infoPopupX = getInfoCardPaneX();
            infoPopupY = getInfoCardPaneY();
            int dividerCenterY = infoPopupY - INFO_CARD_PANE_GAP / 2;
            infoDockDividerBounds = new Rectangle(infoPopupX + 4,
                    dividerCenterY - INFO_DOCK_DIVIDER_HIT_THICKNESS / 2,
                    Math.max(24, infoPopupWidth - 8), INFO_DOCK_DIVIDER_HIT_THICKNESS);
        } else {
            int maxWidth = Math.max(minWidth, getEditorPanelWidth() - 20);
            int maxHeight = Math.max(minHeight, getEditorPanelHeight() - 28);
            infoPopupWidth = MathHelper.clamp(infoPopupPreferredWidth, minWidth, maxWidth);
            infoPopupHeight = MathHelper.clamp(infoPopupPreferredHeight, minHeight, maxHeight);

            int minX = getEditorPanelX() + 8;
            int minY = getEditorPanelY() + 24;
            int maxX = getEditorPanelX() + getEditorPanelWidth() - infoPopupWidth - 8;
            int maxY = getEditorPanelY() + getEditorPanelHeight() - infoPopupHeight - 8;
            if (infoPopupLastFloatX == Integer.MIN_VALUE || infoPopupLastFloatY == Integer.MIN_VALUE) {
                infoPopupLastFloatX = Math.max(minX, getRightPaneX() + getRightPaneWidth() - infoPopupWidth - 12);
                infoPopupLastFloatY = Math.max(minY, getRightPaneY() + 26);
            }
            infoPopupX = MathHelper.clamp(infoPopupLastFloatX, minX, Math.max(minX, maxX));
            infoPopupY = MathHelper.clamp(infoPopupLastFloatY, minY, Math.max(minY, maxY));
            infoPopupLastFloatX = infoPopupX;
            infoPopupLastFloatY = infoPopupY;
            infoDockDividerBounds = null;
        }

        infoPopupBounds = new Rectangle(infoPopupX, infoPopupY, infoPopupWidth, infoPopupHeight);
        infoPopupHeaderBounds = new Rectangle(infoPopupX + 1, infoPopupY + 1, infoPopupWidth - 2, headerHeight);
        int controlSize = Math.max(12, headerHeight - 6);
        infoPopupCloseBounds = new Rectangle(infoPopupX + infoPopupWidth - controlSize - 4, infoPopupY + 3,
                controlSize, controlSize);
        int dockButtonSize = controlSize;
        int dockButtonGap = 2;
        int dockRight = infoPopupCloseBounds.x - dockButtonGap;
        infoPopupDockRightBounds = new Rectangle(dockRight - dockButtonSize, infoPopupY + 3, dockButtonSize, dockButtonSize);
        dockRight = infoPopupDockRightBounds.x - dockButtonGap;
        infoPopupDockBottomBounds = new Rectangle(dockRight - dockButtonSize, infoPopupY + 3, dockButtonSize, dockButtonSize);
        dockRight = infoPopupDockBottomBounds.x - dockButtonGap;
        infoPopupDockMiddleBounds = new Rectangle(dockRight - dockButtonSize, infoPopupY + 3, dockButtonSize, dockButtonSize);
        dockRight = infoPopupDockMiddleBounds.x - dockButtonGap;
        infoPopupDockFloatBounds = new Rectangle(dockRight - dockButtonSize, infoPopupY + 3, dockButtonSize, dockButtonSize);
        infoPopupContentBounds = new Rectangle(infoPopupX + 8, infoPopupHeaderBounds.y + infoPopupHeaderBounds.height + 6,
                Math.max(96, infoPopupWidth - 24), Math.max(40, infoPopupHeight - headerHeight - 14));
        infoPopupResizeRightBounds = new Rectangle(infoPopupX + infoPopupWidth - 6, infoPopupY + headerHeight, 6,
                Math.max(20, infoPopupHeight - headerHeight - 12));
        infoPopupResizeBottomBounds = new Rectangle(infoPopupX + 8, infoPopupY + infoPopupHeight - 6,
                Math.max(20, infoPopupWidth - 14), 6);
        infoPopupResizeCornerBounds = new Rectangle(infoPopupX + infoPopupWidth - 10, infoPopupY + infoPopupHeight - 10,
                10, 10);
        syncInfoPanelPreferences();
    }

    private List<String> buildInfoPopupWrappedLines(int mouseX, int mouseY, int contentWidth) {
        List<String> wrapped = new ArrayList<>();
        int safeWidth = Math.max(96, contentWidth);
        if (showSummaryInfoCard) {
            appendInfoPopupSection(wrapped, "动作摘要", buildInfoSummaryLines(), safeWidth);
        }
        if (showValidationInfoCard) {
            appendInfoPopupSection(wrapped, "实时检查", buildInfoValidationLines(), safeWidth);
        }
        if (showHelpInfoCard) {
            appendInfoPopupSection(wrapped, "参数提示", buildInfoHelpLines(mouseX, mouseY), safeWidth);
        }
        appendWrappedInfoLine(wrapped,
                hasVisibleInfoCards()
                        ? "§7当前为内嵌模式，可拖动分割条调整面板大小。"
                        : "§7拖动标题栏移动窗口，拖动右侧/底边调整大小。",
                safeWidth);
        return wrapped;
    }

    private void appendInfoPopupSection(List<String> target, String title, List<String> bodyLines, int contentWidth) {
        if (target == null) {
            return;
        }
        appendWrappedInfoLine(target, "§b§l" + title, contentWidth);
        if (bodyLines != null) {
            for (String line : bodyLines) {
                appendWrappedInfoLine(target, line, contentWidth);
            }
        }
        target.add("");
    }

    private void appendWrappedInfoLine(List<String> target, String line, int contentWidth) {
        if (target == null) {
            return;
        }
        List<String> split = fontRenderer.listFormattedStringToWidth(safe(line), Math.max(80, contentWidth));
        if (split == null || split.isEmpty()) {
            target.add(safe(line));
            return;
        }
        target.addAll(split);
    }

    private List<String> buildInfoSummaryLines() {
        List<String> lines = new ArrayList<>();
        lines.add("§7类型: §b" + I18n.format(ACTION_DISPLAY_KEYS.get(getSelectedActionType())));
        lines.add("§f" + safe(liveActionSummary.isEmpty() ? getSelectedActionPreviewText() : liveActionSummary));
        lines.add("§a效果: §f" + safe(liveActionEffectHint));
        lines.add((liveValidationErrorCount > 0 ? "§c风险: §f" : "§6风险: §f") + safe(liveActionRiskHint));
        return lines;
    }

    private List<String> buildInfoValidationLines() {
        List<String> lines = new ArrayList<>();
        if (liveValidationIssues.isEmpty()) {
            lines.add("§a" + GuiPathValidationReport.buildEmptyStateText(false));
            lines.add("当前配置可以直接保存，建议继续做一次动作测试。");
            return lines;
        }
        lines.add("§f" + GuiPathValidationReport.buildIssueSummaryText(liveValidationIssues, false));
        for (PathConfigValidator.Issue issue : liveValidationIssues) {
            if (issue == null) {
                continue;
            }
            String prefix = issue.getSeverity() == PathConfigValidator.Severity.ERROR ? "§c- " : "§e- ";
            lines.add(prefix + GuiPathValidationReport.buildIssueBodyText(issue));
        }
        return lines;
    }

    private List<String> buildInfoHelpLines(int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        String helpText = safe(getActiveParamHelpText(mouseX, mouseY)).trim();
        lines.add(helpText.isEmpty() ? "当前没有可显示的参数提示。" : helpText);
        return lines;
    }

    private void drawInfoPopupWindow(int mouseX, int mouseY) {
        ensureInfoPopupWindowBounds();
        drawRect(infoPopupX - 1, infoPopupY - 1, infoPopupX + infoPopupWidth + 1, infoPopupY + infoPopupHeight + 1,
                0xCC091118);
        GuiTheme.drawPanel(infoPopupX, infoPopupY, infoPopupWidth, infoPopupHeight);
        drawRect(infoPopupX + 1, infoPopupY + 1, infoPopupX + infoPopupWidth - 1,
                infoPopupHeaderBounds.y + infoPopupHeaderBounds.height, 0x99303F51);
        drawString(fontRenderer, "信息面板", infoPopupX + 8, infoPopupY + 7, 0xFFF4FAFF);

        boolean hoverClose = infoPopupCloseBounds != null && infoPopupCloseBounds.contains(mouseX, mouseY);
        drawInfoPopupDockButton(infoPopupDockFloatBounds, "浮", INFO_PANEL_DOCK_FLOAT.equals(infoPanelDockMode), mouseX, mouseY);
        drawInfoPopupDockButton(infoPopupDockMiddleBounds, "中", INFO_PANEL_DOCK_MIDDLE.equals(infoPanelDockMode), mouseX, mouseY);
        drawInfoPopupDockButton(infoPopupDockBottomBounds, "下", INFO_PANEL_DOCK_BOTTOM.equals(infoPanelDockMode), mouseX, mouseY);
        drawInfoPopupDockButton(infoPopupDockRightBounds, "右", INFO_PANEL_DOCK_RIGHT.equals(infoPanelDockMode), mouseX, mouseY);
        if (infoPopupCloseBounds != null) {
            GuiTheme.drawButtonFrameSafe(infoPopupCloseBounds.x, infoPopupCloseBounds.y, infoPopupCloseBounds.width,
                    infoPopupCloseBounds.height, hoverClose ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, "x", infoPopupCloseBounds.x + infoPopupCloseBounds.width / 2,
                    infoPopupCloseBounds.y + (infoPopupCloseBounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        int contentWidth = Math.max(90, infoPopupContentBounds.width - 10);
        List<String> wrappedLines = buildInfoPopupWrappedLines(mouseX, mouseY, contentWidth);
        int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        int visibleLines = Math.max(1, infoPopupContentBounds.height / lineHeight);
        infoPopupMaxScroll = Math.max(0, wrappedLines.size() - visibleLines);
        infoPopupScrollOffset = MathHelper.clamp(infoPopupScrollOffset, 0, infoPopupMaxScroll);

        GuiTheme.drawInputFrameSafe(infoPopupContentBounds.x - 2, infoPopupContentBounds.y - 2,
                infoPopupContentBounds.width + 4, infoPopupContentBounds.height + 4, false, true);
        beginScissor(infoPopupContentBounds.x, infoPopupContentBounds.y, infoPopupContentBounds.width,
                infoPopupContentBounds.height);
        int drawY = infoPopupContentBounds.y;
        for (int i = infoPopupScrollOffset; i < wrappedLines.size() && i < infoPopupScrollOffset + visibleLines; i++) {
            drawString(fontRenderer, wrappedLines.get(i), infoPopupContentBounds.x, drawY, 0xFFFFFFFF);
            drawY += lineHeight;
        }
        endScissor();

        if (infoPopupMaxScroll > 0) {
            int scrollbarHeight = infoPopupContentBounds.height;
            int thumbHeight = Math.max(14, (int) ((float) visibleLines / Math.max(1, wrappedLines.size()) * scrollbarHeight));
            int thumbY = infoPopupContentBounds.y
                    + (int) ((float) infoPopupScrollOffset / infoPopupMaxScroll * (scrollbarHeight - thumbHeight));
            GuiTheme.drawScrollbar(infoPopupX + infoPopupWidth - 10, infoPopupContentBounds.y, 6, scrollbarHeight, thumbY,
                    thumbHeight);
        }

        int handleColor = 0xFF6EAED9;
        if (isInfoPanelFloating()) {
            drawRect(infoPopupX + infoPopupWidth - 12, infoPopupY + infoPopupHeight - 4,
                    infoPopupX + infoPopupWidth - 4, infoPopupY + infoPopupHeight - 2, handleColor);
            drawRect(infoPopupX + infoPopupWidth - 8, infoPopupY + infoPopupHeight - 8,
                    infoPopupX + infoPopupWidth - 6, infoPopupY + infoPopupHeight - 4, handleColor);
        }
        if (infoDockDividerBounds != null) {
            drawInfoDockDividerHandle(infoDockDividerBounds, mouseX, mouseY, draggingInfoDockDivider,
                    infoDockDividerBounds.width > infoDockDividerBounds.height);
        }
    }

    private void drawInfoDockDividerHandle(Rectangle bounds, int mouseX, int mouseY, boolean dragging, boolean horizontal) {
        if (bounds == null) {
            return;
        }
        boolean hovered = bounds.contains(mouseX, mouseY);
        int accent = dragging ? 0xFF7CD9FF : (hovered ? 0xFF63BFEF : 0xFF3E617A);
        int actualX = bounds.x + Math.max(0, (bounds.width - INFO_DOCK_DIVIDER_SIZE) / 2);
        int actualY = bounds.y + Math.max(0, (bounds.height - INFO_DOCK_DIVIDER_SIZE) / 2);
        if (hovered || dragging) {
            drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0x35111922);
        }
        if (horizontal) {
            drawRect(bounds.x, actualY, bounds.x + bounds.width, actualY + INFO_DOCK_DIVIDER_SIZE, 0x77111922);
            int centerX = bounds.x + bounds.width / 2 - 12;
            for (int i = 0; i < 4; i++) {
                drawRect(centerX + i * 7, actualY + 3, centerX + i * 7 + 2,
                        actualY + INFO_DOCK_DIVIDER_SIZE - 3, accent);
            }
            return;
        }
        drawRect(actualX, bounds.y, actualX + INFO_DOCK_DIVIDER_SIZE, bounds.y + bounds.height, 0x77111922);
        drawRect(actualX + 5, bounds.y + 18, actualX + 7, bounds.y + bounds.height - 18, accent);
        int centerY = bounds.y + bounds.height / 2 - 12;
        for (int i = 0; i < 4; i++) {
            drawRect(actualX + 3, centerY + i * 7, actualX + INFO_DOCK_DIVIDER_SIZE - 3, centerY + i * 7 + 2, accent);
        }
    }

    private void drawInfoPopupDockButton(Rectangle bounds, String label, boolean selected, int mouseX, int mouseY) {
        if (bounds == null) {
            return;
        }
        boolean hovered = bounds.contains(mouseX, mouseY);
        GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawCenteredString(fontRenderer, label, bounds.x + bounds.width / 2,
                bounds.y + (bounds.height - fontRenderer.FONT_HEIGHT) / 2,
                selected ? 0xFF7AD9FF : 0xFFFFFFFF);
    }

    private void closeInfoPopup() {
        boolean wasDocked = hasVisibleInfoCards();
        showSummaryInfoCard = false;
        showValidationInfoCard = false;
        showHelpInfoCard = false;
        draggingInfoPopup = false;
        resizingInfoPopupRight = false;
        resizingInfoPopupBottom = false;
        resizingInfoPopupCorner = false;
        draggingInfoDockDivider = false;
        updateInfoPanelVisibilityState(wasDocked);
        syncInfoPanelPreferences();
    }

    private boolean handleInfoPopupClick(int mouseX, int mouseY, int mouseButton) {
        if (!hasActiveInfoSections()) {
            return false;
        }
        ensureInfoPopupWindowBounds();
        if (mouseButton == 0 && infoDockDividerBounds != null && infoDockDividerBounds.contains(mouseX, mouseY)) {
            draggingInfoDockDivider = true;
            infoPopupResizeStartMouseX = mouseX;
            infoPopupResizeStartMouseY = mouseY;
            return true;
        }
        if (infoPopupBounds == null || !infoPopupBounds.contains(mouseX, mouseY)) {
            return false;
        }
        if (mouseButton != 0) {
            return true;
        }
        if (infoPopupCloseBounds != null && infoPopupCloseBounds.contains(mouseX, mouseY)) {
            closeInfoPopup();
            savePreferences();
            return true;
        }
        if (infoPopupDockFloatBounds != null && infoPopupDockFloatBounds.contains(mouseX, mouseY)) {
            boolean wasDocked = hasVisibleInfoCards();
            infoPanelDockMode = INFO_PANEL_DOCK_FLOAT;
            infoPopupX = infoPopupLastFloatX;
            infoPopupY = infoPopupLastFloatY;
            syncInfoPanelPreferences();
            savePreferences();
            if (wasDocked) {
                rebuildEditorLayoutPreservingState();
            }
            return true;
        }
        if (infoPopupDockMiddleBounds != null && infoPopupDockMiddleBounds.contains(mouseX, mouseY)) {
            if (isInfoPanelFloating()) {
                infoPopupLastFloatX = infoPopupX;
                infoPopupLastFloatY = infoPopupY;
            }
            infoPanelDockMode = INFO_PANEL_DOCK_MIDDLE;
            syncInfoPanelPreferences();
            savePreferences();
            rebuildEditorLayoutPreservingState();
            return true;
        }
        if (infoPopupDockBottomBounds != null && infoPopupDockBottomBounds.contains(mouseX, mouseY)) {
            if (isInfoPanelFloating()) {
                infoPopupLastFloatX = infoPopupX;
                infoPopupLastFloatY = infoPopupY;
            }
            infoPanelDockMode = INFO_PANEL_DOCK_BOTTOM;
            syncInfoPanelPreferences();
            savePreferences();
            rebuildEditorLayoutPreservingState();
            return true;
        }
        if (infoPopupDockRightBounds != null && infoPopupDockRightBounds.contains(mouseX, mouseY)) {
            if (isInfoPanelFloating()) {
                infoPopupLastFloatX = infoPopupX;
                infoPopupLastFloatY = infoPopupY;
            }
            infoPanelDockMode = INFO_PANEL_DOCK_RIGHT;
            syncInfoPanelPreferences();
            savePreferences();
            rebuildEditorLayoutPreservingState();
            return true;
        }
        if (isInfoPanelFloating() && infoPopupResizeCornerBounds != null && infoPopupResizeCornerBounds.contains(mouseX, mouseY)) {
            resizingInfoPopupCorner = true;
        } else if (isInfoPanelFloating() && infoPopupResizeRightBounds != null && infoPopupResizeRightBounds.contains(mouseX, mouseY)) {
            resizingInfoPopupRight = true;
        } else if (isInfoPanelFloating() && infoPopupResizeBottomBounds != null && infoPopupResizeBottomBounds.contains(mouseX, mouseY)) {
            resizingInfoPopupBottom = true;
        } else if (isInfoPanelFloating() && infoPopupHeaderBounds != null && infoPopupHeaderBounds.contains(mouseX, mouseY)) {
            draggingInfoPopup = true;
            infoPopupDragOffsetX = mouseX - infoPopupX;
            infoPopupDragOffsetY = mouseY - infoPopupY;
            return true;
        } else {
            return true;
        }
        infoPopupResizeStartMouseX = mouseX;
        infoPopupResizeStartMouseY = mouseY;
        infoPopupResizeStartWidth = infoPopupPreferredWidth;
        infoPopupResizeStartHeight = infoPopupPreferredHeight;
        return true;
    }

    private void handleInfoPopupDrag(int mouseX, int mouseY) {
        if (!hasActiveInfoSections()) {
            return;
        }
        if (draggingInfoPopup) {
            infoPopupX = mouseX - infoPopupDragOffsetX;
            infoPopupY = mouseY - infoPopupDragOffsetY;
            infoPopupLastFloatX = infoPopupX;
            infoPopupLastFloatY = infoPopupY;
            ensureInfoPopupWindowBounds();
            savePreferences();
            return;
        }
        if (draggingInfoDockDivider) {
            if (isInfoPanelDockedMiddle()) {
                infoDockPreferredWidth += mouseX - infoPopupResizeStartMouseX;
                infoPopupResizeStartMouseX = mouseX;
            } else if (isInfoPanelDockedRight()) {
                infoDockPreferredWidth -= mouseX - infoPopupResizeStartMouseX;
                infoPopupResizeStartMouseX = mouseX;
            } else if (isInfoPanelDockedBottom()) {
                infoDockPreferredHeight -= mouseY - infoPopupResizeStartMouseY;
                infoPopupResizeStartMouseY = mouseY;
            }
            ensureInfoPopupWindowBounds();
            rebuildEditorLayoutPreservingState();
            draggingInfoDockDivider = true;
            infoPopupResizeStartMouseX = mouseX;
            infoPopupResizeStartMouseY = mouseY;
            savePreferences();
            return;
        }
        if (resizingInfoPopupCorner || resizingInfoPopupRight || resizingInfoPopupBottom) {
            if (resizingInfoPopupCorner || resizingInfoPopupRight) {
                infoPopupPreferredWidth = infoPopupResizeStartWidth + (mouseX - infoPopupResizeStartMouseX);
            }
            if (resizingInfoPopupCorner || resizingInfoPopupBottom) {
                infoPopupPreferredHeight = infoPopupResizeStartHeight + (mouseY - infoPopupResizeStartMouseY);
            }
            ensureInfoPopupWindowBounds();
            savePreferences();
        }
    }

    private boolean handleInfoPopupWheel(int mouseX, int mouseY, int dWheel) {
        if (!hasActiveInfoSections() || dWheel == 0) {
            return false;
        }
        ensureInfoPopupWindowBounds();
        if (infoPopupBounds == null || !infoPopupBounds.contains(mouseX, mouseY)) {
            return false;
        }
        if (infoPopupMaxScroll <= 0) {
            return true;
        }
        if (dWheel > 0) {
            infoPopupScrollOffset = Math.max(0, infoPopupScrollOffset - 2);
        } else {
            infoPopupScrollOffset = Math.min(infoPopupMaxScroll, infoPopupScrollOffset + 2);
        }
        return true;
    }

    private void applyPresetDraft(JsonObject draft) {
        if (draft == null) {
            return;
        }
        this.currentParams = draft;
        this.hasUnsavedChanges = true;
        this.pendingSwitchActionType = null;
        int oldScroll = this.paramScrollOffset;
        initGui();
        this.paramScrollOffset = Math.max(0, Math.min(oldScroll, maxParamScroll));
        refreshDynamicParamLayout();
        this.hasUnsavedChanges = true;
    }

    private void applyHuntPreset(int presetIndex) {
        JsonObject draft = cloneJsonObject(currentParams);
        draft.addProperty("radius", 6.0D);
        draft.addProperty("attackCount", 0);
        draft.addProperty("autoAttack", true);
        draft.addProperty("huntAimLockEnabled", true);
        draft.addProperty("trackingDistance", 1.2D);
        draft.addProperty("targetHostile", true);
        draft.addProperty("targetPassive", false);
        draft.addProperty("targetPlayers", false);
        draft.addProperty("noTargetSkipCount", 0);
        draft.addProperty("huntChaseIntervalEnabled", false);
        draft.addProperty("huntChaseIntervalSeconds", 0.0D);
        draft.addProperty("showHuntRange", false);

        switch (presetIndex) {
            case 0:
                draft.addProperty("attackMode", KillAuraHandler.ATTACK_MODE_NORMAL);
                draft.addProperty("huntMode", KillAuraHandler.HUNT_MODE_APPROACH);
                draft.addProperty("huntOrbitEnabled", false);
                break;
            case 1:
                draft.addProperty("attackMode", KillAuraHandler.ATTACK_MODE_NORMAL);
                draft.addProperty("huntMode", KillAuraHandler.HUNT_MODE_FIXED_DISTANCE);
                draft.addProperty("trackingDistance", 1.6D);
                draft.addProperty("huntOrbitEnabled", true);
                break;
            case 2:
                draft.addProperty("attackMode", KillAuraHandler.ATTACK_MODE_SEQUENCE);
                draft.addProperty("huntMode", KillAuraHandler.HUNT_MODE_APPROACH);
                draft.addProperty("huntOrbitEnabled", false);
                break;
            default:
                return;
        }
        applyPresetDraft(draft);
    }

    private void applyRunSequencePreset(int presetIndex) {
        JsonObject draft = cloneJsonObject(currentParams);
        switch (presetIndex) {
            case 0:
                draft.addProperty("executeMode", "always");
                draft.addProperty("executeEveryCount", 1);
                draft.addProperty("backgroundExecution", false);
                break;
            case 1:
                draft.addProperty("executeMode", "interval");
                draft.addProperty("executeEveryCount", 3);
                draft.addProperty("backgroundExecution", false);
                break;
            case 2:
                draft.addProperty("executeMode", "always");
                draft.addProperty("executeEveryCount", 1);
                draft.addProperty("backgroundExecution", true);
                break;
            default:
                return;
        }
        applyPresetDraft(draft);
    }

    private List<PathConfigValidator.Issue> buildDraftValidationIssues(ActionData draft) {
        return draft == null
                ? Collections.emptyList()
                : PathConfigValidator.validateActionDraft(draft, currentSequenceName, PathSequenceManager.getAllSequences());
    }

    private void openCurrentActionTestReport() {
        ActionData draft = buildEditorActionData();
        List<PathConfigValidator.Issue> issues = buildDraftValidationIssues(draft);
        mc.displayGuiScreen(new GuiPathValidationReport(this, "动作测试预检", "关闭", issues, null));
    }

    private void drawWrappedText(List<String> lines, int x, int startY, int color) {
        int y = startY;
        for (String line : lines) {
            this.drawString(this.fontRenderer, line, x, y, color);
            y += 10;
        }
    }

    private List<String> wrapTextLimited(String text, int width, int maxLines) {
        List<String> lines = this.fontRenderer.listFormattedStringToWidth(
                stripHelpFormatting(text == null ? "" : text), Math.max(20, width));
        if (lines.size() <= maxLines) {
            return lines;
        }
        List<String> trimmed = new ArrayList<>(lines.subList(0, maxLines));
        String last = trimmed.get(maxLines - 1);
        String ellipsis = "...";
        while (!last.isEmpty() && this.fontRenderer.getStringWidth(last + ellipsis) > Math.max(20, width)) {
            last = last.substring(0, last.length() - 1);
        }
        trimmed.set(maxLines - 1, last + ellipsis);
        return trimmed;
    }

    private void drawScaledWrappedTextToFit(String text, int x, int startY, int maxWidth, int maxHeight,
            int color) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.isEmpty() || maxWidth <= 0 || maxHeight <= 0) {
            return;
        }

        float bestScale = 0.1F;
        List<String> bestLines = this.fontRenderer.listFormattedStringToWidth(safeText, maxWidth);
        for (float scale = 1.0F; scale >= 0.1F; scale -= 0.05F) {
            int effectiveWidth = Math.max(1, Math.round(maxWidth / scale));
            List<String> candidateLines = this.fontRenderer.listFormattedStringToWidth(safeText, effectiveWidth);
            float candidateHeight = candidateLines.size() * 10.0F * scale;
            if (candidateHeight > maxHeight) {
                continue;
            }
            int widestLine = 0;
            for (String line : candidateLines) {
                widestLine = Math.max(widestLine, this.fontRenderer.getStringWidth(line));
            }
            if (widestLine * scale <= maxWidth + 0.5F) {
                bestScale = scale;
                bestLines = candidateLines;
                break;
            }
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, startY, 0.0F);
        GlStateManager.scale(bestScale, bestScale, 1.0F);
        int drawY = 0;
        for (String line : bestLines) {
            this.drawString(this.fontRenderer, line, 0, drawY, color);
            drawY += 10;
        }
        GlStateManager.popMatrix();
    }

    void addTextField(String label, String paramKey, String helpText, int width, int x, int y) {
        addTextField(label, paramKey, helpText, width, x, y, "");
    }

    void addSectionTitle(String title, int x, int y) {
        // Add a dummy field entry for the section title that will be rendered as a
        // label
        paramLabels.add(title);
        paramHelpTexts.add("");
        paramFieldKeys.add("_section_title_" + paramFields.size());

        // Create an invisible/disabled text field as a placeholder
        GuiTextField dummyField = new GuiTextField(paramFields.size() + 1, this.fontRenderer, x, y, 1, 1);
        dummyField.setVisible(false);
        dummyField.setEnabled(false);

        paramFields.add(dummyField);
        paramFieldBaseY.add(y);
    }

    void addTextField(String label, String paramKey, String helpText, int width, int x, int y,
            String defaultValue) {
        paramLabels.add(label + ":");
        paramHelpTexts.add("§8" + helpText);
        paramFieldKeys.add(paramKey);
        GuiTextField field = new GuiTextField(paramFields.size() + 1, this.fontRenderer, x, y, width, 20);

        field.setMaxStringLength(Integer.MAX_VALUE);

        if (currentParams.has(paramKey)) {
            field.setText(currentParams.get(paramKey).toString().replace("\"", ""));
        } else {
            field.setText(defaultValue);
        }
        paramFields.add(field);
        paramFieldBaseY.add(y);
    }

    void addDropdown(String label, String paramKey, String helpText, int width, int x, int y,
            String[] options, String defaultValue) {
        paramDropdownLabels.add(label + ":");
        paramDropdownHelpTexts.add("§8" + helpText);
        paramDropdownKeys.add(paramKey);
        EnumDropdown dropdown = new EnumDropdown(x, y, width, 20, options);
        dropdown.setValue(defaultValue);
        paramDropdowns.add(dropdown);
        paramDropdownBaseY.add(y);
    }

    int addPresetButtons(int baseId, String[] labels, int x, int y, int width) {
        if (labels == null || labels.length == 0) {
            return 0;
        }
        int gap = 6;
        int count = labels.length;
        int buttonWidth = Math.max(66, (width - gap * Math.max(0, count - 1)) / Math.max(1, count));
        int totalWidth = buttonWidth * count + gap * Math.max(0, count - 1);
        int startX = x + Math.max(0, (width - totalWidth) / 2);
        for (int i = 0; i < labels.length; i++) {
            ThemedButton button = new ThemedButton(baseId + i, startX + i * (buttonWidth + gap), y, buttonWidth, 20,
                    labels[i]);
            this.buttonList.add(button);
            registerScrollableButton(button, y);
        }
        return 30;
    }

    int addScopedVariableEditor(String label, String actualParamKey, String helpText, int width, int x, int y,
            String defaultBaseName) {
        return ActionVariableBindingSupport.addScopedVariableEditor(this, label, actualParamKey, helpText, width, x, y,
                defaultBaseName);
    }

    int addGroupedRuntimeVariableSelector(String label, String actualParamKey, String helpText, int width,
            int x, int y) {
        return ActionVariableBindingSupport.addGroupedRuntimeVariableSelector(this, label, actualParamKey, helpText,
                width, x, y);
    }

    int addExpressionTemplateEditor(String label, String actualParamKey, int width, int x, int y) {
        return ActionVariableBindingSupport.addExpressionTemplateEditor(this, label, actualParamKey, width, x, y);
    }

    private boolean isExpressionEditorFieldKey(String key) {
        return ActionVariableBindingSupport.isExpressionEditorFieldKey(this, key);
    }

    boolean isExpressionEditorPopupOpen() {
        return expressionPopupSearchField != null
                && expressionPopupInputField != null
                && (activeExpressionEditorBinding != null
                        || activeBooleanExpressionEditIndex != BOOLEAN_EXPRESSION_EDIT_NONE);
    }

    private void drawExpressionEditorButton(GuiTextField field, int mouseX, int mouseY) {
        ActionVariableBindingSupport.drawExpressionEditorButton(this, field, mouseX, mouseY);
    }

    private ExpressionEditorBinding getExpressionEditorBindingAt(int mouseX, int mouseY) {
        return ActionVariableBindingSupport.getExpressionEditorBindingAt(this, mouseX, mouseY);
    }

    void openExpressionEditorPopup(ExpressionEditorBinding binding) {
        ExpressionPopupSupport.openBinding(this, binding);
    }

    void openBooleanExpressionPopup(int editIndex) {
        ExpressionPopupSupport.openBoolean(this, editIndex);
    }

    void openExpressionEditorPopup(String initialValue, String title, boolean booleanOnly) {
        ExpressionPopupSupport.open(this, initialValue, title, booleanOnly);
    }

    void closeExpressionEditorPopup() {
        ExpressionPopupSupport.close(this);
    }

    void commitExpressionEditorPopup() {
        ExpressionPopupSupport.commit(this);
    }

    void cancelExpressionEditorPopup() {
        ExpressionPopupSupport.cancel(this);
    }

    int getExpressionPopupX() {
        int panelWidth = getEditorPanelWidth();
        int popupWidth = getExpressionPopupWidth();
        return getEditorPanelX() + (panelWidth - popupWidth) / 2;
    }

    int getExpressionPopupY() {
        int panelHeight = getEditorPanelHeight();
        int popupHeight = getExpressionPopupHeight();
        return getEditorPanelY() + (panelHeight - popupHeight) / 2;
    }

    int getExpressionPopupWidth() {
        int panelWidth = getEditorPanelWidth();
        return Math.max(320, Math.min(panelWidth - 24, 640));
    }

    int getExpressionPopupHeight() {
        int panelHeight = getEditorPanelHeight();
        return Math.max(260, Math.min(panelHeight - 20, 420));
    }

    int getExpressionPopupSearchX() {
        return getExpressionPopupX() + 12;
    }

    int getExpressionPopupSearchY() {
        return getExpressionPopupY() + 34;
    }

    int getExpressionPopupSearchWidth() {
        return getExpressionPopupWidth() - 24;
    }

    int getExpressionPopupInputX() {
        return getExpressionPopupX() + 12;
    }

    int getExpressionPopupInputY() {
        return getExpressionPopupY() + getExpressionPopupHeight() - 28;
    }

    int getExpressionPopupInputWidth() {
        return Math.max(120, getExpressionPopupConfirmButtonX() - getExpressionPopupInputX() - 8);
    }

    int getExpressionPopupViewportX() {
        return getExpressionPopupX() + 12;
    }

    int getExpressionPopupViewportY() {
        return getExpressionPopupY() + 64;
    }

    int getExpressionPopupViewportWidth() {
        return getExpressionPopupWidth() - 24;
    }

    int getExpressionPopupViewportHeight() {
        return Math.max(60, getExpressionPopupInputY() - getExpressionPopupViewportY() - 12);
    }

    int getExpressionPopupScrollbarWidth() {
        return 8;
    }

    int getExpressionPopupScrollbarGap() {
        return 4;
    }

    int getExpressionPopupScrollbarX() {
        return getExpressionPopupViewportX() + getExpressionPopupViewportWidth() - getExpressionPopupScrollbarWidth() - 2;
    }

    int getExpressionPopupScrollbarY() {
        return getExpressionPopupViewportY() + 1;
    }

    int getExpressionPopupScrollbarHeight() {
        return Math.max(8, getExpressionPopupViewportHeight() - 2);
    }

    int getExpressionPopupTemplateContentWidth() {
        return Math.max(120,
                getExpressionPopupViewportWidth() - 8 - getExpressionPopupScrollbarGap() - getExpressionPopupScrollbarWidth());
    }

    int getExpressionPopupScrollbarThumbHeight(int contentHeight) {
        int viewportHeight = Math.max(1, getExpressionPopupScrollbarHeight());
        return Math.max(18, (int) ((float) viewportHeight / Math.max(viewportHeight, contentHeight) * viewportHeight));
    }

    int getExpressionPopupScrollbarThumbY(int contentHeight) {
        int scrollbarY = getExpressionPopupScrollbarY();
        int scrollbarHeight = getExpressionPopupScrollbarHeight();
        int thumbHeight = getExpressionPopupScrollbarThumbHeight(contentHeight);
        if (expressionPopupMaxScroll <= 0) {
            return scrollbarY;
        }
        return scrollbarY + (int) ((float) expressionPopupScrollOffset / expressionPopupMaxScroll
                * Math.max(1, scrollbarHeight - thumbHeight));
    }

    void updateExpressionPopupScrollFromMouse(int mouseY, int contentHeight) {
        if (expressionPopupMaxScroll <= 0) {
            expressionPopupScrollOffset = 0;
            return;
        }
        int scrollbarY = getExpressionPopupScrollbarY();
        int scrollbarHeight = getExpressionPopupScrollbarHeight();
        int thumbHeight = getExpressionPopupScrollbarThumbHeight(contentHeight);
        int scrollableTrackHeight = scrollbarHeight - thumbHeight;
        if (scrollableTrackHeight <= 0) {
            expressionPopupScrollOffset = 0;
            return;
        }
        float scrollRatio = (float) (mouseY - scrollbarY - thumbHeight / 2) / scrollableTrackHeight;
        scrollRatio = Math.max(0.0f, Math.min(1.0f, scrollRatio));
        expressionPopupScrollOffset = (int) (scrollRatio * expressionPopupMaxScroll);
        expressionPopupScrollOffset = Math.max(0, Math.min(expressionPopupMaxScroll, expressionPopupScrollOffset));
    }

    int getExpressionPopupButtonY() {
        return getExpressionPopupInputY();
    }

    int getExpressionPopupButtonWidth() {
        return 54;
    }

    int getExpressionPopupButtonHeight() {
        return 20;
    }

    int getExpressionPopupButtonGap() {
        return 6;
    }

    int getExpressionPopupCancelButtonX() {
        return getExpressionPopupX() + getExpressionPopupWidth() - 12 - getExpressionPopupButtonWidth();
    }

    int getExpressionPopupConfirmButtonX() {
        return getExpressionPopupCancelButtonX() - getExpressionPopupButtonGap() - getExpressionPopupButtonWidth();
    }

    void syncExpressionPopupFieldBounds() {
        ExpressionPopupSupport.syncBounds(this);
    }

    private void drawExpressionEditorPopup(int mouseX, int mouseY) {
        ExpressionPopupSupport.draw(this, mouseX, mouseY);
    }

    

    

    private void handleExpressionEditorPopupClick(int mouseX, int mouseY, int mouseButton) {
        ExpressionPopupSupport.handleClick(this, mouseX, mouseY, mouseButton);
    }

    private void handleExpressionEditorPopupKeyTyped(char typedChar, int keyCode) throws IOException {
        ExpressionPopupSupport.handleKeyTyped(this, typedChar, keyCode);
    }

    private void handleExpressionEditorPopupScroll(int dWheel) {
        ExpressionPopupSupport.handleScroll(this, dWheel);
    }

    List<ExpressionTemplateCard> getFilteredExpressionTemplateCards(String searchText) {
        return ExpressionPopupSupport.getFilteredCards(this, searchText);
    }

    

    List<ExpressionTemplateLayoutEntry> buildExpressionTemplateLayoutEntries(List<ExpressionTemplateCard> cards) {
        return ExpressionPopupSupport.buildLayoutEntries(this, cards);
    }

    int computeExpressionTemplateContentHeight(List<ExpressionTemplateLayoutEntry> entries) {
        return ExpressionPopupSupport.computeContentHeight(this, entries);
    }

    ExpressionTemplateLayoutEntry getHoveredExpressionTemplateEntry(List<ExpressionTemplateLayoutEntry> entries,
            int mouseX, int mouseY) {
        return ExpressionPopupSupport.getHoveredEntry(this, entries, mouseX, mouseY);
    }

    

    List<String> wrapExpressionCardText(String text, int width, int maxLines) {
        List<String> wrapped = this.fontRenderer.listFormattedStringToWidth(safe(text), Math.max(20, width));
        if (wrapped == null || wrapped.isEmpty()) {
            wrapped = new ArrayList<>();
            wrapped.add("");
        }
        List<String> result = new ArrayList<>();
        int limit = Math.max(1, maxLines);
        for (int i = 0; i < wrapped.size() && i < limit; i++) {
            String line = wrapped.get(i);
            if (i == limit - 1 && wrapped.size() > limit) {
                line = this.fontRenderer.trimStringToWidth(line, Math.max(20, width - 6)) + "...";
            }
            result.add(line);
        }
        return result;
    }

    private ExpressionTemplateCard expressionCard(String name, String example, String description, String format,
            String outputExample, String... keywords) {
        return new ExpressionTemplateCard(name, example, description, format, outputExample, keywords);
    }


    void beginScissor(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        int scale = scaledResolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + height)) * scale, width * scale, height * scale);
    }

    void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private int addConditionExpressionEditor(int width, int x, int y) {
        return ConditionExpressionEditorSupport.addEditor(this, width, x, y);
    }

    EnumDropdown getDropdownByKey(String key) {
        int index = paramDropdownKeys.indexOf(key);
        if (index < 0 || index >= paramDropdowns.size()) {
            return null;
        }
        return paramDropdowns.get(index);
    }

    GuiTextField getFieldByKey(String key) {
        int index = paramFieldKeys.indexOf(key);
        if (index < 0 || index >= paramFields.size()) {
            return null;
        }
        return paramFields.get(index);
    }

    private void onVariableSelectorDropdownChanged(String dropdownKey, String oldValue, String newValue) {
        ActionVariableBindingSupport.onVariableSelectorDropdownChanged(this, dropdownKey, oldValue, newValue);
    }

    private boolean shouldRefreshConditionExpressionLayout(String dropdownKey, String oldValue, String newValue) {
        return ConditionExpressionEditorSupport.shouldRefreshLayout(this, dropdownKey, oldValue, newValue);
    }

    private String getDraftSlotLocatorMode() {
        int dropdownIndex = paramDropdownKeys.indexOf("locatorMode");
        if (dropdownIndex >= 0 && dropdownIndex < paramDropdowns.size()) {
            return displayToLocatorMode(paramDropdowns.get(dropdownIndex).getValue());
        }
        return currentParams.has("locatorMode")
                ? currentParams.get("locatorMode").getAsString()
                : ActionTargetLocator.SLOT_MODE_DIRECT;
    }

    private boolean isDirectSlotLocatorMode(String mode) {
        return mode == null || mode.trim().isEmpty() || ActionTargetLocator.SLOT_MODE_DIRECT.equalsIgnoreCase(mode);
    }

    private boolean usesDynamicSlotLocatorLayout(String actionType) {
        String normalized = actionType == null ? "" : actionType.trim().toLowerCase(Locale.ROOT);
        return "window_click".equals(normalized)
                || "conditional_window_click".equals(normalized)
                || "autochestclick".equals(normalized);
    }

    private boolean shouldRefreshDynamicLocatorLayout(int dropdownIndex, String oldValue, String newValue) {
        if (dropdownIndex < 0 || dropdownIndex >= paramDropdownKeys.size()) {
            return false;
        }
        String dropdownKey = paramDropdownKeys.get(dropdownIndex);
        if ("attackMode".equals(dropdownKey) && "hunt".equalsIgnoreCase(getSelectedActionType())) {
            return !Objects.equals(oldValue, newValue);
        }
        if (!"locatorMode".equals(dropdownKey)) {
            return false;
        }
        if (Objects.equals(oldValue, newValue)) {
            return false;
        }
        return usesDynamicSlotLocatorLayout(getSelectedActionType());
    }

    private void captureCurrentEditorDraftParams() {
        ActionVariableBindingSupport.captureCurrentEditorDraftParams(this);
    }

    String buildConditionExpressionFromEditor() {
        return ConditionExpressionEditorSupport.buildExpressionFromEditor(this);
    }

    void addToggle(String label, String paramKey, String helpText, int width, int x, int y,
            boolean defaultValue, String trueLabel, String falseLabel) {
        toggleLabels.add(label + ":");
        toggleHelpTexts.add("§8" + helpText);
        toggleKeys.add(paramKey);
        ToggleOptionButton button = new ToggleOptionButton(BTN_ID_TOGGLE_BASE + toggleButtons.size(),
                x, y, width, 20, paramKey, trueLabel, falseLabel, defaultValue);
        toggleButtons.add(button);
        toggleBaseY.add(y);
        this.buttonList.add(button);
    }

    void registerScrollableButton(GuiButton button, int baseY) {
        if (button != null) {
            scrollableButtonBaseY.put(button.id, baseY);
        }
    }

    void addEditorButton(GuiButton button) {
        if (button != null) {
            this.buttonList.add(button);
        }
    }

    FontRenderer getEditorFontRenderer() {
        return this.fontRenderer;
    }

    int getScreenWidth() {
        return this.width;
    }

    int getScreenHeight() {
        return this.height;
    }

    void drawEditorTextField(GuiTextField field) {
        drawThemedTextField(field);
    }

    void showEditorHoveringText(List<String> lines, int mouseX, int mouseY) {
        drawHoveringText(lines, mouseX, mouseY);
    }

    private boolean isConditionInventoryActionType(String actionType) {
        return "condition_inventory_item".equalsIgnoreCase(actionType)
                || "wait_until_inventory_item".equalsIgnoreCase(actionType);
    }

    private boolean isConditionInventoryActionSelected() {
        return isConditionInventoryActionType(getSelectedActionType());
    }

    boolean isBooleanExpressionActionSelected() {
        String actionType = getSelectedActionType();
        return "condition_expression".equalsIgnoreCase(actionType)
                || "wait_until_expression".equalsIgnoreCase(actionType);
    }

    void initializeBooleanExpressionEditorState() {
        BooleanExpressionEditorSupport.initializeState(this);
    }

    List<String> getBooleanExpressionList() {
        return BooleanExpressionEditorSupport.getExpressionList(this);
    }

    void applyBooleanExpressionListToCurrentParams(List<String> expressions) {
        BooleanExpressionEditorSupport.applyExpressionListToCurrentParams(this, expressions);
    }

    private String buildLegacyBooleanExpression(List<String> expressions) {
        return BooleanExpressionEditorSupport.buildLegacyExpression(expressions);
    }

    int addBooleanExpressionCardEditor(int width, int x, int y) {
        return BooleanExpressionEditorSupport.addCardEditor(this, width, x, y);
    }

    private void updateBooleanExpressionControlLayout() {
        BooleanExpressionEditorSupport.updateControlLayout(this);
    }

    private int getBooleanExpressionCustomBottomBaseY() {
        return BooleanExpressionEditorSupport.getCustomBottomBaseY(this);
    }

    void initializeConditionInventoryNbtState() {
        conditionInventoryRequiredNbtTags.clear();
        conditionInventorySelectedSlots.clear();
        conditionInventoryDragSelectionSnapshot.clear();
        conditionInventoryTagRemoveRegions.clear();
        conditionInventorySlotRegions.clear();
        conditionInventoryNbtTagScrollOffset = 0;
        conditionInventoryGridDragging = false;
        conditionInventoryDragAddMode = true;
        conditionInventoryDragAnchorIndex = -1;
        conditionInventoryDragCurrentIndex = -1;
        readConditionInventoryTagList(currentParams, "requiredNbtTags", "requiredNbtTagsText",
                conditionInventoryRequiredNbtTags);
        readMoveChestIndexList(currentParams, "inventorySlots", "inventorySlotsText", conditionInventorySelectedSlots);
    }

    private void readConditionInventoryTagList(JsonObject source, String arrayKey, String textKey,
            List<String> target) {
        if (source == null || target == null) {
            return;
        }
        if (source.has(arrayKey) && source.get(arrayKey).isJsonArray()) {
            for (JsonElement element : source.getAsJsonArray(arrayKey)) {
                if (element != null && element.isJsonPrimitive()) {
                    addConditionInventoryTag(element.getAsString());
                }
            }
            return;
        }

        String text = null;
        if (source.has(arrayKey) && source.get(arrayKey).isJsonPrimitive()) {
            text = source.get(arrayKey).getAsString();
        } else if (source.has(textKey) && source.get(textKey).isJsonPrimitive()) {
            text = source.get(textKey).getAsString();
        }
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        for (String token : text.split("\\r?\\n|,")) {
            addConditionInventoryTag(token);
        }
    }

    private void addConditionInventoryTag(String rawTag) {
        String normalized = rawTag == null ? "" : rawTag.trim();
        if (normalized.isEmpty()) {
            return;
        }
        for (String existing : conditionInventoryRequiredNbtTags) {
            if (existing.equalsIgnoreCase(normalized)) {
                return;
            }
        }
        conditionInventoryRequiredNbtTags.add(normalized);
    }

    private boolean isMoveChestActionSelected() {
        return ACTION_MOVE_INVENTORY_ITEMS_TO_CHEST_SLOTS.equalsIgnoreCase(getSelectedActionType());
    }

    private void initializeMoveChestSelectionState() {
        moveChestFilterRules.clear();
        moveChestSelectedChestSlots.clear();
        moveChestSelectedInventorySlots.clear();
        moveChestDragSelectionSnapshot.clear();
        moveChestGridDragging = false;
        moveChestDraggingChestGrid = false;
        moveChestDragAddMode = true;
        moveChestDragAnchorIndex = -1;
        moveChestDragCurrentIndex = -1;
        readMoveChestIndexList(currentParams, "chestSlots", "chestSlotsText", moveChestSelectedChestSlots);
        readMoveChestIndexList(currentParams, "inventorySlots", "inventorySlotsText", moveChestSelectedInventorySlots);
        moveChestDraftItemName = "";
        moveChestDraftNbtText = "";
        readMoveChestFilterRules(currentParams, moveChestFilterRules);
    }

    private void readMoveChestIndexList(JsonObject source, String arrayKey, String textKey, Set<Integer> target) {
        if (source == null || target == null) {
            return;
        }
        if (source.has(arrayKey) && source.get(arrayKey).isJsonArray()) {
            for (JsonElement element : source.getAsJsonArray(arrayKey)) {
                try {
                    target.add(Math.max(0, element.getAsInt()));
                } catch (Exception ignored) {
                }
            }
            return;
        }

        String text = null;
        if (source.has(arrayKey) && source.get(arrayKey).isJsonPrimitive()) {
            text = source.get(arrayKey).getAsString();
        } else if (source.has(textKey) && source.get(textKey).isJsonPrimitive()) {
            text = source.get(textKey).getAsString();
        }
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        for (String token : text.split("[,\\r\\n\\s]+")) {
            if (token == null || token.trim().isEmpty()) {
                continue;
            }
            try {
                target.add(Math.max(0, Integer.parseInt(token.trim())));
            } catch (Exception ignored) {
            }
        }
    }

    private String readMoveChestTagText(JsonObject source, String arrayKey, String textKey) {
        if (source == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        if (source.has(arrayKey) && source.get(arrayKey).isJsonArray()) {
            for (JsonElement element : source.getAsJsonArray(arrayKey)) {
                if (element != null && element.isJsonPrimitive()) {
                    String value = safe(element.getAsString()).trim();
                    if (!value.isEmpty()) {
                        values.add(value);
                    }
                }
            }
            if (!values.isEmpty()) {
                return String.join(", ", values);
            }
        }

        if (source.has(textKey) && source.get(textKey).isJsonPrimitive()) {
            return safe(source.get(textKey).getAsString());
        }
        if (source.has(arrayKey) && source.get(arrayKey).isJsonPrimitive()) {
            return safe(source.get(arrayKey).getAsString());
        }
        return "";
    }

    private void readMoveChestFilterRules(JsonObject source, List<MoveChestFilterRuleDraft> target) {
        if (source == null || target == null) {
            return;
        }
        if (source.has("moveChestRules") && source.get("moveChestRules").isJsonArray()) {
            for (JsonElement element : source.getAsJsonArray("moveChestRules")) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject ruleObject = element.getAsJsonObject();
                String itemName = ruleObject.has("itemName") && ruleObject.get("itemName").isJsonPrimitive()
                        ? safe(ruleObject.get("itemName").getAsString())
                        : "";
                String nbtText = readMoveChestTagText(ruleObject, "requiredNbtTags", "requiredNbtTagsText");
                addMoveChestFilterRule(itemName, nbtText);
            }
            if (!target.isEmpty()) {
                return;
            }
        }

        String legacyItemName = source.has("itemName") && source.get("itemName").isJsonPrimitive()
                ? safe(source.get("itemName").getAsString())
                : "";
        String legacyNbtText = readMoveChestTagText(source, "requiredNbtTags", "requiredNbtTagsText");
        addMoveChestFilterRule(legacyItemName, legacyNbtText);
    }

    private void addMoveChestFilterRule(String itemName, String nbtText) {
        MoveChestFilterRuleDraft rule = new MoveChestFilterRuleDraft(itemName, nbtText);
        if (rule.isEmpty()) {
            return;
        }
        for (MoveChestFilterRuleDraft existing : moveChestFilterRules) {
            if (existing.itemName.equalsIgnoreCase(rule.itemName) && existing.nbtText.equalsIgnoreCase(rule.nbtText)) {
                return;
            }
        }
        moveChestFilterRules.add(rule);
    }

    private void updateConditionInventoryNbtControlLayout() {
        if (!isConditionInventoryActionSelected() || conditionInventoryNbtTagInputField == null) {
            return;
        }

        int fieldWidth = getParamFieldWidth();
        int x = getParamContentX();
        int inputY = conditionInventoryNbtTagInputBaseY - paramScrollOffset;
        boolean wrap = shouldWrapTagInputRow(fieldWidth);
        int inputWidth = wrap ? fieldWidth : Math.max(80, fieldWidth - 88);
        conditionInventoryNbtTagInputField.x = x;
        conditionInventoryNbtTagInputField.y = inputY;
        conditionInventoryNbtTagInputField.width = inputWidth;
        conditionInventoryNbtTagInputField.height = 20;
        conditionInventoryNbtTagInputField.setVisible(inputY + 20 >= paramViewTop && inputY <= paramViewBottom);

        if (btnAddConditionInventoryNbtTag != null) {
            btnAddConditionInventoryNbtTag.x = wrap ? x : x + inputWidth + PARAM_INLINE_GAP;
            btnAddConditionInventoryNbtTag.y = (wrap ? inputY + 28 : inputY);
            btnAddConditionInventoryNbtTag.width = wrap ? fieldWidth : Math.max(72, fieldWidth - inputWidth - PARAM_INLINE_GAP);
            registerScrollableButton(btnAddConditionInventoryNbtTag,
                    getTagButtonBaseY(conditionInventoryNbtTagInputBaseY, fieldWidth));
        }
    }

    private void updateMoveChestCustomControlLayout() {
        if (!isMoveChestActionSelected() || moveChestItemNameInputField == null || moveChestNbtTagInputField == null) {
            return;
        }

        int fieldWidth = getMoveChestFieldWidth();
        int x = getParamContentX();
        int itemInputY = moveChestItemInputBaseY - paramScrollOffset;
        moveChestItemNameInputField.x = x;
        moveChestItemNameInputField.y = itemInputY;
        moveChestItemNameInputField.width = fieldWidth;
        moveChestItemNameInputField.height = 20;
        moveChestItemNameInputField.setVisible(itemInputY + 20 >= paramViewTop && itemInputY <= paramViewBottom);

        int inputY = moveChestTagInputBaseY - paramScrollOffset;
        moveChestNbtTagInputField.x = x;
        moveChestNbtTagInputField.y = inputY;
        moveChestNbtTagInputField.width = fieldWidth;
        moveChestNbtTagInputField.height = 20;
        moveChestNbtTagInputField.setVisible(inputY + 20 >= paramViewTop && inputY <= paramViewBottom);

        if (btnAddMoveChestNbtTag != null) {
            int buttonY = moveChestRuleButtonBaseY - paramScrollOffset;
            btnAddMoveChestNbtTag.x = x;
            btnAddMoveChestNbtTag.y = buttonY;
            btnAddMoveChestNbtTag.width = fieldWidth;
            btnAddMoveChestNbtTag.height = 20;
            btnAddMoveChestNbtTag.visible = buttonY + 20 >= paramViewTop && buttonY <= paramViewBottom;
            registerScrollableButton(btnAddMoveChestNbtTag, moveChestRuleButtonBaseY);
        }
    }

    private int getMoveChestFieldWidth() {
        return getParamFieldWidth();
    }

    private int getMoveChestChestRows() {
        return clampIntValue(getParamFieldIntValue("chestRows", 6), 1, 12);
    }

    boolean shouldWrapTagInputRow(int fieldWidth) {
        return fieldWidth < 240;
    }

    int getTagButtonBaseY(int inputBaseY, int fieldWidth) {
        return inputBaseY + (shouldWrapTagInputRow(fieldWidth) ? 28 : 0);
    }

    int getTagListOffset(int fieldWidth) {
        return shouldWrapTagInputRow(fieldWidth) ? 56 : 28;
    }

    private int getTagEditorAdvance(int fieldWidth) {
        return shouldWrapTagInputRow(fieldWidth) ? 68 : 40;
    }

    private int getConditionInventoryNbtTagListBaseY() {
        return conditionInventoryNbtTagInputBaseY < 0 ? -1
                : conditionInventoryNbtTagInputBaseY + getTagListOffset(getParamFieldWidth());
    }

    int getConditionInventoryNbtListHeight() {
        return 60;
    }

    private int getConditionInventoryCustomBottomBaseY() {
        if (!isConditionInventoryActionSelected() || conditionInventoryNbtTagInputBaseY < 0) {
            return 0;
        }
        return getConditionInventoryGridBaseY() + getConditionInventoryGridHeight() + 8;
    }

    private int getConditionInventoryRows() {
        return clampIntValue(getParamFieldIntValue("inventoryRows", 4), 1, 12);
    }

    private int getConditionInventoryCols() {
        return clampIntValue(getParamFieldIntValue("inventoryCols", 9), 1, 18);
    }

    private void pruneConditionInventorySelections() {
        pruneMoveChestSelectionSet(conditionInventorySelectedSlots,
                getConditionInventoryRows() * getConditionInventoryCols());
    }

    private int getConditionInventoryGridTitleBaseY() {
        int colsFieldIndex = paramFieldKeys.indexOf("inventoryCols");
        if (colsFieldIndex >= 0 && colsFieldIndex < paramFieldBaseY.size()) {
            return paramFieldBaseY.get(colsFieldIndex) + 40;
        }
        return getConditionInventoryNbtTagListBaseY() + getConditionInventoryNbtListHeight() + 18;
    }

    private int getConditionInventoryGridBaseY() {
        return getConditionInventoryGridTitleBaseY() + 14;
    }

    private int getConditionInventoryGridHeight() {
        return getMoveChestGridHeight(getConditionInventoryRows(), getConditionInventoryCols());
    }

    private int getMoveChestChestCols() {
        return clampIntValue(getParamFieldIntValue("chestCols", 9), 1, 18);
    }

    private int getMoveChestInventoryRows() {
        return clampIntValue(getParamFieldIntValue("inventoryRows", 4), 1, 12);
    }

    private int getMoveChestInventoryCols() {
        return clampIntValue(getParamFieldIntValue("inventoryCols", 9), 1, 18);
    }

    private void pruneMoveChestSelections() {
        pruneMoveChestSelectionSet(moveChestSelectedChestSlots, getMoveChestChestRows() * getMoveChestChestCols());
        pruneMoveChestSelectionSet(moveChestSelectedInventorySlots,
                getMoveChestInventoryRows() * getMoveChestInventoryCols());
    }

    private void pruneMoveChestSelectionSet(Set<Integer> values, int maxSlots) {
        if (values == null) {
            return;
        }
        values.removeIf(index -> index == null || index < 0 || index >= maxSlots);
    }

    private int clampIntValue(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int getMoveChestTagListBaseY() {
        return moveChestRuleButtonBaseY < 0 ? -1 : moveChestRuleButtonBaseY + 28;
    }

    private int getMoveChestTagListHeight() {
        return Math.max(1, moveChestFilterRules.size()) * 20;
    }

    private int getMoveChestGridCellSize(int columns) {
        int safeColumns = Math.max(1, columns);
        int gap = 2;
        int raw = (getMoveChestFieldWidth() - gap * (safeColumns - 1)) / safeColumns;
        return clampIntValue(raw, 8, 24);
    }

    private int getMoveChestGridHeight(int rows, int columns) {
        int safeRows = Math.max(1, rows);
        int gap = 2;
        int cellSize = getMoveChestGridCellSize(columns);
        return safeRows * cellSize + Math.max(0, safeRows - 1) * gap;
    }

    private int getMoveChestTargetGridTitleBaseY() {
        return getMoveChestTagListBaseY() + getMoveChestTagListHeight() + 18;
    }

    private int getMoveChestTargetGridBaseY() {
        return getMoveChestTargetGridTitleBaseY() + 14;
    }

    private int getMoveChestInventoryGridTitleBaseY() {
        return getMoveChestTargetGridBaseY()
                + getMoveChestGridHeight(getMoveChestChestRows(), getMoveChestChestCols())
                + 22;
    }

    private int getMoveChestInventoryGridBaseY() {
        return getMoveChestInventoryGridTitleBaseY() + 14;
    }

    private int getMoveChestCustomBottomBaseY() {
        if (!isMoveChestActionSelected() || moveChestTagInputBaseY < 0) {
            return 0;
        }
        return getMoveChestInventoryGridBaseY()
                + getMoveChestGridHeight(getMoveChestInventoryRows(), getMoveChestInventoryCols())
                + 8;
    }

    private void recomputeParamScrollBounds() {
        int contentBottom = getParamContentBottomBaseY();
        int viewHeight = Math.max(1, paramViewBottom - paramViewTop);
        maxParamScroll = Math.max(0, contentBottom - paramViewTop - viewHeight + 6);
    }

    private int getParamContentBottomBaseY() {
        int contentBottom = paramViewTop;

        for (int baseY : paramFieldBaseY) {
            contentBottom = Math.max(contentBottom, baseY + 34);
        }
        for (int baseY : paramDropdownBaseY) {
            contentBottom = Math.max(contentBottom, baseY + 34);
        }
        for (int baseY : scrollableButtonBaseY.values()) {
            contentBottom = Math.max(contentBottom, baseY + 20);
        }
        for (int baseY : toggleBaseY) {
            contentBottom = Math.max(contentBottom, baseY + 34);
        }
        if (nearbyEntityDropdownBaseY >= 0) {
            contentBottom = Math.max(contentBottom, nearbyEntityDropdownBaseY + 34);
        }
        if (nearbyBlockDropdownBaseY >= 0) {
            contentBottom = Math.max(contentBottom, nearbyBlockDropdownBaseY + 34);
        }
        if (systemMessageColorLabelBaseY > 0) {
            contentBottom = Math.max(contentBottom, systemMessageColorLabelBaseY + 16);
        }
        if (systemMessageFormatLabelBaseY > 0) {
            contentBottom = Math.max(contentBottom, systemMessageFormatLabelBaseY + 16);
        }
        if (runSequenceStatusLabelBaseY > 0) {
            contentBottom = Math.max(contentBottom, runSequenceStatusLabelBaseY + 24);
        }
        if (isConditionInventoryActionSelected()) {
            contentBottom = Math.max(contentBottom, getConditionInventoryCustomBottomBaseY());
        }
        if (isBooleanExpressionActionSelected()) {
            contentBottom = Math.max(contentBottom, getBooleanExpressionCustomBottomBaseY());
        }
        if (isMoveChestActionSelected()) {
            contentBottom = Math.max(contentBottom, getMoveChestCustomBottomBaseY());
        }

        return contentBottom;
    }

    private void updateScrollableControlPositions() {
        for (int i = 0; i < paramFields.size() && i < paramFieldBaseY.size(); i++) {
            paramFields.get(i).y = paramFieldBaseY.get(i) - paramScrollOffset;
        }
        for (int i = 0; i < paramDropdowns.size() && i < paramDropdownBaseY.size(); i++) {
            paramDropdowns.get(i).y = paramDropdownBaseY.get(i) - paramScrollOffset;
            paramDropdowns.get(i).setViewportBounds(paramViewTop, paramViewBottom);
        }
        for (GuiButton btn : this.buttonList) {
            Integer baseY = scrollableButtonBaseY.get(btn.id);
            if (baseY != null) {
                btn.y = baseY - paramScrollOffset;
                btn.visible = btn.y + btn.height >= paramViewTop && btn.y <= paramViewBottom;
            }
        }
        for (int i = 0; i < toggleButtons.size() && i < toggleBaseY.size(); i++) {
            ToggleOptionButton button = toggleButtons.get(i);
            button.y = toggleBaseY.get(i) - paramScrollOffset;
            button.visible = button.y + button.height >= paramViewTop && button.y <= paramViewBottom;
        }
        if (nearbyEntityDropdown != null && nearbyEntityDropdownBaseY >= 0) {
            nearbyEntityDropdown.y = nearbyEntityDropdownBaseY - paramScrollOffset;
            nearbyEntityDropdown.setViewportBounds(paramViewTop, paramViewBottom);
        }
        if (nearbyBlockDropdown != null && nearbyBlockDropdownBaseY >= 0) {
            nearbyBlockDropdown.y = nearbyBlockDropdownBaseY - paramScrollOffset;
            nearbyBlockDropdown.setViewportBounds(paramViewTop, paramViewBottom);
        }
        if (systemMessageColorLabelBaseY > 0) {
            systemMessageColorLabelY = systemMessageColorLabelBaseY - paramScrollOffset;
        }
        if (systemMessageFormatLabelBaseY > 0) {
            systemMessageFormatLabelY = systemMessageFormatLabelBaseY - paramScrollOffset;
        }
        if (runSequenceStatusLabelBaseY > 0) {
            runSequenceStatusLabelY = runSequenceStatusLabelBaseY - paramScrollOffset;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_ID_HEADER_TOOLBAR_PREV) {
            headerToolbarScrollIndex = Math.max(0, headerToolbarScrollIndex - 1);
            updateRightHeaderButtonLayout();
            return;
        }

        if (button.id == BTN_ID_HEADER_TOOLBAR_NEXT) {
            headerToolbarScrollIndex = Math.min(headerToolbarMaxScrollIndex, headerToolbarScrollIndex + 1);
            updateRightHeaderButtonLayout();
            return;
        }

        if (button.id == BTN_ID_FILL_NEAREST_CHEST) {
            int range = Math.max(1, (int) Math.round(getParamFieldDoubleValue("range", 10.0D)));
            BlockPos nearestChestPos = findNearestChestPos(range, Math.max(4, range / 2));
            if (nearestChestPos != null) {
                int posFieldIndex = getParamIndexByKey(availableActionTypes.get(selectedTypeIndex), "pos");
                if (posFieldIndex >= 0 && posFieldIndex < paramFields.size()) {
                    paramFields.get(posFieldIndex).setText(String.format("[%d,%d,%d]",
                            nearestChestPos.getX(),
                            nearestChestPos.getY(),
                            nearestChestPos.getZ()));
                    this.hasUnsavedChanges = true;
                    this.pendingSwitchActionType = null;
                }
            }
            return;
        }

        if (button.id == BTN_ID_SCAN_NEARBY_ENTITIES) {
            scanNearbyLivingEntitiesAndRefreshDropdown();
            return;
        }

        if (button.id == BTN_ID_SCAN_NEARBY_BLOCKS) {
            scanNearbyInteractableBlocksAndRefreshDropdown();
            return;
        }

        if (button.id == BTN_ID_SELECT_RUN_SEQUENCE) {
            if (!hasAvailableRunSequence()) {
                return;
            }
            mc.displayGuiScreen(new GuiSequenceSelector(this, seq -> {
                selectedRunSequenceName = seq;
                currentParams.addProperty("sequenceName", seq);
                mc.displayGuiScreen(this);
            }));
            return;
        }

        if (button.id == BTN_ID_SELECT_HUNT_ATTACK_SEQUENCE) {
            if (!hasAvailableRunSequence()) {
                return;
            }
            mc.displayGuiScreen(new GuiSequenceSelector(this, seq -> {
                selectedHuntAttackSequenceName = seq == null ? "" : seq.trim();
                if (selectedHuntAttackSequenceName.isEmpty()) {
                    currentParams.remove("attackSequenceName");
                } else {
                    currentParams.addProperty("attackSequenceName", selectedHuntAttackSequenceName);
                }
                mc.displayGuiScreen(this);
            }));
            return;
        }

        if (button.id == BTN_ID_SELECT_OTHER_FEATURE) {
            mc.displayGuiScreen(new GuiOtherFeatureSelector(this, selectedOtherFeatureId, feature -> {
                if (feature != null && feature.id != null) {
                    selectedOtherFeatureId = feature.id.trim();
                    currentParams.addProperty("featureId", selectedOtherFeatureId);
                    if (feature.name != null && !feature.name.trim().isEmpty()) {
                        currentParams.addProperty("featureName", feature.name.trim());
                    }
                }
                mc.displayGuiScreen(this);
            }));
            return;
        }

        if (button.id == BTN_ID_SELECT_CAPTURED_ID) {
            mc.displayGuiScreen(new GuiCapturedIdSelector(this, card -> {
                if (card != null && card.model != null && card.model.name != null) {
                    selectedCapturedIdName = card.model.name;
                    currentParams.addProperty("capturedId", selectedCapturedIdName);
                }
                mc.displayGuiScreen(this);
            }));
            return;
        }

        if (button.id == BTN_ID_ADD_MOVE_CHEST_NBT_TAG) {
            if (moveChestItemNameInputField != null && moveChestNbtTagInputField != null) {
                addMoveChestFilterRule(moveChestItemNameInputField.getText(), moveChestNbtTagInputField.getText());
                moveChestItemNameInputField.setText("");
                moveChestNbtTagInputField.setText("");
                this.hasUnsavedChanges = true;
                this.pendingSwitchActionType = null;
                refreshDynamicParamLayout();
            }
            return;
        }

        if (button.id == BTN_ID_ADD_CONDITION_INV_NBT_TAG) {
            if (conditionInventoryNbtTagInputField != null) {
                addConditionInventoryTag(conditionInventoryNbtTagInputField.getText());
                conditionInventoryNbtTagInputField.setText("");
                this.hasUnsavedChanges = true;
                this.pendingSwitchActionType = null;
                refreshDynamicParamLayout();
            }
            return;
        }

        if (BooleanExpressionEditorSupport.handleButtonAction(this, button)) {
            return;
        }

        if (button.id == BTN_ID_ADD_HUNT_WHITELIST) {
            appendSelectedNearbyEntityToFilterField("nameWhitelistText", "enableNameWhitelist");
            return;
        }

        if (button.id == BTN_ID_ADD_HUNT_BLACKLIST) {
            appendSelectedNearbyEntityToFilterField("nameBlacklistText", "enableNameBlacklist");
            return;
        }

        if (button.id == BTN_ID_FILL_FOLLOW_ENTITY_NAME) {
            fillFollowEntityNameFromScan();
            return;
        }

        if (button.id == BTN_ID_TOGGLE_SEQUENCE_BUILTIN_DELAY) {
            sequenceBuiltinDelayEnabledDraft = !sequenceBuiltinDelayEnabledDraft;
            refreshSequenceBuiltinDelayToolbarText();
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
            savePreferences();
            return;
        }

        if (button.id == BTN_ID_SEQUENCE_BUILTIN_DELAY_TICKS) {
            cycleSequenceBuiltinDelayTicks();
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
            savePreferences();
            return;
        }

        if (button.id >= BTN_ID_COLOR_BASE && button.id < BTN_ID_COLOR_BASE + SYSTEM_MESSAGE_COLOR_CODES.length) {
            int idx = button.id - BTN_ID_COLOR_BASE;
            if (idx >= 0 && idx < SYSTEM_MESSAGE_COLOR_CODES.length) {
                insertToSystemMessageField(SYSTEM_MESSAGE_COLOR_CODES[idx]);
            }
            return;
        }

        if (button.id >= BTN_ID_FORMAT_BASE && button.id < BTN_ID_FORMAT_BASE + SYSTEM_MESSAGE_FORMAT_CODES.length) {
            int idx = button.id - BTN_ID_FORMAT_BASE;
            if (idx >= 0 && idx < SYSTEM_MESSAGE_FORMAT_CODES.length) {
                insertToSystemMessageField(SYSTEM_MESSAGE_FORMAT_CODES[idx]);
            }
            return;
        }

        if (button.id >= 200 && button.id < 204) {
            String[] skills = { "R", "Z", "X", "C" };
            this.selectedSkill = skills[button.id - 200];
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
            return;
        }

        if (button.id >= BTN_ID_APPLY_HUNT_PRESET_BASE && button.id < BTN_ID_APPLY_HUNT_PRESET_BASE + 3) {
            applyHuntPreset(button.id - BTN_ID_APPLY_HUNT_PRESET_BASE);
            return;
        }

        if (button.id >= BTN_ID_APPLY_RUN_SEQUENCE_PRESET_BASE && button.id < BTN_ID_APPLY_RUN_SEQUENCE_PRESET_BASE + 3) {
            applyRunSequencePreset(button.id - BTN_ID_APPLY_RUN_SEQUENCE_PRESET_BASE);
            return;
        }

        if (button.id >= BTN_ID_TOGGLE_BASE && button.id < BTN_ID_TOGGLE_BASE + toggleButtons.size()) {
            int index = button.id - BTN_ID_TOGGLE_BASE;
            if (index >= 0 && index < toggleButtons.size()) {
                toggleButtons.get(index).toggle();
                this.hasUnsavedChanges = true;
                this.pendingSwitchActionType = null;
            }
            return;
        }

        if (button.id == BTN_ID_TOGGLE_SUMMARY_CARD) {
            boolean wasDocked = hasVisibleInfoCards();
            this.showSummaryInfoCard = !this.showSummaryInfoCard;
            updateInfoPanelVisibilityState(wasDocked);
            syncInfoPanelPreferences();
            savePreferences();
            return;
        }

        if (button.id == BTN_ID_TOGGLE_VALIDATION_CARD) {
            boolean wasDocked = hasVisibleInfoCards();
            this.showValidationInfoCard = !this.showValidationInfoCard;
            updateInfoPanelVisibilityState(wasDocked);
            syncInfoPanelPreferences();
            savePreferences();
            return;
        }

        if (button.id == BTN_ID_TOGGLE_HELP_CARD) {
            boolean wasDocked = hasVisibleInfoCards();
            this.showHelpInfoCard = !this.showHelpInfoCard;
            updateInfoPanelVisibilityState(wasDocked);
            syncInfoPanelPreferences();
            savePreferences();
            return;
        }

        if (button.id == BTN_ID_TEST_CURRENT_ACTION) {
            openCurrentActionTestReport();
            return;
        }

        if (button.id == 100) { // save
            int builtinDelayTicks = getSequenceBuiltinDelayTicksDraftValue();
            sequenceBuiltinDelayTicksDraft = String.valueOf(builtinDelayTicks);
            PathSequenceEventListener.updateBuiltinSequenceDelayConfig(sequenceBuiltinDelayEnabledDraft,
                    builtinDelayTicks);
            ActionData result = buildEditorActionData();
            List<PathConfigValidator.Issue> issues = buildDraftValidationIssues(result);
            List<PathConfigValidator.Issue> promptIssues = GuiPathValidationReport.filterIssuesForPrompt(issues);
            if (!promptIssues.isEmpty()) {
                mc.displayGuiScreen(new GuiPathValidationReport(this, "动作参数检查", "仍然保存", promptIssues, () -> {
                    onSave.accept(result);
                    this.hasUnsavedChanges = false;
                    mc.displayGuiScreen(parentScreen);
                }));
                return;
            }
            onSave.accept(result);
            this.hasUnsavedChanges = false;
            mc.displayGuiScreen(parentScreen);
        } else if (button.id == 101) { // cancel
            mc.displayGuiScreen(parentScreen);
        }
    }

    // drawScreen, drawActionList, mouseClicked, mouseClickMove, mouseReleased,
    // handleMouseInput, keyTyped
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = getEditorPanelWidth();
        int panelHeight = getEditorPanelHeight();
        int panelX = getEditorPanelX();
        int panelY = getEditorPanelY();
        int rightPaneX = getRightPaneX();
        int rightPaneY = getRightPaneY();
        int rightPaneWidth = getRightPaneWidth();
        int rightPaneHeight = getRightPaneHeight();
        int editorPaneX = getEditorPaneX();
        int editorPaneY = getEditorPaneY();
        int editorPaneWidth = getEditorPaneWidth();
        int editorPaneHeight = getEditorPaneHeight();
        refreshDynamicParamLayout();
        refreshLiveEditorFeedback();
        refreshDependencyControlStates();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "", this.fontRenderer);
        this.drawString(this.fontRenderer,
                isNew ? I18n.format("gui.path.action_editor.title_add")
                        : I18n.format("gui.path.action_editor.title_edit"),
                panelX + 12, panelY + 6, 0xFFF0F7FF);
        if (headerToolbarViewportBounds != null && headerToolbarViewportBounds.width > 0) {
            GuiTheme.drawButtonFrameSafe(headerToolbarViewportBounds.x - 3, headerToolbarViewportBounds.y,
                    headerToolbarViewportBounds.width + 6, headerToolbarViewportBounds.height,
                    GuiTheme.UiState.NORMAL);
        }

        GuiTheme.drawPanelSegment(getActionListX(), getActionListY(), actionListWidth, actionListHeight,
                panelX, panelY, panelWidth, panelHeight);
        if (hasVisibleInfoCards()) {
            GuiTheme.drawPanelSegment(editorPaneX, editorPaneY, editorPaneWidth, editorPaneHeight,
                    panelX, panelY, panelWidth, panelHeight);
        } else {
            GuiTheme.drawPanelSegment(rightPaneX, rightPaneY, rightPaneWidth, rightPaneHeight,
                    panelX, panelY, panelWidth, panelHeight);
        }
        drawActionPaneDivider(mouseX, mouseY);

        GuiTheme.drawSectionTitle(getActionListX() + 10, getActionListY() + 8,
                actionLibraryCollapsed ? "动作" : I18n.format("gui.path.action_editor.action_library"), this.fontRenderer);
        GuiTheme.drawSectionTitle((hasVisibleInfoCards() ? editorPaneX : rightPaneX) + 12,
                (hasVisibleInfoCards() ? editorPaneY : rightPaneY) + 8,
                I18n.format("gui.path.action_editor.label.edit_params"), this.fontRenderer);

        if (!actionLibraryCollapsed && actionSearchField.width > 0) {
            GuiTheme.drawInputFrameSafe(actionSearchField.x - 2, actionSearchField.y - 2, actionSearchField.width + 4,
                    actionSearchField.height + 4, actionSearchField.isFocused(), true);
            drawThemedTextField(actionSearchField);
            if (actionSearchField.getText().trim().isEmpty() && !actionSearchField.isFocused()) {
                this.drawString(this.fontRenderer, "§7搜索动作 / 分类", actionSearchField.x + 4, actionSearchField.y + 6,
                        0xFF7F8FA4);
            }
        }
        drawActionLibraryCollapseButton(mouseX, mouseY);

        GuiTheme.drawInputFrameSafe(editorPaneX + 8, paramViewTop - 6, Math.max(60, editorPaneWidth - 16),
                Math.max(44, paramViewBottom - paramViewTop + 14), false, true);

        drawActionList(mouseX, mouseY, getActionListX(), getActionListY());
        drawParamSectionCards(editorPaneX, editorPaneWidth);

        for (int i = 0; i < paramFields.size(); i++) {
            GuiTextField field = paramFields.get(i);
            if (field.y + 20 < paramViewTop || field.y > paramViewBottom) {
                continue;
            }
            String key = i < paramFieldKeys.size() ? paramFieldKeys.get(i) : "";
            if (isSectionTitleKey(key)) {
                continue;
            }
            String label = paramLabels.get(i);
            boolean enabled = isFieldDependencyEnabled(key);
            String dependencyHint = getFieldDependencyHint(key);
            String displayLabel = dependencyHint.isEmpty() ? label : label + " §8(" + dependencyHint + ")";
            this.drawString(fontRenderer, displayLabel, field.x, field.y - 12, enabled ? 0xFFDDDDDD : 0xFF9AA7B6);
            ExpressionEditorBinding expressionBinding = expressionEditorBindings.get(paramFieldKeys.get(i));
            if (expressionBinding != null) {
                drawExpressionEditorButton(field, mouseX, mouseY);
            } else {
                drawThemedTextField(field);
            }
        }

        for (int i = 0; i < paramDropdowns.size(); i++) {
            EnumDropdown dropdown = paramDropdowns.get(i);
            if (dropdown.y + 20 < paramViewTop || dropdown.y > paramViewBottom) {
                continue;
            }
            String key = i < paramDropdownKeys.size() ? paramDropdownKeys.get(i) : "";
            String label = paramDropdownLabels.get(i);
            boolean enabled = isDropdownDependencyEnabled(key);
            String dependencyHint = getDropdownDependencyHint(key);
            String displayLabel = dependencyHint.isEmpty() ? label : label + " §8(" + dependencyHint + ")";
            this.drawString(fontRenderer, displayLabel, dropdown.x, dropdown.y - 12, enabled ? 0xFFDDDDDD : 0xFF9AA7B6);
            dropdown.drawMain(mouseX, mouseY);
        }

        if (nearbyEntityDropdown != null) {
            if (nearbyEntityDropdown.y + 20 >= paramViewTop && nearbyEntityDropdown.y <= paramViewBottom) {
                this.drawString(fontRenderer, I18n.format("gui.path.action_editor.label.entity_candidates") + ":",
                        nearbyEntityDropdown.x, nearbyEntityDropdown.y - 12, 0xFFDDDDDD);
                nearbyEntityDropdown.drawMain(mouseX, mouseY);
            }
        }

        if (nearbyBlockDropdown != null) {
            if (nearbyBlockDropdown.y + 20 >= paramViewTop && nearbyBlockDropdown.y <= paramViewBottom) {
                this.drawString(fontRenderer, "方块候选:",
                        nearbyBlockDropdown.x, nearbyBlockDropdown.y - 12, 0xFFDDDDDD);
                nearbyBlockDropdown.drawMain(mouseX, mouseY);
            }
        }

        drawBooleanExpressionCustomSection(mouseX, mouseY);
        drawConditionInventoryCustomSection(mouseX, mouseY);
        drawMoveChestCustomSection(mouseX, mouseY);

        if ("use_skill".equalsIgnoreCase(availableActionTypes.get(selectedTypeIndex))) {
            int x = getParamContentX();
            int y = paramViewTop + 6;
            this.drawString(fontRenderer, I18n.format("gui.path.action_editor.select_skill"), x, y - 12, 0xFFDDDDDD);
            for (GuiButton btn : this.skillButtons) {
                if (btn.displayString.equals(this.selectedSkill)) {
                    btn.packedFGColour = 0xFFFFAA00;
                } else {
                    btn.packedFGColour = 0xFFE0E0E0;
                }
                if (btn.y + btn.height >= paramViewTop && btn.y <= paramViewBottom) {
                    btn.drawButton(mc, mouseX, mouseY, partialTicks);
                }
            }
        }

        if ("run_sequence".equalsIgnoreCase(availableActionTypes.get(selectedTypeIndex))
                && btnSelectRunSequence != null) {
            int x = btnSelectRunSequence.x;
            int y = btnSelectRunSequence.y;
            if (y + 20 >= paramViewTop && y <= paramViewBottom) {
                this.drawString(fontRenderer, I18n.format("gui.path.action_editor.label.run_sequence_name") + ":",
                        x, y - 12, 0xFFDDDDDD);
            }
            if (runSequenceStatusLabelBaseY > 0) {
                int statusX = getParamContentX();
                int statusY = runSequenceStatusLabelY;
                List<String> statusLines = this.fontRenderer.listFormattedStringToWidth(
                        PathSequenceManager.getRunSequenceActionStatusText(buildRunSequencePreviewParams()),
                        Math.max(120, getEditorPaneWidth() - 30));
                int statusBottom = statusY + Math.max(10, statusLines.size() * 10);
                if (statusBottom >= paramViewTop && statusY <= paramViewBottom) {
                    this.drawString(fontRenderer,
                            I18n.format("gui.path.action_editor.label.run_sequence_current_state") + ":",
                            statusX, statusY - 12, 0xFFDDDDDD);
                    drawWrappedText(statusLines, statusX, statusY, 0xFF9FDFFF);
                }
            }
        }

        if ("hunt".equalsIgnoreCase(availableActionTypes.get(selectedTypeIndex))
                && btnSelectHuntAttackSequence != null) {
            int x = btnSelectHuntAttackSequence.x;
            int y = btnSelectHuntAttackSequence.y;
            if (y + 20 >= paramViewTop && y <= paramViewBottom) {
                boolean enabled = isSequenceHuntAttackModeSelected();
                String label = enabled ? "攻击序列:" : "攻击序列: §8(需切到序列攻击)";
                this.drawString(fontRenderer, label, x, y - 12, enabled ? 0xFFDDDDDD : 0xFF9AA7B6);
            }
        }

        if ("toggle_other_feature".equalsIgnoreCase(availableActionTypes.get(selectedTypeIndex))
                && btnSelectOtherFeature != null) {
            int x = btnSelectOtherFeature.x;
            int y = btnSelectOtherFeature.y;
            if (y + 20 >= paramViewTop && y <= paramViewBottom) {
                this.drawString(fontRenderer, I18n.format("gui.path.action_editor.label.other_feature") + ":",
                        x, y - 12, 0xFFDDDDDD);
            }
        }

        if ("wait_until_captured_id".equalsIgnoreCase(availableActionTypes.get(selectedTypeIndex))
                && btnSelectCapturedId != null) {
            int x = btnSelectCapturedId.x;
            int y = btnSelectCapturedId.y;
            if (y + 20 >= paramViewTop && y <= paramViewBottom) {
                this.drawString(fontRenderer, "捕获ID:", x, y - 12, 0xFFDDDDDD);
            }
        }

        if ("system_message".equalsIgnoreCase(availableActionTypes.get(selectedTypeIndex))) {
            this.drawString(fontRenderer, I18n.format("gui.path.action_editor.label.color_shortcuts") + ":",
                    systemMessageColorLabelX, systemMessageColorLabelY, 0xFFDDDDDD);
            this.drawString(fontRenderer, I18n.format("gui.path.action_editor.label.format_shortcuts") + ":",
                    systemMessageFormatLabelX, systemMessageFormatLabelY, 0xFFDDDDDD);
        }

        if (paramFields.isEmpty() && paramDropdowns.isEmpty() && toggleButtons.isEmpty()
                && skillButtons.isEmpty() && btnSelectRunSequence == null
                && btnSelectOtherFeature == null) {
            GuiTheme.drawEmptyState(editorPaneX + editorPaneWidth / 2, paramViewTop + 18, "当前动作没有可编辑参数",
                    this.fontRenderer);
        }

        for (int i = 0; i < toggleButtons.size(); i++) {
            ToggleOptionButton button = toggleButtons.get(i);
            if (button.y + 20 < paramViewTop || button.y > paramViewBottom) {
                continue;
            }
            String key = i < toggleKeys.size() ? toggleKeys.get(i) : "";
            boolean enabled = isToggleDependencyEnabled(key);
            String dependencyHint = getToggleDependencyHint(key);
            String label = toggleLabels.get(i);
            String displayLabel = dependencyHint.isEmpty() ? label : label + " §8(" + dependencyHint + ")";
            this.drawString(fontRenderer, displayLabel, button.x, button.y - 12, enabled ? 0xFFDDDDDD : 0xFF9AA7B6);
        }

        if (maxParamScroll > 0) {
            int scrollX = editorPaneX + editorPaneWidth - 10;
            int viewHeight = Math.max(1, paramViewBottom - paramViewTop);
            int thumbHeight = Math.max(18, (int) ((float) viewHeight / (viewHeight + maxParamScroll) * viewHeight));
            int thumbY = paramViewTop;
            if (maxParamScroll > 0) {
                thumbY = paramViewTop
                        + (int) ((float) paramScrollOffset / maxParamScroll * (viewHeight - thumbHeight));
            }
            GuiTheme.drawScrollbar(scrollX, paramViewTop, 8, viewHeight, thumbY, thumbHeight);
        }

        if (pendingSwitchActionType != null) {
            String warning = "§e再次点击相同动作确认切换，当前未保存参数将丢失";
            this.drawCenteredString(this.fontRenderer, warning, panelX + panelWidth / 2, panelY + panelHeight - 42,
                    0xFFFFD26A);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (EnumDropdown dropdown : paramDropdowns) {
            if (dropdown.y <= paramViewBottom) {
                dropdown.drawExpanded(mouseX, mouseY);
            }
        }

        if (nearbyEntityDropdown != null
                && nearbyEntityDropdown.y + 20 >= paramViewTop
                && nearbyEntityDropdown.y <= paramViewBottom) {
            nearbyEntityDropdown.drawExpanded(mouseX, mouseY);
        }

        if (nearbyBlockDropdown != null
                && nearbyBlockDropdown.y + 20 >= paramViewTop
                && nearbyBlockDropdown.y <= paramViewBottom) {
            nearbyBlockDropdown.drawExpanded(mouseX, mouseY);
        }

        if (hasActiveInfoSections()) {
            drawInfoPopupWindow(mouseX, mouseY);
        }

        if (isExpressionEditorPopupOpen()) {
            drawExpressionEditorPopup(mouseX, mouseY);
        }
    }

    private void drawBooleanExpressionCustomSection(int mouseX, int mouseY) {
        BooleanExpressionEditorSupport.drawCustomSection(this, mouseX, mouseY);
    }

    private void drawConditionInventoryCustomSection(int mouseX, int mouseY) {
        conditionInventoryTagRemoveRegions.clear();
        conditionInventorySlotRegions.clear();

        if (!isConditionInventoryActionSelected() || conditionInventoryNbtTagInputField == null) {
            return;
        }

        pruneConditionInventorySelections();

        int x = getParamContentX();
        int fieldWidth = getParamFieldWidth();

        if (conditionInventoryNbtTagInputField.getVisible()) {
            this.drawString(fontRenderer, I18n.format("gui.path.action_editor.label.required_nbt_tags") + ":",
                    conditionInventoryNbtTagInputField.x, conditionInventoryNbtTagInputField.y - 12, 0xFFDDDDDD);
            drawThemedTextField(conditionInventoryNbtTagInputField);
            if (conditionInventoryNbtTagInputField.getText().trim().isEmpty()
                    && !conditionInventoryNbtTagInputField.isFocused()) {
                this.drawString(fontRenderer, "§7输入要匹配或排除的 NBT 关键字", conditionInventoryNbtTagInputField.x + 4,
                        conditionInventoryNbtTagInputField.y + 6, 0xFF7F8FA4);
            }
        }

        int listY = getConditionInventoryNbtTagListBaseY() - paramScrollOffset;
        int listHeight = getConditionInventoryNbtListHeight();
        if (listY + listHeight >= paramViewTop && listY <= paramViewBottom) {
            GuiTheme.drawInputFrameSafe(x, listY, fieldWidth, listHeight, false, true);

            if (conditionInventoryRequiredNbtTags.isEmpty()) {
                this.drawString(fontRenderer, "§7留空时不按 NBT 过滤；可只填 NBT，不填物品名", x + 6, listY + 6,
                        0xFF9FB0C4);
            } else {
                int rowHeight = 18;
                int visibleRows = Math.max(1, (listHeight - 4) / rowHeight);
                int maxScroll = Math.max(0, conditionInventoryRequiredNbtTags.size() - visibleRows);
                conditionInventoryNbtTagScrollOffset = MathHelper.clamp(conditionInventoryNbtTagScrollOffset, 0,
                        maxScroll);

                for (int i = 0; i < visibleRows; i++) {
                    int tagIndex = i + conditionInventoryNbtTagScrollOffset;
                    if (tagIndex >= conditionInventoryRequiredNbtTags.size()) {
                        break;
                    }
                    int rowY = listY + 2 + i * rowHeight;
                    boolean hovered = isPointInside(mouseX, mouseY, x + 2, rowY, fieldWidth - 8, rowHeight - 1);
                    int bg = hovered ? 0xAA2E4258 : 0x77222222;
                    int border = hovered ? 0xFF7EC8FF : 0xFF4B4B4B;
                    drawRect(x + 2, rowY, x + fieldWidth - 8, rowY + rowHeight - 1, bg);
                    drawHorizontalLine(x + 2, x + fieldWidth - 8, rowY, border);
                    drawHorizontalLine(x + 2, x + fieldWidth - 8, rowY + rowHeight - 1, border);
                    this.drawString(fontRenderer,
                            this.fontRenderer.trimStringToWidth(conditionInventoryRequiredNbtTags.get(tagIndex),
                                    fieldWidth - 30),
                            x + 6, rowY + 5, 0xFFFFFFFF);
                    int removeX = x + fieldWidth - 20;
                    this.drawString(fontRenderer, "§c✕", removeX, rowY + 5, 0xFFFF8080);
                    conditionInventoryTagRemoveRegions
                            .add(new IndexedHitRegion(removeX - 2, rowY + 1, 16, rowHeight - 2,
                                    tagIndex));
                }

                if (maxScroll > 0) {
                    int thumbHeight = Math.max(12,
                            (int) ((visibleRows / (float) conditionInventoryRequiredNbtTags.size())
                                    * (listHeight - 4)));
                    int trackHeight = Math.max(1, (listHeight - 4) - thumbHeight);
                    int thumbY = listY + 2
                            + (int) ((conditionInventoryNbtTagScrollOffset / (float) maxScroll) * trackHeight);
                    GuiTheme.drawScrollbar(x + fieldWidth - 6, listY + 2, 4, listHeight - 4, thumbY, thumbHeight);
                }
            }
        }

        int gridTitleY = getConditionInventoryGridTitleBaseY() - paramScrollOffset;
        if (gridTitleY + 10 >= paramViewTop && gridTitleY <= paramViewBottom) {
            String countLabel = conditionInventorySelectedSlots.isEmpty()
                    ? "§7(未选时默认检查整个背包)"
                    : "§7(" + conditionInventorySelectedSlots.size() + ")";
            this.drawString(fontRenderer, "指定检查槽位 " + countLabel, x, gridTitleY, 0xFFDDDDDD);
        }

        drawConditionInventorySlotGrid(mouseX, mouseY, getConditionInventoryGridBaseY(),
                getConditionInventoryRows(), getConditionInventoryCols());
    }

    private void drawConditionInventorySlotGrid(int mouseX, int mouseY, int baseY, int rows, int cols) {
        drawSelectionSlotGrid(mouseX, mouseY, baseY, rows, cols, conditionInventorySelectedSlots,
                conditionInventorySlotRegions);
    }

    private void drawMoveChestCustomSection(int mouseX, int mouseY) {
        moveChestTagRemoveRegions.clear();
        moveChestTargetSlotRegions.clear();
        moveChestInventorySlotRegions.clear();

        if (!isMoveChestActionSelected() || moveChestItemNameInputField == null || moveChestNbtTagInputField == null) {
            return;
        }

        pruneMoveChestSelections();

        int x = getParamContentX();
        int fieldWidth = getMoveChestFieldWidth();

        if (moveChestItemNameInputField.getVisible()) {
            this.drawString(fontRenderer, I18n.format("gui.path.action_editor.label.item_name") + ":",
                    moveChestItemNameInputField.x, moveChestItemNameInputField.y - 12, 0xFFDDDDDD);
            drawThemedTextField(moveChestItemNameInputField);
            if (moveChestItemNameInputField.getText().trim().isEmpty() && !moveChestItemNameInputField.isFocused()) {
                this.drawString(fontRenderer,
                        I18n.format("gui.path.action_editor.placeholder.move_chest_item_name"),
                        moveChestItemNameInputField.x + 4, moveChestItemNameInputField.y + 6, 0xFF7F8FA4);
            }
        }

        if (moveChestNbtTagInputField.getVisible()) {
            this.drawString(fontRenderer, I18n.format("gui.path.action_editor.label.required_nbt_tags") + ":",
                    moveChestNbtTagInputField.x, moveChestNbtTagInputField.y - 12, 0xFFDDDDDD);
            drawThemedTextField(moveChestNbtTagInputField);
            if (moveChestNbtTagInputField.getText().trim().isEmpty() && !moveChestNbtTagInputField.isFocused()) {
                this.drawString(fontRenderer,
                        I18n.format("gui.path.action_editor.placeholder.move_chest_required_nbt_tags"),
                        moveChestNbtTagInputField.x + 4,
                        moveChestNbtTagInputField.y + 6, 0xFF7F8FA4);
            }
        }

        int tagListY = getMoveChestTagListBaseY() - paramScrollOffset;
        int tagRowHeight = 18;
        if (tagListY + tagRowHeight >= paramViewTop && tagListY <= paramViewBottom) {
            if (moveChestFilterRules.isEmpty()) {
                String itemValue = moveChestItemNameInputField.getText().trim();
                String nbtValue = moveChestNbtTagInputField.getText().trim();
                String tipText;
                int tipColor;
                if (itemValue.isEmpty() && nbtValue.isEmpty()) {
                    tipText = "§6至少添加一条规则；每条规则可填物品名、NBT 或两者同时填写";
                    tipColor = 0xFFE4C46A;
                } else if (!itemValue.isEmpty() && !nbtValue.isEmpty()) {
                    tipText = "§a当前待添加规则将同时验证物品名和 NBT";
                    tipColor = 0xFF9FE0B1;
                } else if (!itemValue.isEmpty()) {
                    tipText = "§7当前待添加规则仅按物品名匹配";
                    tipColor = 0xFF9FB0C0;
                } else {
                    tipText = "§7当前待添加规则仅按 NBT 匹配";
                    tipColor = 0xFF9FB0C0;
                }
                GuiTheme.drawInputFrameSafe(x, tagListY, fieldWidth, tagRowHeight, false, true);
                this.drawString(fontRenderer, tipText, x + 6, tagListY + 5, tipColor);
            } else {
                for (int i = 0; i < moveChestFilterRules.size(); i++) {
                    int rowY = tagListY + i * 20;
                    if (rowY + tagRowHeight < paramViewTop || rowY > paramViewBottom) {
                        continue;
                    }
                    MoveChestFilterRuleDraft rule = moveChestFilterRules.get(i);
                    String ruleText = (rule.itemName.isEmpty() ? "任意名称" : ("物品:" + rule.itemName))
                            + " | "
                            + (rule.nbtText.isEmpty() ? "任意NBT" : ("NBT:" + rule.nbtText));
                    GuiTheme.drawInputFrameSafe(x, rowY, fieldWidth, tagRowHeight, false, true);
                    this.drawString(fontRenderer, this.fontRenderer.trimStringToWidth(ruleText, fieldWidth - 30), x + 6,
                            rowY + 5, 0xFFEAF7FF);
                    int removeX = x + fieldWidth - 16;
                    this.drawString(fontRenderer, "§cX", removeX, rowY + 5, 0xFFFF6666);
                    moveChestTagRemoveRegions.add(new IndexedHitRegion(removeX - 2, rowY + 1, 16, tagRowHeight - 2, i));
                }
            }
        }

        boolean chestToInventory = ItemFilterHandler.MOVE_DIRECTION_CHEST_TO_INVENTORY
                .equalsIgnoreCase(getDraftMoveChestDirection());

        int chestTitleY = getMoveChestTargetGridTitleBaseY() - paramScrollOffset;
        if (chestTitleY + 10 >= paramViewTop && chestTitleY <= paramViewBottom) {
            this.drawString(fontRenderer,
                    I18n.format(chestToInventory
                            ? "gui.path.action_editor.label.source_chest_slots"
                            : "gui.path.action_editor.label.target_chest_slots")
                            + " §7(" + moveChestSelectedChestSlots.size() + ")",
                    x, chestTitleY, 0xFFDDDDDD);
        }
        drawMoveChestSlotGrid(mouseX, mouseY, getMoveChestTargetGridBaseY(),
                getMoveChestChestRows(), getMoveChestChestCols(), moveChestSelectedChestSlots,
                moveChestTargetSlotRegions);

        int inventoryTitleY = getMoveChestInventoryGridTitleBaseY() - paramScrollOffset;
        if (inventoryTitleY + 10 >= paramViewTop && inventoryTitleY <= paramViewBottom) {
            this.drawString(fontRenderer,
                    I18n.format(chestToInventory
                            ? "gui.path.action_editor.label.target_inventory_slots"
                            : "gui.path.action_editor.label.source_inventory_slots")
                            + " §7(" + moveChestSelectedInventorySlots.size() + ")",
                    x, inventoryTitleY, 0xFFDDDDDD);
        }
        drawMoveChestSlotGrid(mouseX, mouseY, getMoveChestInventoryGridBaseY(),
                getMoveChestInventoryRows(), getMoveChestInventoryCols(), moveChestSelectedInventorySlots,
                moveChestInventorySlotRegions);
    }

    private void drawMoveChestSlotGrid(int mouseX, int mouseY, int baseY, int rows, int cols,
            Set<Integer> selectedSlots, List<IndexedHitRegion> regions) {
        drawSelectionSlotGrid(mouseX, mouseY, baseY, rows, cols, selectedSlots, regions);
    }

    private void drawSelectionSlotGrid(int mouseX, int mouseY, int baseY, int rows, int cols,
            Set<Integer> selectedSlots, List<IndexedHitRegion> regions) {
        int gap = 2;
        int cellSize = getMoveChestGridCellSize(cols);
        int gridWidth = cols * cellSize + Math.max(0, cols - 1) * gap;
        int drawX = getParamContentX() + Math.max(0, (getMoveChestFieldWidth() - gridWidth) / 2);
        int drawY = baseY - paramScrollOffset;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                int cellX = drawX + col * (cellSize + gap);
                int cellY = drawY + row * (cellSize + gap);
                regions.add(new IndexedHitRegion(cellX, cellY, cellSize, cellSize, index));
                if (cellY + cellSize < paramViewTop || cellY > paramViewBottom) {
                    continue;
                }

                boolean selected = selectedSlots.contains(index);
                boolean hovered = mouseX >= cellX && mouseX <= cellX + cellSize
                        && mouseY >= cellY && mouseY <= cellY + cellSize;
                int bg = selected ? (hovered ? 0xFF4B8BB2 : 0xFF2F6F95) : (hovered ? 0xFF31465C : 0xFF1B2D3D);
                int border = selected ? 0xFF9FDFFF : 0xFF3F6A8C;

                drawRect(cellX, cellY, cellX + cellSize, cellY + cellSize, bg);
                drawHorizontalLine(cellX, cellX + cellSize, cellY, border);
                drawHorizontalLine(cellX, cellX + cellSize, cellY + cellSize, border);
                drawVerticalLine(cellX, cellY, cellY + cellSize, border);
                drawVerticalLine(cellX + cellSize, cellY, cellY + cellSize, border);

                String label = String.valueOf(index);
                int textWidth = this.fontRenderer.getStringWidth(label);
                int textX = cellX + (cellSize - textWidth) / 2;
                int textY = cellY + Math.max(3, (cellSize - 8) / 2);
                this.drawString(fontRenderer, label, textX, textY, 0xFFEAF7FF);
            }
        }
    }

    private void drawActionList(int mouseX, int mouseY, int x, int y) {
        if (actionLibraryCollapsed) {
            int hintY = getActionListTop() + 8;
            GuiTheme.drawInputFrameSafe(x + 6, getActionListTop() - 4, actionListWidth - 12,
                    getActionListContentHeight() + 8, false, true);
            this.drawCenteredString(this.fontRenderer, "展开", x + actionListWidth / 2, hintY, 0xFF9FDFFF);
            this.drawCenteredString(this.fontRenderer, "动作库", x + actionListWidth / 2, hintY + 14, 0xFF7F8FA4);
            return;
        }
        int listTop = getActionListTop();
        int listContentHeight = getActionListContentHeight();
        int visibleRowCount = Math.max(1, listContentHeight / ACTION_LIBRARY_ROW_HEIGHT);
        String selectedType = getSelectedActionType();
        String filter = getActionSearchText();

        GuiTheme.drawInputFrameSafe(x + 8, listTop - 4, actionListWidth - 16, listContentHeight + 8, false, true);

        for (int i = 0; i < visibleRowCount; i++) {
            int rowIndex = i + actionListScrollOffset;
            if (rowIndex >= visibleActionLibraryRows.size()) {
                break;
            }

            ActionLibraryVisibleRow row = visibleActionLibraryRows.get(rowIndex);
            int itemY = listTop + i * ACTION_LIBRARY_ROW_HEIGHT;
            boolean isHovered = mouseX >= x && mouseX <= x + actionListWidth - 8 && mouseY >= itemY
                    && mouseY <= itemY + ACTION_LIBRARY_ROW_HEIGHT;
            boolean isSelected = !row.node.isGroup()
                    && row.node.actionType != null
                    && row.node.actionType.equalsIgnoreCase(selectedType);
            ActionLibraryViewSupport.MatchKind matchKind = ActionLibraryViewSupport.getMatchKind(row.node, filter);

            GuiTheme.UiState state = isSelected ? GuiTheme.UiState.SELECTED
                    : (isHovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            GuiTheme.drawButtonFrameSafe(x + 10, itemY, actionListWidth - 24, ACTION_LIBRARY_ROW_HEIGHT - 1, state);

            int indent = row.depth * ACTION_LIBRARY_INDENT;
            int textX = x + 15 + indent;
            int rightEdge = x + actionListWidth - 20;
            if (!row.node.isGroup()) {
                rightEdge -= 12;
            }
            String matchBadge = filter == null || filter.isEmpty() ? ""
                    : ActionLibraryViewSupport.getMatchBadge(row.node, filter);
            if (!matchBadge.isEmpty()) {
                rightEdge -= Math.max(22, this.fontRenderer.getStringWidth(matchBadge) + 10);
            }
            int textWidth = Math.max(10, rightEdge - textX);
            String prefix = row.node.isGroup()
                    ? (isActionLibraryGroupExpanded(row.node.id) ? "▼ " : "▶ ")
                    : "• ";
            String text = fontRenderer.trimStringToWidth(prefix + row.node.label, textWidth);
            int color = ActionLibraryViewSupport.getMatchColor(matchKind, row.node.isGroup());
            this.drawString(fontRenderer, text, textX, itemY + 6, color);

            int drawRightX = x + actionListWidth - 22;
            if (!row.node.isGroup()) {
                boolean favorite = isFavoriteActionType(row.node.actionType);
                this.drawString(fontRenderer, favorite ? "★" : "☆", drawRightX, itemY + 6,
                        favorite ? 0xFFFFD36B : 0xFF6F8296);
                drawRightX -= 16;
            }
            if (!matchBadge.isEmpty()) {
                int badgeWidth = Math.max(22, this.fontRenderer.getStringWidth(matchBadge) + 8);
                int badgeX = drawRightX - badgeWidth + 8;
                drawRect(badgeX, itemY + 4, badgeX + badgeWidth, itemY + ACTION_LIBRARY_ROW_HEIGHT - 5,
                        matchKind == ActionLibraryViewSupport.MatchKind.PINYIN ? 0x55348DB0
                                : (matchKind == ActionLibraryViewSupport.MatchKind.ACTION_TYPE ? 0x556E5A1E : 0x55405C76));
                this.drawString(fontRenderer, matchBadge, badgeX + 4, itemY + 6,
                        matchKind == ActionLibraryViewSupport.MatchKind.PINYIN ? 0xFFC7F1FF
                                : (matchKind == ActionLibraryViewSupport.MatchKind.ACTION_TYPE ? 0xFFFFE4A8 : 0xFFE6EEF8));
            }
        }

        if (visibleActionLibraryRows.isEmpty()) {
            GuiTheme.drawEmptyState(x + actionListWidth / 2, listTop + 12,
                    I18n.format("gui.path.action_editor.entity_dropdown_empty"), this.fontRenderer);
        }

        if (!actionLibraryCollapsed && mouseX >= x && mouseX <= x + actionListWidth - 8
                && mouseY >= listTop && mouseY <= listTop + listContentHeight) {
            ActionLibraryVisibleRow hoveredRow = hitTestActionLibraryRow(mouseX, mouseY);
            if (hoveredRow != null && hoveredRow.node != null && !hoveredRow.node.isGroup()) {
                this.drawString(this.fontRenderer, "§8左键切换  |  右键收藏", x + 14, listTop + listContentHeight - 12,
                        0xFF7F8FA4);
            }
        }

        if (maxActionListScroll > 0 && !visibleActionLibraryRows.isEmpty()) {
            int scrollbarX = x + actionListWidth - 10;
            int scrollbarY = listTop;
            int scrollbarHeight = listContentHeight;

            int thumbHeight = Math.max(10,
                    (int) ((float) visibleRowCount / visibleActionLibraryRows.size() * scrollbarHeight));
            int thumbY = scrollbarY
                    + (int) ((float) actionListScrollOffset / maxActionListScroll * (scrollbarHeight - thumbHeight));
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 8, scrollbarHeight, thumbY, thumbHeight);
        }
    }

    private void drawActionPaneDivider(int mouseX, int mouseY) {
        boolean hovered = isHoverRegion(mouseX, mouseY, actionDividerX - 2, actionDividerY,
                ACTION_PANE_DIVIDER_WIDTH + 4, actionDividerHeight);
        int accent = draggingActionDivider ? 0xFF7CD9FF : (hovered ? 0xFF63BFEF : 0xFF3E617A);
        drawRect(actionDividerX, actionDividerY, actionDividerX + ACTION_PANE_DIVIDER_WIDTH,
                actionDividerY + actionDividerHeight, 0x77111922);
        drawRect(actionDividerX + 5, actionDividerY + 18, actionDividerX + 7, actionDividerY + actionDividerHeight - 18,
                accent);
        int centerY = actionDividerY + actionDividerHeight / 2 - 12;
        for (int i = 0; i < 4; i++) {
            drawRect(actionDividerX + 3, centerY + i * 7, actionDividerX + ACTION_PANE_DIVIDER_WIDTH - 3,
                    centerY + i * 7 + 2, accent);
        }
    }

    private void drawActionLibraryCollapseButton(int mouseX, int mouseY) {
        boolean hovered = isPointInsideActionCollapseButton(mouseX, mouseY);
        GuiTheme.UiState state = hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
        GuiTheme.drawButtonFrameSafe(actionCollapseButtonX, actionCollapseButtonY, actionCollapseButtonWidth,
                actionCollapseButtonHeight, state);
        String arrow = actionLibraryCollapsed ? ">" : "<";
        drawCenteredString(this.fontRenderer, arrow, actionCollapseButtonX + actionCollapseButtonWidth / 2,
                actionCollapseButtonY + 5, 0xFFEAF7FF);
    }

    private void applyActionDividerDrag(int mouseX) {
        int preferred = mouseX - getActionListX() - ACTION_PANE_DIVIDER_WIDTH / 2;
        this.actionListPreferredWidth = clampActionListPreferredWidth(preferred);
        savedActionListPreferredWidth = this.actionListPreferredWidth;
        captureCurrentEditorDraftParams();
        int oldScroll = this.paramScrollOffset;
        boolean dirty = this.hasUnsavedChanges;
        String pending = this.pendingSwitchActionType;
        initGui();
        this.paramScrollOffset = Math.max(0, Math.min(oldScroll, maxParamScroll));
        refreshDynamicParamLayout();
        this.hasUnsavedChanges = dirty;
        this.pendingSwitchActionType = pending;
        savePreferences();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isExpressionEditorPopupOpen()) {
            handleExpressionEditorPopupClick(mouseX, mouseY, mouseButton);
            return;
        }
        if (handleInfoPopupClick(mouseX, mouseY, mouseButton)) {
            return;
        }

        // 关键修复：保存/取消按钮优先于下拉框点击处理，
        // 避免当下拉展开覆盖到底部区域时，误触发下拉而不是按钮。
        if (mouseButton == 0) {
            for (GuiButton btn : this.buttonList) {
                if ((btn.id == 100 || btn.id == 101 || btn.id == BTN_ID_TEST_CURRENT_ACTION
                        || btn.id == BTN_ID_TOGGLE_SEQUENCE_BUILTIN_DELAY)
                        && btn.visible && btn.enabled
                        && btn.mousePressed(this.mc, mouseX, mouseY)) {
                    collapseAllDropdowns();
                    super.mouseClicked(mouseX, mouseY, mouseButton);
                    return;
                }
            }
        }

        if (mouseButton == 0 && isPointInsideActionCollapseButton(mouseX, mouseY)) {
            boolean dirty = this.hasUnsavedChanges;
            String pending = this.pendingSwitchActionType;
            actionLibraryCollapsed = !actionLibraryCollapsed;
            savedActionLibraryCollapsed = actionLibraryCollapsed;
            captureCurrentEditorDraftParams();
            initGui();
            this.hasUnsavedChanges = dirty;
            this.pendingSwitchActionType = pending;
            savePreferences();
            return;
        }

        if (!actionLibraryCollapsed
                && mouseButton == 0
                && isHoverRegion(mouseX, mouseY, actionDividerX - 2, actionDividerY,
                        ACTION_PANE_DIVIDER_WIDTH + 4, actionDividerHeight)) {
            draggingActionDivider = true;
            return;
        }

        if (handleExpandedDropdownClick(mouseX, mouseY, mouseButton)) {
            return;
        }

        boolean dropdownHandled = false;
        boolean refreshDynamicLocatorLayout = false;
        boolean refreshConditionExpressionLayout = false;
        for (int i = paramDropdowns.size() - 1; i >= 0; i--) {
            EnumDropdown dropdown = paramDropdowns.get(i);
            String oldValue = dropdown.getValue();
            if (dropdown.handleClick(mouseX, mouseY, mouseButton)) {
                collapseOtherDropdowns(dropdown);
                dropdownHandled = true;
                refreshDynamicLocatorLayout = shouldRefreshDynamicLocatorLayout(i, oldValue, dropdown.getValue());
                String dropdownKey = paramDropdownKeys.get(i);
                onVariableSelectorDropdownChanged(dropdownKey, oldValue, dropdown.getValue());
                refreshConditionExpressionLayout = shouldRefreshConditionExpressionLayout(dropdownKey, oldValue,
                        dropdown.getValue());
                break;
            }
        }
        if (!dropdownHandled && nearbyEntityDropdown != null
                && nearbyEntityDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            collapseParamDropdowns();
            if (nearbyBlockDropdown != null) {
                nearbyBlockDropdown.collapse();
            }
            dropdownHandled = true;
            syncNearbyEntitySelectionToPosField();
        }
        if (!dropdownHandled && nearbyBlockDropdown != null
                && nearbyBlockDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            collapseParamDropdowns();
            if (nearbyEntityDropdown != null) {
                nearbyEntityDropdown.collapse();
            }
            dropdownHandled = true;
            syncNearbyBlockSelectionToPosField();
        }
        if (dropdownHandled) {
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
            if (refreshDynamicLocatorLayout || refreshConditionExpressionLayout) {
                captureCurrentEditorDraftParams();
                this.paramScrollOffset = 0;
                initGui();
            }
            return;
        }

        if (mouseButton == 0 && handleConditionInventoryCustomClick(mouseX, mouseY)) {
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
            refreshDynamicParamLayout();
            return;
        }

        if (mouseButton == 0 && handleBooleanExpressionCustomClick(mouseX, mouseY)) {
            refreshDynamicParamLayout();
            return;
        }

        if (mouseButton == 0 && handleMoveChestCustomClick(mouseX, mouseY)) {
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
            refreshDynamicParamLayout();
            return;
        }

        if (mouseButton == 0) {
            ExpressionEditorBinding expressionBinding = getExpressionEditorBindingAt(mouseX, mouseY);
            if (expressionBinding != null) {
                openExpressionEditorPopup(expressionBinding);
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        updateScrollableControlPositions();
        if (!actionLibraryCollapsed && actionSearchField != null) {
            actionSearchField.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (actionSearchField != null) {
            actionSearchField.setFocused(false);
        }
        for (int i = 0; i < paramFields.size(); i++) {
            if (isExpressionEditorFieldKey(paramFieldKeys.get(i))) {
                paramFields.get(i).setFocused(false);
                continue;
            }
            paramFields.get(i).mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (conditionInventoryNbtTagInputField != null) {
            conditionInventoryNbtTagInputField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (moveChestItemNameInputField != null) {
            moveChestItemNameInputField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (moveChestNbtTagInputField != null) {
            moveChestNbtTagInputField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        collapseAllDropdowns();

        if (!actionLibraryCollapsed) {
            int listX = getActionListX();
            int listTop = getActionListTop();
            int listWidth = actionListWidth;
            int listContentHeight = getActionListContentHeight();

            int scrollbarX = listX + listWidth - 10;
            if (mouseButton == 0 && mouseX >= scrollbarX && mouseX < scrollbarX + 8 && mouseY >= listTop
                    && mouseY < listTop + listContentHeight) {
                isDraggingActionScrollbar = true;
                scrollClickY = mouseY;
                initialScrollOffset = actionListScrollOffset;
                return;
            }
        }

        // 参数面板滚动条拖动处理
        if (maxParamScroll > 0 && mouseButton == 0) {
            int paramScrollbarX = getEditorPaneX() + getEditorPaneWidth() - 10;
            if (mouseX >= paramScrollbarX && mouseX < paramScrollbarX + 8
                    && mouseY >= paramViewTop && mouseY < paramViewBottom) {
                isDraggingParamScrollbar = true;
                scrollClickY = mouseY;
                initialScrollOffset = paramScrollOffset;
                return;
            }
        }

        if (!actionLibraryCollapsed && (mouseButton == 0 || mouseButton == 1)) {
            ActionLibraryVisibleRow row = hitTestActionLibraryRow(mouseX, mouseY);
            if (row != null) {
                if (mouseButton == 0) {
                    onActionLibraryRowLeftClick(row);
                } else if (!row.node.isGroup()) {
                    toggleFavoriteActionType(row.node.actionType);
                    refreshEditorAfterActionLibraryPreferenceChange();
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        if (isExpressionEditorPopupOpen()) {
            if (clickedMouseButton == 0 && isDraggingExpressionPopupScrollbar) {
                List<ExpressionTemplateLayoutEntry> entries = buildExpressionTemplateLayoutEntries(
                        getFilteredExpressionTemplateCards(expressionPopupSearchField == null ? ""
                                : expressionPopupSearchField.getText()));
                int contentHeight = computeExpressionTemplateContentHeight(entries);
                expressionPopupMaxScroll = Math.max(0, contentHeight - Math.max(1, getExpressionPopupViewportHeight() - 8));
                updateExpressionPopupScrollFromMouse(mouseY, contentHeight);
            }
            return;
        }
        if (clickedMouseButton == 0 && (draggingInfoPopup || draggingInfoDockDivider
                || resizingInfoPopupRight || resizingInfoPopupBottom || resizingInfoPopupCorner)) {
            handleInfoPopupDrag(mouseX, mouseY);
            return;
        }

        if (clickedMouseButton == 0 && draggingActionDivider) {
            applyActionDividerDrag(mouseX);
            return;
        }
        if (clickedMouseButton == 0 && moveChestGridDragging) {
            updateMoveChestGridDrag(mouseX, mouseY);
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
        }
        if (clickedMouseButton == 0 && conditionInventoryGridDragging) {
            updateConditionInventoryGridDrag(mouseX, mouseY);
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
        }

        if (isDraggingActionScrollbar) {
            int listTop = getActionListTop();
            int listContentHeight = getActionListContentHeight();

            int visibleActionRows = Math.max(1, listContentHeight / ACTION_LIBRARY_ROW_HEIGHT);
            int totalRows = visibleActionLibraryRows.size();
            if (totalRows <= 0) {
                return;
            }

            int thumbHeight = Math.max(10, (int) ((float) visibleActionRows / totalRows * listContentHeight));
            int scrollableTrackHeight = listContentHeight - thumbHeight;

            if (scrollableTrackHeight > 0) {
                float scrollRatio = (float) (mouseY - listTop - thumbHeight / 2) / scrollableTrackHeight;
                scrollRatio = Math.max(0.0f, Math.min(1.0f, scrollRatio));

                actionListScrollOffset = (int) (scrollRatio * maxActionListScroll);
                actionListScrollOffset = Math.max(0, Math.min(maxActionListScroll, actionListScrollOffset));
            }
        }

        if (isDraggingParamScrollbar && maxParamScroll > 0) {
            int viewHeight = Math.max(1, paramViewBottom - paramViewTop);
            int thumbHeight = Math.max(18, (int) ((float) viewHeight / (viewHeight + maxParamScroll) * viewHeight));
            int scrollableTrackHeight = viewHeight - thumbHeight;

            if (scrollableTrackHeight > 0) {
                float scrollRatio = (float) (mouseY - paramViewTop - thumbHeight / 2) / scrollableTrackHeight;
                scrollRatio = Math.max(0.0f, Math.min(1.0f, scrollRatio));

                paramScrollOffset = (int) (scrollRatio * maxParamScroll);
                paramScrollOffset = Math.max(0, Math.min(maxParamScroll, paramScrollOffset));
                refreshDynamicParamLayout();
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            isDraggingExpressionPopupScrollbar = false;
            draggingInfoPopup = false;
            draggingInfoDockDivider = false;
            resizingInfoPopupRight = false;
            resizingInfoPopupBottom = false;
            resizingInfoPopupCorner = false;
            syncInfoPanelPreferences();
            savePreferences();
            if (moveChestGridDragging) {
                updateMoveChestGridDrag(mouseX, mouseY);
                endMoveChestGridDrag();
            }
            if (conditionInventoryGridDragging) {
                updateConditionInventoryGridDrag(mouseX, mouseY);
                endConditionInventoryGridDrag();
            }
            if (draggingActionDivider) {
                applyActionDividerDrag(mouseX);
            }
            draggingActionDivider = false;
            isDraggingActionScrollbar = false;
            isDraggingParamScrollbar = false;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (isExpressionEditorPopupOpen()) {
                handleExpressionEditorPopupScroll(dWheel);
                return;
            }

            int mouseScaledX = getEventMouseX();
            int mouseScaledY = getEventMouseY();
            if (handleInfoPopupWheel(mouseScaledX, mouseScaledY, dWheel)) {
                return;
            }

            int listX = getActionListX();
            int listY = getActionListTop();
            int listWidth = actionListWidth;
            int listHeight = getActionListContentHeight();

            if (!actionLibraryCollapsed
                    && mouseScaledX >= listX &&
                    mouseScaledY >= listY &&
                    mouseScaledX < listX + listWidth &&
                    mouseScaledY < listY + listHeight) {

                if (dWheel > 0) {
                    actionListScrollOffset = Math.max(0, actionListScrollOffset - 1);
                } else {
                    actionListScrollOffset = Math.min(maxActionListScroll, actionListScrollOffset + 1);
                }
                return;
            }

            int editorPaneX = getEditorPaneX();
            int editorPaneW = getEditorPaneWidth();
            if (mouseScaledX >= editorPaneX && mouseScaledX <= editorPaneX + editorPaneW
                    && mouseScaledY >= paramViewTop && mouseScaledY <= paramViewBottom) {
                if (handleExpandedDropdownWheel(mouseScaledX, mouseScaledY, dWheel)) {
                    return;
                }
                if (handleBooleanExpressionCustomWheel(mouseScaledX, mouseScaledY, dWheel)) {
                    return;
                }
                if (handleConditionInventoryCustomWheel(mouseScaledX, mouseScaledY, dWheel)) {
                    return;
                }
                if (dWheel > 0) {
                    paramScrollOffset = Math.max(0, paramScrollOffset - 12);
                } else {
                    paramScrollOffset = Math.min(maxParamScroll, paramScrollOffset + 12);
                }
                refreshDynamicParamLayout();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isExpressionEditorPopupOpen()) {
            handleExpressionEditorPopupKeyTyped(typedChar, keyCode);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE && hasActiveInfoSections()) {
            closeInfoPopup();
            return;
        }

        if (actionSearchField != null && actionSearchField.isFocused()) {
            if (keyCode == 1) {
                actionSearchField.setFocused(false);
                collapseAllDropdowns();
                return;
            }
            actionSearchField.textboxKeyTyped(typedChar, keyCode);
            rebuildActionLibraryAndScrollBounds();
            return;
        }

        super.keyTyped(typedChar, keyCode);
        if (keyCode == 1) { // ESC
            collapseAllDropdowns();
            pendingSwitchActionType = null;
        }
        for (int i = 0; i < paramFields.size(); i++) {
            if (isExpressionEditorFieldKey(paramFieldKeys.get(i))) {
                paramFields.get(i).setFocused(false);
                continue;
            }
            GuiTextField field = paramFields.get(i);
            int beforeLen = field.getText().length();
            field.textboxKeyTyped(typedChar, keyCode);
            if (field.getText().length() != beforeLen || Character.isDefined(typedChar)) {
                hasUnsavedChanges = true;
                pendingSwitchActionType = null;
            }
        }
        if (conditionInventoryNbtTagInputField != null) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_RETURN
                    || keyCode == org.lwjgl.input.Keyboard.KEY_NUMPADENTER) {
                if (conditionInventoryNbtTagInputField.isFocused()) {
                    addConditionInventoryTag(conditionInventoryNbtTagInputField.getText());
                    conditionInventoryNbtTagInputField.setText("");
                    hasUnsavedChanges = true;
                    pendingSwitchActionType = null;
                    refreshDynamicParamLayout();
                    return;
                }
            }
            int beforeLen = conditionInventoryNbtTagInputField.getText().length();
            conditionInventoryNbtTagInputField.textboxKeyTyped(typedChar, keyCode);
            if (conditionInventoryNbtTagInputField.getText().length() != beforeLen || Character.isDefined(typedChar)) {
                hasUnsavedChanges = true;
                pendingSwitchActionType = null;
            }
        }
        if (moveChestItemNameInputField != null) {
            if ((keyCode == org.lwjgl.input.Keyboard.KEY_RETURN
                    || keyCode == org.lwjgl.input.Keyboard.KEY_NUMPADENTER)
                    && moveChestItemNameInputField.isFocused()
                    && moveChestNbtTagInputField != null) {
                addMoveChestFilterRule(moveChestItemNameInputField.getText(), moveChestNbtTagInputField.getText());
                moveChestItemNameInputField.setText("");
                moveChestNbtTagInputField.setText("");
                hasUnsavedChanges = true;
                pendingSwitchActionType = null;
                refreshDynamicParamLayout();
                return;
            }
            int beforeLen = moveChestItemNameInputField.getText().length();
            moveChestItemNameInputField.textboxKeyTyped(typedChar, keyCode);
            if (moveChestItemNameInputField.getText().length() != beforeLen || Character.isDefined(typedChar)) {
                hasUnsavedChanges = true;
                pendingSwitchActionType = null;
            }
        }
        if (moveChestNbtTagInputField != null) {
            if ((keyCode == org.lwjgl.input.Keyboard.KEY_RETURN
                    || keyCode == org.lwjgl.input.Keyboard.KEY_NUMPADENTER)
                    && moveChestNbtTagInputField.isFocused()
                    && moveChestItemNameInputField != null) {
                addMoveChestFilterRule(moveChestItemNameInputField.getText(), moveChestNbtTagInputField.getText());
                moveChestItemNameInputField.setText("");
                moveChestNbtTagInputField.setText("");
                hasUnsavedChanges = true;
                pendingSwitchActionType = null;
                refreshDynamicParamLayout();
                return;
            }
            int beforeLen = moveChestNbtTagInputField.getText().length();
            moveChestNbtTagInputField.textboxKeyTyped(typedChar, keyCode);
            if (moveChestNbtTagInputField.getText().length() != beforeLen || Character.isDefined(typedChar)) {
                hasUnsavedChanges = true;
                pendingSwitchActionType = null;
            }
        }
    }

    private int getParamIndexByKey(String type, String targetKey) {
        return paramFieldKeys.indexOf(targetKey);
    }

    private double getParamFieldDoubleValue(String key, double defaultValue) {
        int index = paramFieldKeys.indexOf(key);
        if (index < 0 || index >= paramFields.size()) {
            return defaultValue;
        }
        String text = paramFields.get(index).getText();
        if (text == null || text.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private double getNearbyEntityScanRadius() {
        double scanRadius = getParamFieldDoubleValue("scanRadius", -1.0D);
        if (scanRadius >= 0.0D) {
            return Math.max(1.0D, scanRadius);
        }
        double maxDistance = getParamFieldDoubleValue("range", -1.0D);
        if (maxDistance < 0.0D) {
            maxDistance = getParamFieldDoubleValue("radius", 10.0D);
        }
        return Math.max(1.0D, maxDistance);
    }

    private String formatScanRadius(double radius) {
        if (Math.abs(radius - Math.rint(radius)) < 1.0E-6D) {
            return String.valueOf((long) Math.rint(radius));
        }
        return String.format(Locale.ROOT, "%.1f", radius);
    }

    private int getParamFieldIntValue(String key, int defaultValue) {
        return (int) Math.round(getParamFieldDoubleValue(key, defaultValue));
    }

    String getJoinedStringParam(JsonObject source, String arrayKey, String textKey) {
        if (source == null) {
            return "";
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (source.has(arrayKey) && source.get(arrayKey).isJsonArray()) {
            for (JsonElement element : source.getAsJsonArray(arrayKey)) {
                if (element != null && element.isJsonPrimitive()) {
                    addNormalizedFilterValue(values, element.getAsString());
                }
            }
        } else if (source.has(textKey) && source.get(textKey).isJsonPrimitive()) {
            for (String token : splitFilterText(source.get(textKey).getAsString())) {
                addNormalizedFilterValue(values, token);
            }
        } else if (source.has(arrayKey) && source.get(arrayKey).isJsonPrimitive()) {
            for (String token : splitFilterText(source.get(arrayKey).getAsString())) {
                addNormalizedFilterValue(values, token);
            }
        }
        return String.join(", ", values);
    }

    private String getParamDropdownValue(String key, String defaultValue) {
        int index = paramDropdownKeys.indexOf(key);
        if (index < 0 || index >= paramDropdowns.size()) {
            return defaultValue;
        }
        String value = paramDropdowns.get(index).getValue();
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private boolean getDraftToggleValue(String key, boolean defaultValue) {
        int index = toggleKeys.indexOf(key);
        if (index < 0 || index >= toggleButtons.size()) {
            return currentParams != null && currentParams.has(key)
                    ? currentParams.get(key).getAsBoolean()
                    : defaultValue;
        }
        return toggleButtons.get(index).getValue();
    }

    private boolean isSectionTitleKey(String key) {
        return key != null && key.startsWith("_section_title_");
    }

    private boolean isHuntFixedDistanceModeSelected() {
        return KillAuraHandler.HUNT_MODE_FIXED_DISTANCE.equalsIgnoreCase(
                displayToHuntMode(getParamDropdownValue("huntMode",
                        huntModeToDisplay(currentParams.has("huntMode")
                                ? currentParams.get("huntMode").getAsString()
                                : KillAuraHandler.HUNT_MODE_FIXED_DISTANCE))));
    }

    private boolean isRunSequenceIntervalModeSelected() {
        return "interval".equalsIgnoreCase(displayToRunSequenceExecuteMode(getParamDropdownValue("executeMode",
                runSequenceExecuteModeToDisplay(currentParams.has("executeMode")
                        ? currentParams.get("executeMode").getAsString()
                        : "always"))));
    }

    private boolean isFieldDependencyEnabled(String key) {
        if (key == null || key.trim().isEmpty() || isSectionTitleKey(key)) {
            return true;
        }
        if ("huntChaseIntervalSeconds".equals(key)) {
            return getDraftToggleValue("huntChaseIntervalEnabled", false);
        }
        if ("executeEveryCount".equals(key)) {
            return isRunSequenceIntervalModeSelected();
        }
        return true;
    }

    private String getFieldDependencyHint(String key) {
        if ("huntChaseIntervalSeconds".equals(key) && !isFieldDependencyEnabled(key)) {
            return "需先开启追怪间隔";
        }
        if ("executeEveryCount".equals(key) && !isFieldDependencyEnabled(key)) {
            return "仅间隔执行可用";
        }
        return "";
    }

    private boolean isDropdownDependencyEnabled(String key) {
        return true;
    }

    private String getDropdownDependencyHint(String key) {
        return "";
    }

    private boolean isToggleDependencyEnabled(String key) {
        if ("huntOrbitEnabled".equals(key)) {
            return isHuntFixedDistanceModeSelected();
        }
        return true;
    }

    private String getToggleDependencyHint(String key) {
        if ("huntOrbitEnabled".equals(key) && !isToggleDependencyEnabled(key)) {
            return "仅固定距离可用";
        }
        return "";
    }

    private void refreshDependencyControlStates() {
        for (int i = 0; i < paramFields.size(); i++) {
            String key = i < paramFieldKeys.size() ? paramFieldKeys.get(i) : "";
            GuiTextField field = paramFields.get(i);
            if (field == null || isSectionTitleKey(key)) {
                continue;
            }
            boolean enabled = isFieldDependencyEnabled(key);
            field.setEnabled(enabled);
            if (!enabled) {
                field.setFocused(false);
            }
        }

        for (int i = 0; i < paramDropdowns.size(); i++) {
            String key = i < paramDropdownKeys.size() ? paramDropdownKeys.get(i) : "";
            paramDropdowns.get(i).setEnabled(isDropdownDependencyEnabled(key));
        }

        for (int i = 0; i < toggleButtons.size(); i++) {
            String key = i < toggleKeys.size() ? toggleKeys.get(i) : "";
            ToggleOptionButton button = toggleButtons.get(i);
            button.enabled = isToggleDependencyEnabled(key);
        }

        if (btnSelectHuntAttackSequence != null) {
            boolean enabled = isSequenceHuntAttackModeSelected();
            btnSelectHuntAttackSequence.enabled = enabled;
            btnSelectHuntAttackSequence.displayString = enabled
                    ? getHuntAttackSequenceButtonText()
                    : "§8需切到序列攻击";
        }
    }

    private List<ParamSectionCard> buildParamSectionCards() {
        List<ParamSectionCard> cards = new ArrayList<>();
        for (int i = 0; i < paramFieldKeys.size() && i < paramLabels.size() && i < paramFieldBaseY.size(); i++) {
            String key = paramFieldKeys.get(i);
            if (!isSectionTitleKey(key)) {
                continue;
            }
            String title = paramLabels.get(i);
            int startBaseY = paramFieldBaseY.get(i);
            int nextStart = getParamContentBottomBaseY();
            for (int j = i + 1; j < paramFieldKeys.size() && j < paramFieldBaseY.size(); j++) {
                if (isSectionTitleKey(paramFieldKeys.get(j))) {
                    nextStart = paramFieldBaseY.get(j) - 12;
                    break;
                }
            }
            cards.add(new ParamSectionCard(title, startBaseY, Math.max(startBaseY + 36, nextStart)));
        }
        return cards;
    }

    private void drawParamSectionCards(int rightPaneX, int rightPaneWidth) {
        List<ParamSectionCard> cards = buildParamSectionCards();
        if (cards.isEmpty()) {
            return;
        }
        int cardX = getParamContentX() - 6;
        int cardWidth = Math.max(60, rightPaneWidth - 28);
        for (ParamSectionCard card : cards) {
            int drawTop = card.startBaseY - paramScrollOffset - 4;
            int drawBottom = card.endBaseY - paramScrollOffset + 6;
            if (drawBottom < paramViewTop || drawTop > paramViewBottom) {
                continue;
            }
            int clippedTop = Math.max(paramViewTop, drawTop);
            int clippedBottom = Math.min(paramViewBottom, drawBottom);
            int height = Math.max(28, clippedBottom - clippedTop);
            GuiTheme.drawInputFrameSafe(cardX, clippedTop, cardWidth, height, false, true);
            int titleY = Math.max(clippedTop + 6, drawTop + 6);
            if (titleY + 10 <= paramViewBottom) {
                this.drawString(fontRenderer, card.title, cardX + 8, titleY, 0xFF9FDFFF);
            }
        }
    }

    private JsonObject buildRunSequencePreviewParams() {
        JsonObject preview = currentParams == null
                ? new JsonObject()
                : new JsonParser().parse(currentParams.toString()).getAsJsonObject();
        if (selectedRunSequenceName != null && !selectedRunSequenceName.trim().isEmpty()) {
            preview.addProperty("sequenceName", selectedRunSequenceName.trim());
        }
        String executeMode = displayToRunSequenceExecuteMode(getParamDropdownValue("executeMode",
                runSequenceExecuteModeToDisplay(preview.has("executeMode")
                        ? preview.get("executeMode").getAsString()
                        : "always")));
        preview.addProperty("executeMode", executeMode);
        preview.addProperty("backgroundExecution", displayYesNoToBool(getParamDropdownValue("backgroundExecution",
                boolToDisplayYesNo(preview.has("backgroundExecution")
                        && preview.get("backgroundExecution").getAsBoolean()))));
        int savedExecuteEveryCount = 1;
        if (preview.has("executeEveryCount")) {
            try {
                savedExecuteEveryCount = preview.get("executeEveryCount").getAsInt();
            } catch (Exception ignored) {
                savedExecuteEveryCount = 1;
            }
        }
        preview.addProperty("executeEveryCount", Math.max(1,
                getParamFieldIntValue("executeEveryCount", savedExecuteEveryCount)));
        if (!preview.has("uuid") && currentParams != null && currentParams.has("uuid")) {
            preview.addProperty("uuid", currentParams.get("uuid").getAsString());
        }
        return preview;
    }

    private String stripHelpFormatting(String text) {
        return text == null ? "" : text.replaceAll("§.", "");
    }

    boolean isPointInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void setToggleValue(String key, boolean value) {
        int index = toggleKeys.indexOf(key);
        if (index < 0 || index >= toggleButtons.size()) {
            return;
        }
        toggleButtons.get(index).setValue(value);
    }

    private void appendSelectedNearbyEntityToFilterField(String fieldKey, String toggleKey) {
        if (nearbyEntityDropdown == null || nearbyEntityNameMap.isEmpty()) {
            return;
        }
        String selectedName = nearbyEntityNameMap.get(nearbyEntityDropdown.getValue());
        String normalizedSelected = KillAuraHandler.normalizeFilterName(selectedName);
        if (normalizedSelected.isEmpty()) {
            return;
        }

        int fieldIndex = getParamIndexByKey(availableActionTypes.get(selectedTypeIndex), fieldKey);
        if (fieldIndex < 0 || fieldIndex >= paramFields.size()) {
            return;
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String token : splitFilterText(paramFields.get(fieldIndex).getText())) {
            addNormalizedFilterValue(values, token);
        }
        addNormalizedFilterValue(values, normalizedSelected);
        paramFields.get(fieldIndex).setText(String.join(", ", values));
        setToggleValue(toggleKey, true);
        this.hasUnsavedChanges = true;
        this.pendingSwitchActionType = null;
    }

    private void fillFollowEntityNameFromScan() {
        if (nearbyEntityDropdown == null || nearbyEntityNameMap.isEmpty()) {
            return;
        }
        String selectedName = nearbyEntityNameMap.get(nearbyEntityDropdown.getValue());
        if (selectedName == null || selectedName.trim().isEmpty()) {
            return;
        }

        int fieldIndex = getParamIndexByKey(availableActionTypes.get(selectedTypeIndex), "targetName");
        if (fieldIndex < 0 || fieldIndex >= paramFields.size()) {
            return;
        }

        paramFields.get(fieldIndex).setText(selectedName.trim());
        this.hasUnsavedChanges = true;
        this.pendingSwitchActionType = null;
    }

    private List<String> splitFilterText(String text) {
        List<String> values = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return values;
        }
        for (String token : text.split("\\r?\\n|,|，")) {
            if (token != null && !token.trim().isEmpty()) {
                values.add(token.trim());
            }
        }
        return values;
    }

    private void addNormalizedFilterValue(Set<String> target, String rawValue) {
        if (target == null) {
            return;
        }
        String normalized = KillAuraHandler.normalizeFilterName(rawValue);
        if (!normalized.isEmpty()) {
            target.add(normalized);
        }
    }

    private String getActiveParamHelpText(int mouseX, int mouseY) {
        for (int i = 0; i < paramFields.size(); i++) {
            GuiTextField field = paramFields.get(i);
            if (field.isFocused() || isPointInside(mouseX, mouseY, field.x, field.y, field.width, field.height)) {
                return stripHelpFormatting(paramHelpTexts.get(i));
            }
        }

        for (int i = 0; i < paramDropdowns.size(); i++) {
            EnumDropdown dropdown = paramDropdowns.get(i);
            if (isPointInside(mouseX, mouseY, dropdown.x, dropdown.y, dropdown.width, dropdown.height)) {
                return stripHelpFormatting(paramDropdownHelpTexts.get(i));
            }
        }

        for (int i = 0; i < toggleButtons.size(); i++) {
            ToggleOptionButton button = toggleButtons.get(i);
            if (button.visible && isPointInside(mouseX, mouseY, button.x, button.y, button.width, button.height)) {
                return stripHelpFormatting(toggleHelpTexts.get(i));
            }
        }

        if (nearbyEntityDropdown != null
                && isPointInside(mouseX, mouseY, nearbyEntityDropdown.x, nearbyEntityDropdown.y,
                        nearbyEntityDropdown.width, nearbyEntityDropdown.height)) {
            if ("hunt".equalsIgnoreCase(availableActionTypes.get(selectedTypeIndex))) {
                return I18n.format("gui.path.action_editor.help.hunt_entity_candidates");
            }
            return I18n.format("gui.path.action_editor.help.entity_candidates");
        }

        if (nearbyBlockDropdown != null
                && isPointInside(mouseX, mouseY, nearbyBlockDropdown.x, nearbyBlockDropdown.y,
                        nearbyBlockDropdown.width, nearbyBlockDropdown.height)) {
            return "点击下拉框选择附近可交互方块，会自动填入目标坐标";
        }

        if (btnScanNearbyEntities != null && btnScanNearbyEntities.visible
                && isPointInside(mouseX, mouseY, btnScanNearbyEntities.x, btnScanNearbyEntities.y,
                        btnScanNearbyEntities.width, btnScanNearbyEntities.height)) {
            if ("hunt".equalsIgnoreCase(availableActionTypes.get(selectedTypeIndex))) {
                return I18n.format("gui.path.action_editor.help.hunt_scan_nearby");
            }
            return "按当前范围参数扫描玩家附近的生物目标";
        }

        if (btnScanNearbyBlocks != null && btnScanNearbyBlocks.visible
                && isPointInside(mouseX, mouseY, btnScanNearbyBlocks.x, btnScanNearbyBlocks.y,
                        btnScanNearbyBlocks.width, btnScanNearbyBlocks.height)) {
            return "按当前范围参数扫描玩家附近的可交互方块";
        }

        if (btnFillNearestChest != null && btnFillNearestChest.visible
                && isPointInside(mouseX, mouseY, btnFillNearestChest.x, btnFillNearestChest.y,
                        btnFillNearestChest.width, btnFillNearestChest.height)) {
            return "快速填入最近的普通箱或陷阱箱坐标";
        }

        if (btnSelectRunSequence != null && btnSelectRunSequence.visible
                && isPointInside(mouseX, mouseY, btnSelectRunSequence.x, btnSelectRunSequence.y,
                        btnSelectRunSequence.width, btnSelectRunSequence.height)) {
            return I18n.format("gui.path.action_editor.help.run_sequence_name");
        }

        if (btnSelectHuntAttackSequence != null && btnSelectHuntAttackSequence.visible
                && isPointInside(mouseX, mouseY, btnSelectHuntAttackSequence.x, btnSelectHuntAttackSequence.y,
                        btnSelectHuntAttackSequence.width, btnSelectHuntAttackSequence.height)) {
            return "为中心搜怪击杀选择攻击序列；仅在攻击方式=执行序列攻击时生效。";
        }

        if (btnSelectOtherFeature != null && btnSelectOtherFeature.visible
                && isPointInside(mouseX, mouseY, btnSelectOtherFeature.x, btnSelectOtherFeature.y,
                        btnSelectOtherFeature.width, btnSelectOtherFeature.height)) {
            return I18n.format("gui.path.action_editor.help.other_feature");
        }

        if (btnSelectCapturedId != null && btnSelectCapturedId.visible
                && isPointInside(mouseX, mouseY, btnSelectCapturedId.x, btnSelectCapturedId.y,
                        btnSelectCapturedId.width, btnSelectCapturedId.height)) {
            return "按分组选择一个捕获ID规则，等待其更新或重新捕获";
        }

        if (moveChestItemNameInputField != null
                && moveChestItemNameInputField.getVisible()
                && isPointInside(mouseX, mouseY, moveChestItemNameInputField.x, moveChestItemNameInputField.y,
                        moveChestItemNameInputField.width, moveChestItemNameInputField.height)) {
            return I18n.format("gui.path.action_editor.help.move_chest_item_name");
        }

        if (moveChestNbtTagInputField != null
                && moveChestNbtTagInputField.getVisible()
                && isPointInside(mouseX, mouseY, moveChestNbtTagInputField.x, moveChestNbtTagInputField.y,
                        moveChestNbtTagInputField.width, moveChestNbtTagInputField.height)) {
            return I18n.format("gui.path.action_editor.help.move_chest_required_nbt_tags");
        }

        if (btnAddMoveChestNbtTag != null && btnAddMoveChestNbtTag.visible
                && isPointInside(mouseX, mouseY, btnAddMoveChestNbtTag.x, btnAddMoveChestNbtTag.y,
                        btnAddMoveChestNbtTag.width, btnAddMoveChestNbtTag.height)) {
            return "把当前物品名 + NBT 输入保存为一条批量转移规则；命中任意规则的物品都会被转移";
        }

        if (conditionInventoryNbtTagInputField != null
                && conditionInventoryNbtTagInputField.getVisible()
                && isPointInside(mouseX, mouseY, conditionInventoryNbtTagInputField.x,
                        conditionInventoryNbtTagInputField.y,
                        conditionInventoryNbtTagInputField.width, conditionInventoryNbtTagInputField.height)) {
            return "输入要匹配或排除的 NBT 关键字；支持物品提示、附魔、NBT 文本和物品 ID";
        }

        if (btnAddConditionInventoryNbtTag != null && btnAddConditionInventoryNbtTag.visible
                && isPointInside(mouseX, mouseY, btnAddConditionInventoryNbtTag.x, btnAddConditionInventoryNbtTag.y,
                        btnAddConditionInventoryNbtTag.width, btnAddConditionInventoryNbtTag.height)) {
            return "把当前输入的 NBT 关键字加入列表";
        }

        if (btnAddSelectedHuntWhitelist != null && btnAddSelectedHuntWhitelist.visible
                && isPointInside(mouseX, mouseY, btnAddSelectedHuntWhitelist.x, btnAddSelectedHuntWhitelist.y,
                        btnAddSelectedHuntWhitelist.width, btnAddSelectedHuntWhitelist.height)) {
            return I18n.format("gui.path.action_editor.help.hunt_add_whitelist");
        }

        if (btnAddSelectedHuntBlacklist != null && btnAddSelectedHuntBlacklist.visible
                && isPointInside(mouseX, mouseY, btnAddSelectedHuntBlacklist.x, btnAddSelectedHuntBlacklist.y,
                        btnAddSelectedHuntBlacklist.width, btnAddSelectedHuntBlacklist.height)) {
            return I18n.format("gui.path.action_editor.help.hunt_add_blacklist");
        }

        for (IndexedHitRegion region : moveChestTagRemoveRegions) {
            if (region.contains(mouseX, mouseY)) {
                return "点击删除这一条批量转移规则";
            }
        }

        for (IndexedHitRegion region : conditionInventoryTagRemoveRegions) {
            if (region.contains(mouseX, mouseY)) {
                return "点击删除这一条 NBT 标签条件";
            }
        }

        for (IndexedHitRegion region : conditionInventorySlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                return "拖拽选择要检查的背包槽位；不选任何槽位时默认检查整个背包。";
            }
        }

        for (IndexedHitRegion region : moveChestTargetSlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                return "按住鼠标拖选矩形范围，批量选择或取消目标容器槽位";
            }
        }

        for (IndexedHitRegion region : moveChestInventorySlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                return "按住鼠标拖选矩形范围，批量选择或取消来源背包槽位";
            }
        }

        return "悬停或聚焦某个参数控件，可在这里查看说明";
    }

    private boolean handleBooleanExpressionCustomClick(int mouseX, int mouseY) {
        return BooleanExpressionEditorSupport.handleCustomClick(this, mouseX, mouseY);
    }

    private boolean handleBooleanExpressionCustomWheel(int mouseX, int mouseY, int dWheel) {
        return BooleanExpressionEditorSupport.handleCustomWheel(this, mouseX, mouseY, dWheel);
    }

    private boolean handleConditionInventoryCustomClick(int mouseX, int mouseY) {
        if (!isConditionInventoryActionSelected()) {
            return false;
        }

        for (IndexedHitRegion region : conditionInventoryTagRemoveRegions) {
            if (region.contains(mouseX, mouseY)
                    && region.index >= 0
                    && region.index < conditionInventoryRequiredNbtTags.size()) {
                conditionInventoryRequiredNbtTags.remove(region.index);
                conditionInventoryNbtTagScrollOffset = Math.max(0, conditionInventoryNbtTagScrollOffset - 1);
                return true;
            }
        }
        for (IndexedHitRegion region : conditionInventorySlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                beginConditionInventoryGridDrag(region);
                return true;
            }
        }
        return false;
    }

    private boolean handleConditionInventoryCustomWheel(int mouseX, int mouseY, int dWheel) {
        if (!isConditionInventoryActionSelected() || conditionInventoryRequiredNbtTags.isEmpty()) {
            return false;
        }

        int x = getParamContentX();
        int fieldWidth = getParamFieldWidth();
        int listY = getConditionInventoryNbtTagListBaseY() - paramScrollOffset;
        int listHeight = getConditionInventoryNbtListHeight();
        if (!isPointInside(mouseX, mouseY, x, listY, fieldWidth, listHeight)) {
            return false;
        }

        int rowHeight = 18;
        int visibleRows = Math.max(1, (listHeight - 4) / rowHeight);
        int maxScroll = Math.max(0, conditionInventoryRequiredNbtTags.size() - visibleRows);
        if (maxScroll <= 0) {
            return true;
        }

        if (dWheel > 0) {
            conditionInventoryNbtTagScrollOffset = Math.max(0, conditionInventoryNbtTagScrollOffset - 1);
        } else {
            conditionInventoryNbtTagScrollOffset = Math.min(maxScroll, conditionInventoryNbtTagScrollOffset + 1);
        }
        return true;
    }

    private boolean handleMoveChestCustomClick(int mouseX, int mouseY) {
        if (!isMoveChestActionSelected()) {
            return false;
        }

        for (IndexedHitRegion region : moveChestTagRemoveRegions) {
            if (region.contains(mouseX, mouseY)
                    && region.index >= 0
                    && region.index < moveChestFilterRules.size()) {
                moveChestFilterRules.remove(region.index);
                return true;
            }
        }

        for (IndexedHitRegion region : moveChestTargetSlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                beginMoveChestGridDrag(region, true);
                return true;
            }
        }

        for (IndexedHitRegion region : moveChestInventorySlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                beginMoveChestGridDrag(region, false);
                return true;
            }
        }

        return false;
    }

    private void beginMoveChestGridDrag(IndexedHitRegion region, boolean chestGrid) {
        if (region == null) {
            return;
        }

        pruneMoveChestSelections();
        moveChestGridDragging = true;
        moveChestDraggingChestGrid = chestGrid;
        moveChestDragAnchorIndex = region.index;
        moveChestDragCurrentIndex = region.index;
        moveChestDragSelectionSnapshot.clear();

        Set<Integer> selectedSet = getMoveChestDragSelectionSet();
        moveChestDragSelectionSnapshot.addAll(selectedSet);
        moveChestDragAddMode = !selectedSet.contains(region.index);
        applyMoveChestDragSelection();
    }

    private void beginConditionInventoryGridDrag(IndexedHitRegion region) {
        if (region == null) {
            return;
        }

        pruneConditionInventorySelections();
        conditionInventoryGridDragging = true;
        conditionInventoryDragAnchorIndex = region.index;
        conditionInventoryDragCurrentIndex = region.index;
        conditionInventoryDragSelectionSnapshot.clear();
        conditionInventoryDragSelectionSnapshot.addAll(conditionInventorySelectedSlots);
        conditionInventoryDragAddMode = !conditionInventorySelectedSlots.contains(region.index);
        applyConditionInventoryDragSelection();
    }

    private IndexedHitRegion findMoveChestGridRegion(int mouseX, int mouseY) {
        List<IndexedHitRegion> regions = moveChestDraggingChestGrid
                ? moveChestTargetSlotRegions
                : moveChestInventorySlotRegions;
        for (IndexedHitRegion region : regions) {
            if (region.contains(mouseX, mouseY)) {
                return region;
            }
        }
        return null;
    }

    private void updateMoveChestGridDrag(int mouseX, int mouseY) {
        if (!moveChestGridDragging) {
            return;
        }

        IndexedHitRegion region = findMoveChestGridRegion(mouseX, mouseY);
        if (region == null || region.index == moveChestDragCurrentIndex) {
            return;
        }

        moveChestDragCurrentIndex = region.index;
        applyMoveChestDragSelection();
    }

    private IndexedHitRegion findConditionInventoryGridRegion(int mouseX, int mouseY) {
        for (IndexedHitRegion region : conditionInventorySlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                return region;
            }
        }
        return null;
    }

    private void updateConditionInventoryGridDrag(int mouseX, int mouseY) {
        if (!conditionInventoryGridDragging) {
            return;
        }

        IndexedHitRegion region = findConditionInventoryGridRegion(mouseX, mouseY);
        if (region == null || region.index == conditionInventoryDragCurrentIndex) {
            return;
        }

        conditionInventoryDragCurrentIndex = region.index;
        applyConditionInventoryDragSelection();
    }

    private Set<Integer> getMoveChestDragSelectionSet() {
        return moveChestDraggingChestGrid ? moveChestSelectedChestSlots : moveChestSelectedInventorySlots;
    }

    private void applyMoveChestDragSelection() {
        Set<Integer> selectedSet = getMoveChestDragSelectionSet();
        int cols = moveChestDraggingChestGrid ? getMoveChestChestCols() : getMoveChestInventoryCols();
        int maxSlots = moveChestDraggingChestGrid
                ? getMoveChestChestRows() * getMoveChestChestCols()
                : getMoveChestInventoryRows() * getMoveChestInventoryCols();

        selectedSet.clear();
        selectedSet.addAll(moveChestDragSelectionSnapshot);
        if (cols <= 0 || maxSlots <= 0) {
            return;
        }

        int anchor = MathHelper.clamp(moveChestDragAnchorIndex, 0, maxSlots - 1);
        int current = MathHelper.clamp(moveChestDragCurrentIndex, 0, maxSlots - 1);
        int startRow = Math.min(anchor / cols, current / cols);
        int endRow = Math.max(anchor / cols, current / cols);
        int startCol = Math.min(anchor % cols, current % cols);
        int endCol = Math.max(anchor % cols, current % cols);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int index = row * cols + col;
                if (index < 0 || index >= maxSlots) {
                    continue;
                }
                if (moveChestDragAddMode) {
                    selectedSet.add(index);
                } else {
                    selectedSet.remove(index);
                }
            }
        }
    }

    private void applyConditionInventoryDragSelection() {
        conditionInventorySelectedSlots.clear();
        conditionInventorySelectedSlots.addAll(conditionInventoryDragSelectionSnapshot);

        int cols = getConditionInventoryCols();
        int maxSlots = getConditionInventoryRows() * getConditionInventoryCols();
        if (cols <= 0 || maxSlots <= 0) {
            return;
        }

        int anchor = MathHelper.clamp(conditionInventoryDragAnchorIndex, 0, maxSlots - 1);
        int current = MathHelper.clamp(conditionInventoryDragCurrentIndex, 0, maxSlots - 1);
        int startRow = Math.min(anchor / cols, current / cols);
        int endRow = Math.max(anchor / cols, current / cols);
        int startCol = Math.min(anchor % cols, current % cols);
        int endCol = Math.max(anchor % cols, current % cols);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int index = row * cols + col;
                if (index < 0 || index >= maxSlots) {
                    continue;
                }
                if (conditionInventoryDragAddMode) {
                    conditionInventorySelectedSlots.add(index);
                } else {
                    conditionInventorySelectedSlots.remove(index);
                }
            }
        }
    }

    private void endMoveChestGridDrag() {
        moveChestGridDragging = false;
        moveChestDraggingChestGrid = false;
        moveChestDragAddMode = true;
        moveChestDragAnchorIndex = -1;
        moveChestDragCurrentIndex = -1;
        moveChestDragSelectionSnapshot.clear();
    }

    private void endConditionInventoryGridDrag() {
        conditionInventoryGridDragging = false;
        conditionInventoryDragAddMode = true;
        conditionInventoryDragAnchorIndex = -1;
        conditionInventoryDragCurrentIndex = -1;
        conditionInventoryDragSelectionSnapshot.clear();
    }

    private String getDraftMoveChestDirection() {
        int dropdownIndex = paramDropdownKeys.indexOf("moveDirection");
        if (dropdownIndex >= 0 && dropdownIndex < paramDropdowns.size()) {
            return displayToMoveChestDirection(paramDropdowns.get(dropdownIndex).getValue());
        }
        return currentParams.has("moveDirection")
                ? currentParams.get("moveDirection").getAsString()
                : ItemFilterHandler.MOVE_DIRECTION_INVENTORY_TO_CHEST;
    }

    private void collapseOtherDropdowns(EnumDropdown keep) {
        for (EnumDropdown dropdown : paramDropdowns) {
            if (dropdown != keep) {
                dropdown.collapse();
            }
        }
    }

    void collapseAllDropdowns() {
        for (EnumDropdown dropdown : paramDropdowns) {
            dropdown.collapse();
        }
        if (nearbyEntityDropdown != null) {
            nearbyEntityDropdown.collapse();
        }
        if (nearbyBlockDropdown != null) {
            nearbyBlockDropdown.collapse();
        }
    }

    private void collapseParamDropdowns() {
        for (EnumDropdown dropdown : paramDropdowns) {
            dropdown.collapse();
        }
    }

    private boolean handleExpandedDropdownClick(int mouseX, int mouseY, int mouseButton) {
        if (nearbyBlockDropdown != null && nearbyBlockDropdown.isMouseInsideExpanded(mouseX, mouseY)) {
            boolean handled = nearbyBlockDropdown.handleClick(mouseX, mouseY, mouseButton);
            if (handled) {
                collapseParamDropdowns();
                if (nearbyEntityDropdown != null) {
                    nearbyEntityDropdown.collapse();
                }
                syncNearbyBlockSelectionToPosField();
                this.hasUnsavedChanges = true;
                this.pendingSwitchActionType = null;
            }
            return handled || mouseButton != 0 || nearbyBlockDropdown.expanded;
        }

        if (nearbyEntityDropdown != null && nearbyEntityDropdown.isMouseInsideExpanded(mouseX, mouseY)) {
            boolean handled = nearbyEntityDropdown.handleClick(mouseX, mouseY, mouseButton);
            if (handled) {
                collapseParamDropdowns();
                if (nearbyBlockDropdown != null) {
                    nearbyBlockDropdown.collapse();
                }
                syncNearbyEntitySelectionToPosField();
                this.hasUnsavedChanges = true;
                this.pendingSwitchActionType = null;
            }
            return handled || mouseButton != 0 || nearbyEntityDropdown.expanded;
        }

        for (int i = paramDropdowns.size() - 1; i >= 0; i--) {
            EnumDropdown dropdown = paramDropdowns.get(i);
            if (!dropdown.isMouseInsideExpanded(mouseX, mouseY)) {
                continue;
            }

            String oldValue = dropdown.getValue();
            boolean handled = dropdown.handleClick(mouseX, mouseY, mouseButton);
            if (handled) {
                collapseOtherDropdowns(dropdown);
                String dropdownKey = paramDropdownKeys.get(i);
                onVariableSelectorDropdownChanged(dropdownKey, oldValue, dropdown.getValue());
                boolean refreshDynamicLocatorLayout = shouldRefreshDynamicLocatorLayout(i, oldValue,
                        dropdown.getValue());
                boolean refreshConditionExpressionLayout = shouldRefreshConditionExpressionLayout(dropdownKey, oldValue,
                        dropdown.getValue());
                this.hasUnsavedChanges = true;
                this.pendingSwitchActionType = null;
                if (refreshDynamicLocatorLayout || refreshConditionExpressionLayout) {
                    captureCurrentEditorDraftParams();
                    this.paramScrollOffset = 0;
                    initGui();
                }
            }
            return handled || mouseButton != 0 || dropdown.expanded;
        }

        return false;
    }

    private boolean handleExpandedDropdownWheel(int mouseX, int mouseY, int dWheel) {
        if (dWheel == 0) {
            return false;
        }
        if (nearbyBlockDropdown != null && nearbyBlockDropdown.handleMouseWheel(mouseX, mouseY, dWheel)) {
            return true;
        }
        if (nearbyEntityDropdown != null && nearbyEntityDropdown.handleMouseWheel(mouseX, mouseY, dWheel)) {
            return true;
        }
        for (int i = paramDropdowns.size() - 1; i >= 0; i--) {
            EnumDropdown dropdown = paramDropdowns.get(i);
            if (dropdown.handleMouseWheel(mouseX, mouseY, dWheel)) {
                return true;
            }
        }
        return false;
    }

    private void scanNearbyLivingEntitiesAndRefreshDropdown() {
        if (mc == null || mc.player == null || mc.world == null || nearbyEntityDropdown == null) {
            return;
        }
        nearbyEntityPosMap.clear();
        nearbyEntityNameMap.clear();
        List<EntityEntry> entries = new ArrayList<>();
        double maxDistance = getNearbyEntityScanRadius();
        double maxDistSq = maxDistance * maxDistance;

        for (Object entityObj : mc.world.loadedEntityList) {
            if (!(entityObj instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase living = (EntityLivingBase) entityObj;
            if (living == null || living == mc.player) {
                continue;
            }

            double dx = living.posX - mc.player.posX;
            double dy = living.posY - mc.player.posY;
            double dz = living.posZ - mc.player.posZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > maxDistSq) {
                continue;
            }

            String entityName = getNearbyEntityDisplayName(living);
            String pos = String.format("[%.1f,%.1f,%.1f]", living.posX, living.posY, living.posZ);
            String label = entityName + pos;
            entries.add(new EntityEntry(label, pos, entityName, distSq));
        }

        entries.sort(Comparator.comparingDouble(a -> a.distSq));

        if (entries.isEmpty()) {
            String emptyText = I18n.format("gui.path.action_editor.entity_dropdown_empty_with_radius",
                    formatScanRadius(maxDistance));
            nearbyEntityDropdown.setOptions(new String[] { emptyText });
            nearbyEntityDropdown.setValue(emptyText);
            return;
        }

        String[] options = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            options[i] = entries.get(i).label;
            nearbyEntityPosMap.put(entries.get(i).label, entries.get(i).pos);
            nearbyEntityNameMap.put(entries.get(i).label, entries.get(i).name);
        }
        nearbyEntityDropdown.setOptions(options);
        nearbyEntityDropdown.setValue(options[0]);
        syncNearbyEntitySelectionToPosField();
    }

    private String getNearbyEntityDisplayName(EntityLivingBase living) {
        if (living == null) {
            return "";
        }
        String displayName = living.getDisplayName() == null ? "" : living.getDisplayName().getUnformattedText();
        String normalized = KillAuraHandler.normalizeFilterName(displayName);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        normalized = KillAuraHandler.normalizeFilterName(living.getName());
        return normalized.isEmpty() ? "未知实体" : normalized;
    }

    private void scanNearbyInteractableBlocksAndRefreshDropdown() {
        if (mc == null || mc.player == null || mc.world == null || nearbyBlockDropdown == null) {
            return;
        }
        nearbyBlockPosMap.clear();
        List<BlockEntry> entries = new ArrayList<>();
        int range = Math.max(1, (int) Math.round(getParamFieldDoubleValue("range", 10.0D)));
        double maxDistSq = range * range;

        BlockPos playerPos = mc.player.getPosition();
        for (int y = -range; y <= range; y++) {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq > maxDistSq) {
                        continue;
                    }
                    BlockPos current = playerPos.add(x, y, z);
                    IBlockState state = mc.world.getBlockState(current);
                    if (!isInteractableBlock(state)) {
                        continue;
                    }

                    String pos = String.format("[%d,%d,%d]", current.getX(), current.getY(), current.getZ());
                    String label = state.getBlock().getLocalizedName() + " " + pos;
                    entries.add(new BlockEntry(label, pos, distSq));
                }
            }
        }

        entries.sort(Comparator.comparingDouble(a -> a.distSq));

        if (entries.isEmpty()) {
            nearbyBlockDropdown.setOptions(new String[] { "未找到范围内可交互方块" });
            nearbyBlockDropdown.setValue("未找到范围内可交互方块");
            return;
        }

        String[] options = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            options[i] = entries.get(i).label;
            nearbyBlockPosMap.put(entries.get(i).label, entries.get(i).pos);
        }
        nearbyBlockDropdown.setOptions(options);
        nearbyBlockDropdown.setValue(options[0]);
        syncNearbyBlockSelectionToPosField();
    }

    private boolean isInteractableBlock(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block instanceof BlockChest
                || block instanceof BlockEnderChest
                || block instanceof BlockWorkbench
                || block instanceof BlockFurnace
                || block instanceof BlockAnvil
                || block instanceof BlockBrewingStand
                || block instanceof BlockHopper
                || block instanceof BlockDispenser
                || block instanceof BlockDropper
                || block instanceof BlockDoor
                || block instanceof BlockTrapDoor
                || block instanceof BlockFenceGate
                || block instanceof BlockLever
                || block instanceof BlockButton
                || block instanceof BlockContainer;
    }

    private void syncNearbyEntitySelectionToPosField() {
        if (nearbyEntityDropdown == null || nearbyEntityPosMap.isEmpty()) {
            return;
        }
        String selected = nearbyEntityDropdown.getValue();
        String pos = nearbyEntityPosMap.get(selected);
        if (pos == null) {
            return;
        }
        int posFieldIndex = getParamIndexByKey(availableActionTypes.get(selectedTypeIndex), "pos");
        if (posFieldIndex >= 0 && posFieldIndex < paramFields.size()) {
            paramFields.get(posFieldIndex).setText(pos);
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
        }
    }

    private void syncNearbyBlockSelectionToPosField() {
        if (nearbyBlockDropdown == null || nearbyBlockPosMap.isEmpty()) {
            return;
        }
        String selected = nearbyBlockDropdown.getValue();
        String pos = nearbyBlockPosMap.get(selected);
        if (pos == null) {
            return;
        }
        int posFieldIndex = getParamIndexByKey(availableActionTypes.get(selectedTypeIndex), "pos");
        if (posFieldIndex >= 0 && posFieldIndex < paramFields.size()) {
            paramFields.get(posFieldIndex).setText(pos);
            this.hasUnsavedChanges = true;
            this.pendingSwitchActionType = null;
        }
    }

    private BlockPos findNearestChestPos(int radiusXZ, int radiusY) {
        if (mc == null || mc.player == null || mc.world == null)
            return null;

        BlockPos playerPos = mc.player.getPosition();
        BlockPos nearest = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int y = -radiusY; y <= radiusY; y++) {
            for (int x = -radiusXZ; x <= radiusXZ; x++) {
                for (int z = -radiusXZ; z <= radiusXZ; z++) {
                    BlockPos current = playerPos.add(x, y, z);
                    IBlockState state = mc.world.getBlockState(current);
                    Block block = state.getBlock();
                    if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                        double dx = (current.getX() + 0.5) - mc.player.posX;
                        double dy = current.getY() - mc.player.posY;
                        double dz = (current.getZ() + 0.5) - mc.player.posZ;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            nearest = current;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    private void insertToSystemMessageField(String token) {
        if (systemMessageField == null || token == null || token.isEmpty()) {
            return;
        }
        systemMessageField.setFocused(true);

        String currentText = systemMessageField.getText();
        if (currentText == null) {
            currentText = "";
        }

        int cursorPos = MathHelper.clamp(systemMessageField.getCursorPosition(), 0, currentText.length());
        int selectionPos = MathHelper.clamp(systemMessageField.getSelectionEnd(), 0, currentText.length());
        int start = Math.min(cursorPos, selectionPos);
        int end = Math.max(cursorPos, selectionPos);

        String newText = currentText.substring(0, start) + token + currentText.substring(end);
        systemMessageField.setText(newText);
        systemMessageField.setCursorPosition(start + token.length());
        systemMessageField.setSelectionPos(start + token.length());

        this.hasUnsavedChanges = true;
        this.pendingSwitchActionType = null;
    }

    private boolean hasAvailableRunSequence() {
        String noSeq = I18n.format("gui.path.action_editor.option.no_sequence_available");
        return !(availableSequenceNames.isEmpty()
                || (availableSequenceNames.size() == 1 && noSeq.equals(availableSequenceNames.get(0))));
    }

    String getRunSequenceButtonText() {
        if (selectedRunSequenceName != null && !selectedRunSequenceName.trim().isEmpty()) {
            return "§f" + selectedRunSequenceName;
        }
        if (!hasAvailableRunSequence()) {
            return "§7" + I18n.format("gui.path.action_editor.option.no_sequence_available");
        }
        return I18n.format("gui.conditional.btn.select_seq");
    }

    private boolean isSequenceHuntAttackModeSelected() {
        return KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(
                displayToHuntAttackMode(getParamDropdownValue("attackMode",
                        huntAttackModeToDisplay(currentParams.has("attackMode")
                                ? currentParams.get("attackMode").getAsString()
                                : KillAuraHandler.ATTACK_MODE_NORMAL))));
    }

    String getHuntAttackSequenceButtonText() {
        if (selectedHuntAttackSequenceName != null && !selectedHuntAttackSequenceName.trim().isEmpty()) {
            return "§f" + selectedHuntAttackSequenceName;
        }
        if (!hasAvailableRunSequence()) {
            return "§7" + I18n.format("gui.path.action_editor.option.no_sequence_available");
        }
        return "选择攻击序列";
    }

    void applyHuntActionDraftParams(JsonObject draft) {
        if (!"hunt".equalsIgnoreCase(getSelectedActionType()) || draft == null) {
            return;
        }
        String attackMode = draft.has("attackMode")
                ? draft.get("attackMode").getAsString()
                : KillAuraHandler.ATTACK_MODE_NORMAL;
        if (!KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(attackMode)) {
            return;
        }
        if (selectedHuntAttackSequenceName != null && !selectedHuntAttackSequenceName.trim().isEmpty()) {
            draft.addProperty("attackSequenceName", selectedHuntAttackSequenceName.trim());
        }
    }

    private void applyHuntActionSaveParams(String selectedType, JsonObject newParams) {
        if (!"hunt".equalsIgnoreCase(selectedType) || newParams == null) {
            return;
        }
        String attackMode = newParams.has("attackMode")
                ? newParams.get("attackMode").getAsString()
                : KillAuraHandler.ATTACK_MODE_NORMAL;
        if (KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(attackMode)) {
            if (selectedHuntAttackSequenceName != null && !selectedHuntAttackSequenceName.trim().isEmpty()) {
                newParams.addProperty("attackSequenceName", selectedHuntAttackSequenceName.trim());
            }
        } else {
            newParams.remove("attackSequenceName");
        }
    }

    String getOtherFeatureButtonText() {
        String display = resolveOtherFeatureDisplayName(selectedOtherFeatureId,
                currentParams.has("featureName") ? currentParams.get("featureName").getAsString() : "");
        if (!display.isEmpty()) {
            return "§f" + display;
        }
        if (!hasAvailableOtherFeature()) {
            return "§7" + I18n.format("gui.path.action_editor.option.no_other_feature_available");
        }
        return I18n.format("gui.path.action_editor.button.select_other_feature");
    }

    String getCapturedIdButtonText() {
        if (selectedCapturedIdName != null && !selectedCapturedIdName.trim().isEmpty()) {
            String display = selectedCapturedIdName.trim();
            for (CapturedIdRuleManager.RuleCard card : CapturedIdRuleManager.getRuleCards()) {
                if (card != null && card.model != null && display.equalsIgnoreCase(card.model.name)) {
                    if (card.model.displayName != null && !card.model.displayName.trim().isEmpty()) {
                        display = card.model.displayName.trim() + " (" + card.model.name.trim() + ")";
                    }
                    break;
                }
            }
            return "§f" + display;
        }
        return "§a选择捕获ID";
    }

    private boolean hasAvailableOtherFeature() {
        OtherFeatureGroupManager.reload();
        for (OtherFeatureGroupManager.GroupDef group : OtherFeatureGroupManager.getGroups()) {
            if (group != null && group.features != null && !group.features.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String resolveOtherFeatureDisplayName(String featureId, String fallbackName) {
        String normalizedId = featureId == null ? "" : featureId.trim();
        if (!normalizedId.isEmpty()) {
            OtherFeatureGroupManager.reload();
            for (OtherFeatureGroupManager.GroupDef group : OtherFeatureGroupManager.getGroups()) {
                if (group == null || group.features == null) {
                    continue;
                }
                for (FeatureDef feature : group.features) {
                    if (feature != null && normalizedId.equalsIgnoreCase(feature.id)) {
                        if (feature.name != null && !feature.name.trim().isEmpty()) {
                            return feature.name.trim();
                        }
                        return normalizedId;
                    }
                }
            }
            return normalizedId;
        }
        return fallbackName == null ? "" : fallbackName.trim();
    }

    String safe(String value) {
        return value == null ? "" : value;
    }
}

