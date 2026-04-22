package com.zszl.zszlScriptMod.gui.path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.ActionVariableRegistry;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.runtime.ScopedRuntimeVariables;
import com.zszl.zszlScriptMod.path.runtime.log.ExecutionLogManager;
import com.zszl.zszlScriptMod.path.runtime.log.ExecutionLogManager.ExecutionEvent;
import com.zszl.zszlScriptMod.path.runtime.log.ExecutionLogManager.SessionSnapshot;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import net.minecraft.ChatFormatting;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuiActionVariableManager extends ThemedGuiScreen {

    private static final int TREE_ROW_HEIGHT = 20;
    private static final int SOURCE_CARD_HEIGHT = 96;
    private static final int SOURCE_CARD_GAP = 10;
    private static final int SOURCE_POPUP_SCROLL_STEP = 18;
    private static final int DIVIDER_WIDTH = 12;
    private static final int MIN_LEFT_PANE_WIDTH = 180;
    private static final int MIN_RIGHT_PANE_WIDTH = 220;
    private static final String[] SCOPE_ORDER = new String[] { "global", "sequence", "local", "temp" };
    private static final String PREFERENCES_FILE_NAME = "gui_action_variable_manager_preferences.json";
    private static boolean preferencesLoaded = false;
    private static double savedSplitRatio = 0.34D;

    private static final class TreeRow {
        private final boolean groupRow;
        private final String scopeKey;
        private final String label;
        private final int variableIndex;
        private final int count;

        private TreeRow(boolean groupRow, String scopeKey, String label, int variableIndex, int count) {
            this.groupRow = groupRow;
            this.scopeKey = scopeKey == null ? "sequence" : scopeKey;
            this.label = label == null ? "" : label;
            this.variableIndex = variableIndex;
            this.count = count;
        }
    }

    private static final class SourcePopupVariable {
        private final String name;
        private final String value;
        private final boolean hasValue;

        private SourcePopupVariable(String name, String value, boolean hasValue) {
            this.name = name == null ? "" : name;
            this.value = value == null ? "" : value;
            this.hasValue = hasValue;
        }
    }

    private static final class SourcePopupState {
        private final ActionVariableRegistry.VariableSource source;
        private final String variablePrefix;
        private final String sessionSummary;
        private final String eventSummary;
        private final boolean hasMatchedEvent;
        private final boolean hasMatchedValues;
        private final List<SourcePopupVariable> variables;

        private SourcePopupState(ActionVariableRegistry.VariableSource source, String variablePrefix,
                String sessionSummary, String eventSummary, boolean hasMatchedEvent, boolean hasMatchedValues,
                List<SourcePopupVariable> variables) {
            this.source = source;
            this.variablePrefix = variablePrefix == null ? "" : variablePrefix;
            this.sessionSummary = sessionSummary == null ? "" : sessionSummary;
            this.eventSummary = eventSummary == null ? "" : eventSummary;
            this.hasMatchedEvent = hasMatchedEvent;
            this.hasMatchedValues = hasMatchedValues;
            this.variables = variables == null ? new ArrayList<SourcePopupVariable>() : variables;
        }
    }

    private static final class SourcePopupRow {
        private final SourcePopupVariable variable;
        private final List<String> valueLines;
        private final int offsetY;
        private final int height;

        private SourcePopupRow(SourcePopupVariable variable, List<String> valueLines, int offsetY, int height) {
            this.variable = variable;
            this.valueLines = valueLines == null ? new ArrayList<String>() : valueLines;
            this.offsetY = offsetY;
            this.height = height;
        }
    }

    private static final class SourcePopupCopyHit {
        private final SourcePopupVariable variable;
        private final boolean valueTarget;

        private SourcePopupCopyHit(SourcePopupVariable variable, boolean valueTarget) {
            this.variable = variable;
            this.valueTarget = valueTarget;
        }
    }

    private static final class SourcePopupLayout {
        private final List<String> noteLines;
        private final List<SourcePopupRow> rows;
        private final int contentHeight;

        private SourcePopupLayout(List<String> noteLines, List<SourcePopupRow> rows, int contentHeight) {
            this.noteLines = noteLines == null ? new ArrayList<String>() : noteLines;
            this.rows = rows == null ? new ArrayList<SourcePopupRow>() : rows;
            this.contentHeight = contentHeight;
        }
    }

    private static final class MatchedExecutionLogEvent {
        private final SessionSnapshot session;
        private final ExecutionEvent event;

        private MatchedExecutionLogEvent(SessionSnapshot session, ExecutionEvent event) {
            this.session = session;
            this.event = event;
        }
    }

    private final GuiScreen parentScreen;
    private final List<PathSequenceManager.PathSequence> sequences;

    private List<ActionVariableRegistry.VariableEntry> variables = new ArrayList<>();
    private final List<TreeRow> treeRows = new ArrayList<>();
    private final Set<Integer> selectedIndices = new LinkedHashSet<>();
    private final Set<String> collapsedScopeKeys = new LinkedHashSet<>();
    private int focusedIndex = -1;
    private GuiTextField searchField;
    private String statusText = "§7左侧按作用域查看变量，右侧查看来源档案。";
    private Map<String, Object> globalValues = Collections.emptyMap();

    private double splitRatio = 0.34D;
    private boolean draggingDivider = false;
    private boolean draggingListScrollbar = false;
    private boolean draggingDetailScrollbar = false;

    private int listScrollOffset = 0;
    private int maxListScroll = 0;
    private int detailScrollOffset = 0;
    private int maxDetailScroll = 0;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int workspaceX;
    private int workspaceY;
    private int workspaceWidth;
    private int workspaceHeight;
    private int dividerX;
    private int dividerY;
    private int dividerHeight;
    private int leftPaneX;
    private int leftPaneY;
    private int leftPaneWidth;
    private int leftPaneHeight;
    private int rightPaneX;
    private int rightPaneY;
    private int rightPaneWidth;
    private int rightPaneHeight;
    private int listViewportX;
    private int listViewportY;
    private int listViewportWidth;
    private int listViewportHeight;
    private int detailViewportX;
    private int detailViewportY;
    private int detailViewportWidth;
    private int detailViewportHeight;
    private int detailContentWidth;
    private int footerButtonY;
    private SourcePopupState sourcePopupState;
    private int sourcePopupScrollOffset = 0;
    private int maxSourcePopupScroll = 0;
    private int sourcePopupX = 0;
    private int sourcePopupY = 0;
    private int sourcePopupWidth = 0;
    private int sourcePopupHeight = 0;
    private int sourcePopupViewportX = 0;
    private int sourcePopupViewportY = 0;
    private int sourcePopupViewportWidth = 0;
    private int sourcePopupViewportHeight = 0;

    public GuiActionVariableManager(GuiScreen parentScreen, List<PathSequenceManager.PathSequence> sequences) {
        ensurePreferencesLoaded();
        this.parentScreen = parentScreen;
        this.sequences = sequences == null ? new ArrayList<>() : sequences;
        this.splitRatio = savedSplitRatio;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        String previousSearch = searchField == null ? "" : searchField.getText();
        recalcLayout();

        if (searchField == null) {
            searchField = new GuiTextField(9000, this.fontRenderer, 0, 0, 0, 18);
            searchField.setMaxStringLength(120);
        }
        searchField.setText(previousSearch);
        syncSearchFieldBounds();
        reloadVariables(true);
        layoutButtons();
    }

    @Override
    public void onGuiClosed() {
        savePreferences();
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private static synchronized void ensurePreferencesLoaded() {
        if (preferencesLoaded) {
            return;
        }
        preferencesLoaded = true;
        savedSplitRatio = 0.34D;
        Path path = getPreferencesPath();
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root != null && root.has("splitRatio")) {
                savedSplitRatio = root.get("splitRatio").getAsDouble();
            }
        } catch (Exception ignored) {
            savedSplitRatio = 0.34D;
        }
    }

    private static synchronized void savePreferences() {
        Path path = getPreferencesPath();
        if (path == null) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            JsonObject root = new JsonObject();
            root.addProperty("splitRatio", savedSplitRatio);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private static Path getPreferencesPath() {
        return ProfileManager.getCurrentProfileDir().resolve(PREFERENCES_FILE_NAME);
    }

    private void recalcLayout() {
        panelWidth = Math.max(260, this.width - 8);
        panelHeight = Math.max(180, this.height - 8);
        panelWidth = Math.min(panelWidth, Math.max(220, this.width - 4));
        panelHeight = Math.min(panelHeight, Math.max(160, this.height - 4));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        footerButtonY = panelY + panelHeight - 26;

        workspaceX = panelX + 12;
        workspaceY = panelY + 30;
        workspaceWidth = panelWidth - 24;
        workspaceHeight = footerButtonY - workspaceY - 12;

        int availablePaneWidth = workspaceWidth - DIVIDER_WIDTH;
        int minRightWidth = Math.min(MIN_RIGHT_PANE_WIDTH, Math.max(160, availablePaneWidth - MIN_LEFT_PANE_WIDTH));
        int maxLeftWidth = Math.max(MIN_LEFT_PANE_WIDTH, availablePaneWidth - minRightWidth);
        leftPaneWidth = clamp((int) Math.round(availablePaneWidth * splitRatio), MIN_LEFT_PANE_WIDTH, maxLeftWidth);
        splitRatio = leftPaneWidth / (double) Math.max(1, availablePaneWidth);

        rightPaneWidth = availablePaneWidth - leftPaneWidth;
        leftPaneX = workspaceX;
        leftPaneY = workspaceY;
        leftPaneHeight = workspaceHeight;

        dividerX = leftPaneX + leftPaneWidth;
        dividerY = workspaceY + 4;
        dividerHeight = Math.max(40, workspaceHeight - 8);

        rightPaneX = dividerX + DIVIDER_WIDTH;
        rightPaneY = workspaceY;
        rightPaneHeight = workspaceHeight;

        listViewportX = leftPaneX + 8;
        listViewportY = leftPaneY + 72;
        listViewportWidth = leftPaneWidth - 16;
        listViewportHeight = leftPaneHeight - 80;

        detailViewportX = rightPaneX + 8;
        detailViewportY = rightPaneY + 48;
        detailViewportWidth = rightPaneWidth - 16;
        detailViewportHeight = rightPaneHeight - 56;
        detailContentWidth = Math.max(160, detailViewportWidth - 10);

        syncSearchFieldBounds();
        refreshScrollLimits();
    }

    private void syncSearchFieldBounds() {
        if (searchField == null) {
            return;
        }
        searchField.x = leftPaneX + 14;
        searchField.y = leftPaneY + 42;
        searchField.width = Math.max(120, leftPaneWidth - 28);
        searchField.height = 18;
    }

    private void layoutButtons() {
        int left = panelX + 12;
        int gap = 8;
        int available = panelWidth - 24 - gap * 3;
        int buttonWidth = Math.max(58, available / 4);
        int compactWidth = Math.min(buttonWidth, 96);
        int secondWidth = Math.min(Math.max(70, buttonWidth + 8), 112);

        this.buttonList.add(new ThemedButton(1, left, footerButtonY, compactWidth, 20, "重载"));
        this.buttonList.add(new ThemedButton(2, left + compactWidth + gap, footerButtonY, secondWidth, 20, "重命名"));
        this.buttonList.add(new ThemedButton(3, left + compactWidth + gap + secondWidth + gap, footerButtonY,
                compactWidth, 20, "删除"));
        this.buttonList.add(
                new ThemedButton(4, panelX + panelWidth - compactWidth - 12, footerButtonY, compactWidth, 20, "返回"));
    }

    private void reloadVariables(boolean preserveSelection) {
        globalValues = ScopedRuntimeVariables.getGlobalScopeSnapshot();

        Set<String> selectedNames = preserveSelection ? getSelectedVariableNames() : Collections.emptySet();
        String focusedName = preserveSelection ? getFocusedVariableName() : "";

        List<ActionVariableRegistry.VariableEntry> all = ActionVariableRegistry.collectVariables(sequences);
        all.sort(Comparator
                .comparing((ActionVariableRegistry.VariableEntry entry) -> ActionVariableRegistry
                        .extractScopeKey(entry.getVariableName()))
                .thenComparing(entry -> ActionVariableRegistry.extractBaseName(entry.getVariableName()).toLowerCase()));

        String keyword = PinyinSearchHelper.normalizeQuery(searchField == null ? "" : safe(searchField.getText()));
        variables.clear();
        for (ActionVariableRegistry.VariableEntry entry : all) {
            String searchText = safe(entry.getVariableName()) + " "
                    + safe(ActionVariableRegistry.extractBaseName(entry.getVariableName())) + " "
                    + safe(ActionVariableRegistry.extractScopeKey(entry.getVariableName()));
            if (keyword.isEmpty() || PinyinSearchHelper.matchesNormalized(searchText, keyword)) {
                variables.add(entry);
            }
        }

        selectedIndices.clear();
        focusedIndex = -1;
        for (int i = 0; i < variables.size(); i++) {
            ActionVariableRegistry.VariableEntry entry = variables.get(i);
            String name = entry == null ? "" : safe(entry.getVariableName());
            if (preserveSelection && selectedNames.contains(name)) {
                selectedIndices.add(i);
            }
            if (focusedIndex < 0 && preserveSelection && !focusedName.isEmpty() && focusedName.equalsIgnoreCase(name)) {
                focusedIndex = i;
            }
        }

        if (focusedIndex < 0 && !variables.isEmpty()) {
            focusedIndex = 0;
        }
        if (focusedIndex >= 0 && focusedIndex < variables.size()) {
            selectedIndices.add(focusedIndex);
        }

        listScrollOffset = Math.max(0, listScrollOffset);
        detailScrollOffset = 0;
        rebuildTreeRows();
        refreshScrollLimits();
        updateStatusText();
    }

    private void refreshScrollLimits() {
        maxListScroll = Math.max(0, treeRows.size() - getVisibleTreeRowCount());
        listScrollOffset = clamp(listScrollOffset, 0, maxListScroll);

        int sourceRows = getTotalSourceRows();
        maxDetailScroll = Math.max(0, sourceRows - getVisibleSourceRows());
        detailScrollOffset = clamp(detailScrollOffset, 0, maxDetailScroll);
    }

    private void rebuildTreeRows() {
        treeRows.clear();
        Map<String, List<Integer>> grouped = new LinkedHashMap<>();
        for (String scope : SCOPE_ORDER) {
            grouped.put(scope, new ArrayList<Integer>());
        }
        for (int i = 0; i < variables.size(); i++) {
            String scope = ActionVariableRegistry.extractScopeKey(variables.get(i).getVariableName());
            if (!grouped.containsKey(scope)) {
                grouped.put(scope, new ArrayList<Integer>());
            }
            grouped.get(scope).add(i);
        }

        for (String scope : SCOPE_ORDER) {
            appendScopeRows(scope, grouped.get(scope));
        }
        for (Map.Entry<String, List<Integer>> entry : grouped.entrySet()) {
            if (!isKnownScope(entry.getKey())) {
                appendScopeRows(entry.getKey(), entry.getValue());
            }
        }
    }

    private void appendScopeRows(String scopeKey, List<Integer> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return;
        }
        treeRows.add(
                new TreeRow(true, scopeKey, ActionVariableRegistry.scopeKeyToDisplay(scopeKey), -1, indexes.size()));
        if (collapsedScopeKeys.contains(scopeKey)) {
            return;
        }
        for (Integer index : indexes) {
            if (index != null && index >= 0 && index < variables.size()) {
                treeRows.add(new TreeRow(false, scopeKey,
                        ActionVariableRegistry.extractBaseName(variables.get(index).getVariableName()),
                        index, 0));
            }
        }
    }

    private int getVisibleTreeRowCount() {
        return Math.max(1, listViewportHeight / TREE_ROW_HEIGHT);
    }

    private int getSourceCardColumns() {
        return detailContentWidth >= 680 ? 2 : 1;
    }

    private int getVisibleSourceRows() {
        return Math.max(1, (detailViewportHeight + SOURCE_CARD_GAP) / (SOURCE_CARD_HEIGHT + SOURCE_CARD_GAP));
    }

    private int getTotalSourceRows() {
        ActionVariableRegistry.VariableEntry entry = getFocusedEntry();
        if (entry == null) {
            return 0;
        }
        int sourceCount = entry.getSources().size();
        if (sourceCount <= 0) {
            return 0;
        }
        int cols = getSourceCardColumns();
        return (sourceCount + cols - 1) / cols;
    }

    private String getFocusedVariableName() {
        if (focusedIndex < 0 || focusedIndex >= variables.size()) {
            return "";
        }
        ActionVariableRegistry.VariableEntry entry = variables.get(focusedIndex);
        return entry == null ? "" : safe(entry.getVariableName());
    }

    private Set<String> getSelectedVariableNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Integer index : selectedIndices) {
            if (index == null || index < 0 || index >= variables.size()) {
                continue;
            }
            ActionVariableRegistry.VariableEntry entry = variables.get(index);
            if (entry != null) {
                names.add(safe(entry.getVariableName()));
            }
        }
        return names;
    }

    private ActionVariableRegistry.VariableEntry getFocusedEntry() {
        if (focusedIndex < 0 || focusedIndex >= variables.size()) {
            return null;
        }
        return variables.get(focusedIndex);
    }

    private void updateStatusText() {
        if (variables.isEmpty()) {
            statusText = "§7当前搜索结果为空，没有找到变量定义。";
            return;
        }
        ActionVariableRegistry.VariableEntry focused = getFocusedEntry();
        if (focused == null) {
            statusText = "§7已加载 " + variables.size() + " 个变量。";
            return;
        }

        int selectedCount = Math.max(1, selectedIndices.size());
        int sourceCount = focused.getSources().size();
        statusText = "§b共 " + variables.size() + " 个变量  §7|  §f已选中 " + selectedCount
                + " 个  §7|  §f当前查看 §b" + focused.getVariableName()
                + " §7(" + sourceCount + " 个来源)  |  §e点击来源卡片查看子变量";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                reloadVariables(true);
                statusText = "§a已刷新变量列表，共 " + variables.size() + " 个变量。";
                break;
            case 2:
                if (getFocusedEntry() == null) {
                    statusText = "§e请先从左侧选择一个变量。";
                    return;
                }
                ActionVariableRegistry.VariableEntry target = getFocusedEntry();
                mc.setScreen(new GuiTextInput(this, "重命名变量",
                        ActionVariableRegistry.extractBaseName(target.getVariableName()), newName -> {
                            String normalized = newName == null ? "" : newName.trim();
                            if (normalized.isEmpty()) {
                                mc.setScreen(this);
                                return;
                            }
                            String renamed = ActionVariableRegistry.buildScopedVariableName(target.getScopeKey(),
                                    normalized);
                            int changed = ActionVariableRegistry.renameVariable(sequences, target.getVariableName(),
                                    renamed);
                            statusText = (changed > 0 ? "§a" : "§e") + "重命名完成，受影响动作: " + changed;
                            if (mc.player != null) {
                                mc.player.sendSystemMessage(new TextComponentString(
                                        (changed > 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
                                                + "重命名变量完成，受影响动作: " + changed));
                            }
                            reloadVariables(false);
                            focusVariableByName(renamed);
                            mc.setScreen(this);
                        }));
                break;
            case 3:
                if (selectedIndices.isEmpty()) {
                    statusText = "§e请先选择要清除的变量卡片。";
                    return;
                }
                List<String> names = new ArrayList<>();
                for (Integer index : selectedIndices) {
                    if (index != null && index >= 0 && index < variables.size()) {
                        names.add(variables.get(index).getVariableName());
                    }
                }
                int removed = ActionVariableRegistry.clearVariables(sequences, names);
                statusText = "§a已清除变量定义动作: " + removed;
                reloadVariables(false);
                break;
            case 4:
                mc.setScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    private void focusVariableByName(String variableName) {
        String normalized = safe(variableName).trim();
        if (normalized.isEmpty()) {
            return;
        }
        for (int i = 0; i < variables.size(); i++) {
            ActionVariableRegistry.VariableEntry entry = variables.get(i);
            if (entry != null && normalized.equalsIgnoreCase(safe(entry.getVariableName()).trim())) {
                collapsedScopeKeys.remove(ActionVariableRegistry.extractScopeKey(normalized));
                focusedIndex = i;
                selectedIndices.clear();
                selectedIndices.add(i);
                detailScrollOffset = 0;
                rebuildTreeRows();
                refreshScrollLimits();
                ensureListVisible(i);
                updateStatusText();
                return;
            }
        }
    }

    private void ensureListVisible(int index) {
        int treeIndex = getTreeRowIndexForVariable(index);
        int visible = getVisibleTreeRowCount();
        if (treeIndex < listScrollOffset) {
            listScrollOffset = treeIndex;
        } else if (treeIndex >= listScrollOffset + visible) {
            listScrollOffset = treeIndex - visible + 1;
        }
        listScrollOffset = clamp(listScrollOffset, 0, maxListScroll);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        globalValues = ScopedRuntimeVariables.getGlobalScopeSnapshot();

        drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "动作全局变量", this.fontRenderer);

        drawPaneBackground(leftPaneX, leftPaneY, leftPaneWidth, leftPaneHeight, 0xFF6CC7FF, 0xA0162230, 0xA00D151F);
        drawPaneBackground(rightPaneX, rightPaneY, rightPaneWidth, rightPaneHeight, 0xFF79D2B4, 0xA017222C, 0xA00D141C);
        drawDivider(mouseX, mouseY);

        drawLeftPaneHeader();
        drawRightPaneHeader();
        drawThemedTextField(searchField);

        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            drawString(fontRenderer, "§8按变量名搜索，支持实时过滤", searchField.x + 4, searchField.y + 5, 0xFFFFFFFF);
        }

        drawVariableTree(mouseX, mouseY);
        drawSourceCards(mouseX, mouseY);
        drawFooterStatus();

        super.drawScreen(mouseX, mouseY, partialTicks);
        if (sourcePopupState != null) {
            drawSourcePopup(mouseX, mouseY);
        } else {
            drawScreenTooltip(mouseX, mouseY);
        }
    }

    private void drawLeftPaneHeader() {
        drawString(fontRenderer, "变量树", leftPaneX + 12, leftPaneY + 10, 0xFFFFFFFF);
        drawString(fontRenderer, "按作用域展开 / 收起变量名", leftPaneX + 12, leftPaneY + 20, 0xFF9FBFD7);

        int chipY = leftPaneY + 10;
        int right = leftPaneX + leftPaneWidth - 12;
        right = drawChipAlignedRight(right, chipY, "树节点 " + treeRows.size(), 0xFF5A93D8, 0xAA132533);
        drawChipAlignedRight(right - 6, chipY, "变量 " + variables.size(), 0xFF66C6B5, 0xAA132533);

        drawString(fontRenderer, "搜索变量", leftPaneX + 14, leftPaneY + 32, 0xFFB6CCE0);
    }

    private void drawRightPaneHeader() {
        ActionVariableRegistry.VariableEntry entry = getFocusedEntry();
        String header = entry == null ? "来源档案" : "来源档案 · " + safe(entry.getVariableName());
        drawString(fontRenderer, header, rightPaneX + 12, rightPaneY + 10, 0xFFFFFFFF);
        drawString(fontRenderer,
                entry == null
                        ? "请选择左侧变量前缀，再点击卡片查看详细子变量。"
                        : "当前变量为变量前缀，请点击卡片查看详细子变量。",
                rightPaneX + 12, rightPaneY + 20, 0xFF9FBFD7);
        drawString(fontRenderer, "来源卡片流", detailViewportX + 2, detailViewportY - 14, 0xFFE5F5FF);
    }

    private void drawVariableTree(int mouseX, int mouseY) {
        drawRect(listViewportX, listViewportY, listViewportX + listViewportWidth, listViewportY + listViewportHeight,
                0x44101821);
        beginScissor(listViewportX, listViewportY, listViewportWidth, listViewportHeight);

        int startIndex = listScrollOffset;
        int visible = getVisibleTreeRowCount();
        int rowX = listViewportX + 2;
        int rowWidth = listViewportWidth - 8;
        for (int row = 0; row < visible; row++) {
            int index = startIndex + row;
            if (index >= treeRows.size()) {
                break;
            }

            int rowY = listViewportY + row * TREE_ROW_HEIGHT;
            TreeRow treeRow = treeRows.get(index);
            drawTreeRow(treeRow, rowX, rowY, rowWidth, TREE_ROW_HEIGHT,
                    isHoverRegion(mouseX, mouseY, rowX, rowY, rowWidth, TREE_ROW_HEIGHT));
        }

        if (variables.isEmpty()) {
            drawCenteredString(fontRenderer, "暂无变量", listViewportX + listViewportWidth / 2, listViewportY + 22,
                    0xFFB5C7D8);
            drawCenteredString(fontRenderer, "动作里使用 set_var / capture_* 后会自动出现在这里",
                    listViewportX + listViewportWidth / 2,
                    listViewportY + 38, 0xFF8CA2B7);
        }
        endScissor();

        drawScrollBar(listViewportX + listViewportWidth - 6, listViewportY + 2, listViewportHeight - 4,
                listScrollOffset, maxListScroll, treeRows.size(), getVisibleTreeRowCount());
    }

    private void drawTreeRow(TreeRow treeRow, int x, int y, int width, int height, boolean hovered) {
        if (treeRow == null) {
            return;
        }
        if (treeRow.groupRow) {
            boolean collapsed = collapsedScopeKeys.contains(treeRow.scopeKey);
            int bg = hovered ? 0xAA22384D : 0x8A172633;
            int border = hovered ? 0xFF7ED0FF : 0xFF3F6A8C;
            drawRect(x, y, x + width, y + height - 1, bg);
            drawHorizontalLine(x, x + width, y, border);
            drawHorizontalLine(x, x + width, y + height - 1, border);
            drawString(fontRenderer, collapsed ? "▶" : "▼", x + 6, y + 6, 0xFFEAF7FF);
            drawString(fontRenderer, treeRow.label, x + 18, y + 6, 0xFFFFFFFF);
            drawString(fontRenderer, "(" + treeRow.count + ")",
                    x + width - 6 - fontRenderer.getStringWidth("(" + treeRow.count + ")"),
                    y + 6, 0xFF9FDFFF);
            return;
        }

        ActionVariableRegistry.VariableEntry entry = treeRow.variableIndex >= 0
                && treeRow.variableIndex < variables.size()
                        ? variables.get(treeRow.variableIndex)
                        : null;
        if (entry == null) {
            return;
        }
        boolean focused = treeRow.variableIndex == focusedIndex;
        boolean selected = selectedIndices.contains(treeRow.variableIndex);
        int bg = focused ? 0xAA27445D : (selected ? 0x99253C50 : (hovered ? 0x66223342 : 0x44111922));
        int border = focused ? 0xFF7AD9FF : (selected ? 0xFF67BFF0 : 0x00000000);
        drawRect(x, y, x + width, y + height - 1, bg);
        if (border != 0) {
            drawHorizontalLine(x, x + width, y, border);
            drawHorizontalLine(x, x + width, y + height - 1, border);
        }
        drawString(fontRenderer, "•", x + 18, y + 6, hasGlobalValue(entry.getVariableName()) ? 0xFF7AE59B : 0xFFF0C56D);
        String label = ActionVariableRegistry.extractBaseName(entry.getVariableName());
        drawString(fontRenderer, fontRenderer.trimStringToWidth(label, width - 74), x + 28, y + 6, 0xFFEAF7FF);
        String count = String.valueOf(entry.getSources().size());
        drawString(fontRenderer, count, x + width - 6 - fontRenderer.getStringWidth(count), y + 6, 0xFF9FDFFF);
    }

    private void drawSourceCards(int mouseX, int mouseY) {
        drawRect(detailViewportX, detailViewportY, detailViewportX + detailViewportWidth,
                detailViewportY + detailViewportHeight, 0x40101820);

        ActionVariableRegistry.VariableEntry entry = getFocusedEntry();
        if (entry == null) {
            drawCenteredString(fontRenderer, "暂无来源卡片", detailViewportX + detailViewportWidth / 2, detailViewportY + 20,
                    0xFF9FB3C6);
            return;
        }

        List<ActionVariableRegistry.VariableSource> sources = entry.getSources();
        if (sources.isEmpty()) {
            drawCenteredString(fontRenderer, "这个变量当前没有来源定义", detailViewportX + detailViewportWidth / 2,
                    detailViewportY + 20, 0xFF9FB3C6);
            return;
        }

        int cols = getSourceCardColumns();
        int gap = SOURCE_CARD_GAP;
        int cardWidth = cols <= 1
                ? detailContentWidth - 4
                : (detailContentWidth - gap * (cols - 1) - 4) / cols;

        beginScissor(detailViewportX, detailViewportY, detailViewportWidth, detailViewportHeight);
        int visibleRows = getVisibleSourceRows();
        int startRow = detailScrollOffset;
        for (int row = 0; row < visibleRows; row++) {
            int actualRow = startRow + row;
            int baseIndex = actualRow * cols;
            if (baseIndex >= sources.size()) {
                break;
            }
            int cardY = detailViewportY + row * (SOURCE_CARD_HEIGHT + gap);
            for (int col = 0; col < cols; col++) {
                int sourceIndex = baseIndex + col;
                if (sourceIndex >= sources.size()) {
                    break;
                }
                int cardX = detailViewportX + 2 + col * (cardWidth + gap);
                boolean hovered = isHoverRegion(mouseX, mouseY, cardX, cardY, cardWidth, SOURCE_CARD_HEIGHT);
                drawSourceCard(sources.get(sourceIndex), sourceIndex, cardX, cardY, cardWidth, SOURCE_CARD_HEIGHT,
                        hovered);
            }
        }
        endScissor();

        drawScrollBar(detailViewportX + detailViewportWidth - 6, detailViewportY + 2, detailViewportHeight - 4,
                detailScrollOffset, maxDetailScroll, getTotalSourceRows(), getVisibleSourceRows());
    }

    private void drawSourceCard(ActionVariableRegistry.VariableSource source, int index, int x, int y, int width,
            int height,
            boolean hovered) {
        boolean editable = source != null && source.isCustomSequence();
        int accent = editable ? 0xFF67D0B5 : 0xFFF1C86C;
        int top = editable ? 0xB31C3440 : 0xB3292620;
        int bottom = editable ? 0xC110171D : 0xC1151411;
        drawCardShell(x, y, width, height, accent, top, bottom, hovered, false);
        drawRect(x + 1, y + 1, x + 3, y + height - 1, accent);

        String seqName = safe(source.getSequenceName()).trim();
        if (seqName.isEmpty()) {
            seqName = "(未命名序列)";
        }
        drawString(fontRenderer, fontRenderer.trimStringToWidth(seqName, width - 106), x + 10, y + 8, 0xFFFFFFFF);
        drawChipAlignedRight(x + width - 8, y + 8, trimToChip(safe(source.getActionType()), 42), accent, 0xAA173242);

        String category = safe(source.getCategory());
        String subCategory = safe(source.getSubCategory());
        String categoryLine = category + (subCategory.trim().isEmpty() ? "" : " / " + subCategory);
        drawString(fontRenderer, fontRenderer.trimStringToWidth(categoryLine, width - 20), x + 10, y + 24, 0xFFB5C9DA);

        drawString(fontRenderer,
                "步骤 " + (source.getStepIndex() + 1) + "  ·  动作 " + (source.getActionIndex() + 1)
                        + "  ·  字段 " + safe(source.getParamKey()),
                x + 10, y + 38, 0xFF9CB5C8);

        String canonicalVariableName = ActionVariableRegistry.buildCanonicalVariableName(
                ActionVariableRegistry.extractScopeKey(source.getVariableName()),
                ActionVariableRegistry.extractBaseName(source.getVariableName()));
        List<String> extra = wrapText("变量名 " + safe(canonicalVariableName), width - 20, 1);
        if (!extra.isEmpty()) {
            drawString(fontRenderer, extra.get(0), x + 10, y + 52, 0xFFDEECF7);
        }

        String footer = editable ? "左键查看子变量 · 可编辑自定义序列" : "左键查看子变量 · 内置序列只读";
        drawString(fontRenderer, footer, x + 10, y + height - 14, editable ? 0xFF79E0BE : 0xFFF2CB76);
        drawString(fontRenderer, "#" + (index + 1), x + width - 18 - fontRenderer.getStringWidth("#" + (index + 1)),
                y + height - 14,
                0xFF9DB2C5);
    }

    private int getSourceIndexAt(int mouseX, int mouseY) {
        if (!isHoverRegion(mouseX, mouseY, detailViewportX, detailViewportY, detailViewportWidth,
                detailViewportHeight)) {
            return -1;
        }

        ActionVariableRegistry.VariableEntry entry = getFocusedEntry();
        if (entry == null) {
            return -1;
        }

        List<ActionVariableRegistry.VariableSource> sources = entry.getSources();
        if (sources.isEmpty()) {
            return -1;
        }

        int cols = getSourceCardColumns();
        int gap = SOURCE_CARD_GAP;
        int cardWidth = cols <= 1
                ? detailContentWidth - 4
                : (detailContentWidth - gap * (cols - 1) - 4) / cols;
        int visibleRows = getVisibleSourceRows();
        int startRow = detailScrollOffset;
        for (int row = 0; row < visibleRows; row++) {
            int actualRow = startRow + row;
            int baseIndex = actualRow * cols;
            if (baseIndex >= sources.size()) {
                break;
            }
            int cardY = detailViewportY + row * (SOURCE_CARD_HEIGHT + gap);
            for (int col = 0; col < cols; col++) {
                int sourceIndex = baseIndex + col;
                if (sourceIndex >= sources.size()) {
                    break;
                }
                int cardX = detailViewportX + 2 + col * (cardWidth + gap);
                if (isHoverRegion(mouseX, mouseY, cardX, cardY, cardWidth, SOURCE_CARD_HEIGHT)) {
                    return sourceIndex;
                }
            }
        }
        return -1;
    }

    private void openSourcePopup(ActionVariableRegistry.VariableSource source) {
        if (source == null) {
            return;
        }
        sourcePopupState = buildSourcePopupState(source);
        sourcePopupScrollOffset = 0;
        maxSourcePopupScroll = 0;
    }

    private void closeSourcePopup() {
        sourcePopupState = null;
        sourcePopupScrollOffset = 0;
        maxSourcePopupScroll = 0;
    }

    private SourcePopupState buildSourcePopupState(ActionVariableRegistry.VariableSource source) {
        String prefix = ActionVariableRegistry.buildCanonicalVariableName(
                ActionVariableRegistry.extractScopeKey(source == null ? "" : source.getVariableName()),
                ActionVariableRegistry.extractBaseName(source == null ? "" : source.getVariableName()));
        LinkedHashSet<String> variableNames = new LinkedHashSet<>(
                ActionVariableRegistry.collectProducedVariableNames(source));
        MatchedExecutionLogEvent matched = findMatchedExecutionLogEvent(source, prefix);
        SessionSnapshot matchedSession = matched == null ? null : matched.session;
        ExecutionEvent matchedEvent = matched == null ? null : matched.event;

        LinkedHashMap<String, String> matchedValues = new LinkedHashMap<>();
        if (matchedEvent != null) {
            for (Map.Entry<String, String> entry : matchedEvent.getVariablePreview().entrySet()) {
                if (matchesSourceVariableKey(entry.getKey(), prefix)) {
                    variableNames.add(entry.getKey());
                    matchedValues.put(entry.getKey(), safe(entry.getValue()));
                }
            }
        }
        mergeCurrentGlobalValues(prefix, variableNames, matchedValues);
        if (variableNames.isEmpty() && !prefix.isEmpty()) {
            variableNames.add(prefix);
        }

        List<SourcePopupVariable> variables = new ArrayList<>();
        for (String variableName : variableNames) {
            boolean hasValue = matchedValues.containsKey(variableName);
            variables.add(
                    new SourcePopupVariable(variableName, hasValue ? matchedValues.get(variableName) : "", hasValue));
        }

        String sessionSummary = matchedSession == null
                ? "最近匹配日志: 未找到"
                : "最近匹配日志: " + matchedSession.buildSummary();
        String eventSummary = matchedEvent == null
                ? (matchedValues.isEmpty()
                ? "匹配事件: 未找到当前来源动作的执行记录"
                : "匹配事件: 未命中执行日志，已回退到当前全局作用域")
                : "匹配事件: 步骤 " + (matchedEvent.getStepIndex() + 1)
                        + " · 动作 " + (matchedEvent.getActionIndex() + 1)
                        + " · " + safe(matchedEvent.getType()).toUpperCase();

        return new SourcePopupState(source, prefix, sessionSummary, eventSummary,
                matchedEvent != null, !matchedValues.isEmpty(), variables);
    }

    private MatchedExecutionLogEvent findMatchedExecutionLogEvent(ActionVariableRegistry.VariableSource source, String prefix) {
        if (source == null) {
            return null;
        }
        SessionSnapshot bestSession = null;
        ExecutionEvent bestEvent = null;
        int bestScore = Integer.MIN_VALUE;

        for (SessionSnapshot session : ExecutionLogManager.getSessionsSnapshot()) {
            if (session == null || !safe(session.getSequenceName()).equalsIgnoreCase(safe(source.getSequenceName()))) {
                continue;
            }
            List<ExecutionEvent> events = session.getEvents();
            for (int i = events.size() - 1; i >= 0; i--) {
                ExecutionEvent event = events.get(i);
                int score = scoreExecutionLogEvent(source, prefix, event);
                if (score > bestScore) {
                    bestScore = score;
                    bestSession = session;
                    bestEvent = event;
                }
            }
            if (bestScore >= 1000) {
                break;
            }
        }

        return bestEvent == null ? null : new MatchedExecutionLogEvent(bestSession, bestEvent);
    }

    private int scoreExecutionLogEvent(ActionVariableRegistry.VariableSource source, String prefix, ExecutionEvent event) {
        if (source == null || event == null) {
            return Integer.MIN_VALUE;
        }

        boolean exactMatch = event.getStepIndex() == source.getStepIndex()
                && event.getActionIndex() == source.getActionIndex();
        boolean shiftedControlMatch = safe(event.getType()).equalsIgnoreCase("trace")
                && safe(event.getMessage()).startsWith("控制流动作")
                && event.getStepIndex() == source.getStepIndex()
                && event.getActionIndex() == source.getActionIndex() + 1;
        boolean sameStep = event.getStepIndex() == source.getStepIndex();
        boolean hasMatchingValues = eventContainsSourceVariables(event, prefix);

        if (exactMatch && hasMatchingValues) {
            return 1000;
        }
        if (shiftedControlMatch && hasMatchingValues) {
            return 900;
        }
        if (exactMatch) {
            return 700;
        }
        if (shiftedControlMatch) {
            return 650;
        }
        if (hasMatchingValues && sameStep) {
            return 500;
        }
        if (hasMatchingValues) {
            return 400;
        }
        return Integer.MIN_VALUE;
    }

    private boolean eventContainsSourceVariables(ExecutionEvent event, String prefix) {
        if (event == null || prefix == null || prefix.trim().isEmpty()) {
            return false;
        }
        for (String key : event.getVariablePreview().keySet()) {
            if (matchesSourceVariableKey(key, prefix)) {
                return true;
            }
        }
        return false;
    }

    private void mergeCurrentGlobalValues(String prefix, Set<String> variableNames, Map<String, String> matchedValues) {
        if (prefix == null || prefix.trim().isEmpty() || variableNames == null || matchedValues == null) {
            return;
        }
        String scopeKey = ActionVariableRegistry.extractScopeKey(prefix);
        if (!"global".equals(scopeKey) || globalValues == null || globalValues.isEmpty()) {
            return;
        }
        for (String variableName : new ArrayList<>(variableNames)) {
            if (matchedValues.containsKey(variableName)) {
                continue;
            }
            String baseName = ActionVariableRegistry.extractBaseName(variableName);
            if (!globalValues.containsKey(baseName)) {
                continue;
            }
            Object value = globalValues.get(baseName);
            matchedValues.put(variableName, value == null ? "" : String.valueOf(value));
        }
    }

    private boolean matchesSourceVariableKey(String key, String prefix) {
        String normalizedKey = safe(key).trim();
        String normalizedPrefix = safe(prefix).trim();
        if (normalizedKey.isEmpty() || normalizedPrefix.isEmpty()) {
            return false;
        }
        return normalizedKey.equals(normalizedPrefix) || normalizedKey.startsWith(normalizedPrefix + "_");
    }

    private void updateSourcePopupBounds() {
        int maxWidth = Math.max(240, panelWidth - 24);
        int maxHeight = Math.max(180, panelHeight - 28);
        int minWidth = Math.min(320, maxWidth);
        int minHeight = Math.min(220, maxHeight);
        sourcePopupWidth = clamp(panelWidth - 72, minWidth, maxWidth);
        sourcePopupHeight = clamp(panelHeight - 88, minHeight, maxHeight);
        sourcePopupX = panelX + (panelWidth - sourcePopupWidth) / 2;
        sourcePopupY = panelY + (panelHeight - sourcePopupHeight) / 2;
        sourcePopupViewportX = sourcePopupX + 12;
        sourcePopupViewportY = sourcePopupY + 60;
        sourcePopupViewportWidth = sourcePopupWidth - 24;
        sourcePopupViewportHeight = sourcePopupHeight - 80;
    }

    private SourcePopupLayout buildSourcePopupLayout(int wrapWidth) {
        if (sourcePopupState == null) {
            return new SourcePopupLayout(new ArrayList<String>(), new ArrayList<SourcePopupRow>(), 0);
        }

        String note;
        if (!sourcePopupState.hasMatchedEvent && sourcePopupState.hasMatchedValues) {
            note = "未匹配到最近执行日志，以下内容已回退为当前全局作用域里的最新值。";
        } else if (!sourcePopupState.hasMatchedEvent) {
            note = "未找到这个来源动作的最近执行日志，下面展示该来源会产生的子变量名。";
        } else if (sourcePopupState.hasMatchedValues) {
            note = "以下内容来自最近一次匹配来源动作的执行日志预览。点击可复制";
        } else {
            note = "已找到来源动作的执行记录，但日志预览里没有截取到这个变量前缀的子变量值。";
        }

        List<String> noteLines = wrapText(note, Math.max(120, wrapWidth), 3);
        List<SourcePopupRow> rows = new ArrayList<>();
        int offsetY = noteLines.isEmpty() ? 0 : noteLines.size() * 10 + 10;
        for (SourcePopupVariable variable : sourcePopupState.variables) {
            String displayValue = getSourcePopupDisplayValue(variable);
            List<String> valueLines = wrapText("值: " + displayValue, Math.max(100, wrapWidth - 18), 8);
            int height = Math.max(42, 20 + valueLines.size() * 10 + 10);
            rows.add(new SourcePopupRow(variable, valueLines, offsetY, height));
            offsetY += height + 8;
        }

        int contentHeight = offsetY;
        if (!rows.isEmpty()) {
            contentHeight -= 8;
        }
        return new SourcePopupLayout(noteLines, rows, Math.max(contentHeight, noteLines.size() * 10));
    }

    private void drawSourcePopup(int mouseX, int mouseY) {
        if (sourcePopupState == null) {
            return;
        }

        updateSourcePopupBounds();
        SourcePopupLayout layout = buildSourcePopupLayout(Math.max(120, sourcePopupViewportWidth - 20));
        maxSourcePopupScroll = Math.max(0, layout.contentHeight - Math.max(1, sourcePopupViewportHeight - 8));
        sourcePopupScrollOffset = clamp(sourcePopupScrollOffset, 0, maxSourcePopupScroll);

        drawRect(0, 0, this.width, this.height, 0x9A000000);
        drawCardShell(sourcePopupX, sourcePopupY, sourcePopupWidth, sourcePopupHeight,
                0xFF7AD9FF, 0xE01A2836, 0xF011161E, false, true);
        drawRect(sourcePopupX + 1, sourcePopupY + 1, sourcePopupX + sourcePopupWidth - 1, sourcePopupY + 24,
                0x18000000);

        ActionVariableRegistry.VariableSource source = sourcePopupState.source;
        drawString(fontRenderer, "子变量查看 · " + safe(sourcePopupState.variablePrefix),
                sourcePopupX + 12, sourcePopupY + 10, 0xFFFFFFFF);
        drawString(fontRenderer,
                fontRenderer.trimStringToWidth(
                        "来源: " + safe(source.getSequenceName()) + " · 步骤 " + (source.getStepIndex() + 1)
                                + " · 动作 " + (source.getActionIndex() + 1) + " · " + safe(source.getActionType()),
                        Math.max(120, sourcePopupWidth - 24)),
                sourcePopupX + 12, sourcePopupY + 22, 0xFFD8E8F7);
        drawString(fontRenderer,
                fontRenderer.trimStringToWidth(sourcePopupState.sessionSummary, Math.max(120, sourcePopupWidth - 24)),
                sourcePopupX + 12, sourcePopupY + 34, 0xFFB7CCE0);
        drawString(fontRenderer,
                fontRenderer.trimStringToWidth(sourcePopupState.eventSummary, Math.max(120, sourcePopupWidth - 24)),
                sourcePopupX + 12, sourcePopupY + 46, 0xFF9FB7CB);

        drawRect(sourcePopupViewportX, sourcePopupViewportY, sourcePopupViewportX + sourcePopupViewportWidth,
                sourcePopupViewportY + sourcePopupViewportHeight, 0x3D0E151C);
        beginScissor(sourcePopupViewportX, sourcePopupViewportY, sourcePopupViewportWidth, sourcePopupViewportHeight);

        int contentX = sourcePopupViewportX + 4;
        int contentY = sourcePopupViewportY + 4 - sourcePopupScrollOffset;
        int contentWidth = sourcePopupViewportWidth - 12;
        int noteY = contentY;
        for (String line : layout.noteLines) {
            drawString(fontRenderer, line, contentX + 4, noteY, 0xFFBED3E5);
            noteY += 10;
        }

        for (SourcePopupRow row : layout.rows) {
            int rowY = contentY + row.offsetY;
            if (rowY + row.height < sourcePopupViewportY + 1
                    || rowY > sourcePopupViewportY + sourcePopupViewportHeight - 1) {
                continue;
            }
            drawSourcePopupRow(row, contentX, rowY, contentWidth);
        }
        endScissor();

        if (maxSourcePopupScroll > 0) {
            drawPixelScrollBar(sourcePopupViewportX + sourcePopupViewportWidth - 6, sourcePopupViewportY + 2,
                    sourcePopupViewportHeight - 4, sourcePopupScrollOffset, maxSourcePopupScroll, layout.contentHeight);
        }

        drawString(fontRenderer, "点击变量名复制名称，点击值复制内容，Esc / 点击空白处关闭", sourcePopupX + 12,
                sourcePopupY + sourcePopupHeight - 14, 0xFF9FB3C6);
    }

    private void drawSourcePopupRow(SourcePopupRow row, int x, int y, int width) {
        int accent = row.variable.hasValue ? 0xFF69D3BC : 0xFF6B90AD;
        int top = row.variable.hasValue ? 0xBD153039 : 0xB51A2934;
        int bottom = row.variable.hasValue ? 0xD011171D : 0xD011161C;
        drawCardShell(x, y, width, row.height, accent, top, bottom, false, false);
        drawRect(x + 1, y + 1, x + 3, y + row.height - 1, accent);
        drawString(fontRenderer, fontRenderer.trimStringToWidth(row.variable.name, Math.max(80, width - 70)),
                x + 10, y + 7, 0xFFFFFFFF);
        String state = row.variable.hasValue ? "已记录" : "未记录";
        drawString(fontRenderer, state, x + width - 10 - fontRenderer.getStringWidth(state), y + 7,
                row.variable.hasValue ? 0xFF9EF0D4 : 0xFFC1D0DB);
        int lineY = y + 19;
        for (String line : row.valueLines) {
            drawString(fontRenderer, line, x + 10, lineY, row.variable.hasValue ? 0xFFDCECF7 : 0xFFB8C8D5);
            lineY += 10;
        }
    }

    private String getSourcePopupDisplayValue(SourcePopupVariable variable) {
        if (variable == null) {
            return "(暂无最近执行值)";
        }
        if (!variable.hasValue) {
            return "(暂无最近执行值)";
        }
        return safe(variable.value).trim().isEmpty() ? "(空)" : safe(variable.value);
    }

    private boolean handleSourcePopupCopyClick(int mouseX, int mouseY) {
        SourcePopupCopyHit hit = findSourcePopupCopyHit(mouseX, mouseY);
        if (hit == null || hit.variable == null) {
            return false;
        }

        if (!hit.valueTarget) {
            setClipboardString(hit.variable.name);
            statusText = "§a已复制子变量名: " + hit.variable.name;
            return true;
        }

        if (!hit.variable.hasValue) {
            statusText = "§e这个子变量当前没有最近执行值可复制。";
            return true;
        }

        setClipboardString(safe(hit.variable.value));
        statusText = safe(hit.variable.value).trim().isEmpty()
                ? "§a已复制子变量值: (空字符串)"
                : "§a已复制子变量值: " + safe(hit.variable.value);
        return true;
    }

    private SourcePopupCopyHit findSourcePopupCopyHit(int mouseX, int mouseY) {
        if (sourcePopupState == null) {
            return null;
        }
        if (!isHoverRegion(mouseX, mouseY, sourcePopupViewportX, sourcePopupViewportY,
                sourcePopupViewportWidth, sourcePopupViewportHeight)) {
            return null;
        }

        SourcePopupLayout layout = buildSourcePopupLayout(Math.max(120, sourcePopupViewportWidth - 20));
        int contentX = sourcePopupViewportX + 4;
        int contentY = sourcePopupViewportY + 4 - sourcePopupScrollOffset;
        int contentWidth = sourcePopupViewportWidth - 12;
        for (SourcePopupRow row : layout.rows) {
            int rowY = contentY + row.offsetY;
            if (!isHoverRegion(mouseX, mouseY, contentX, rowY, contentWidth, row.height)) {
                continue;
            }

            int nameX = contentX + 10;
            int nameY = rowY + 7;
            int visibleNameWidth = fontRenderer.getStringWidth(
                    fontRenderer.trimStringToWidth(row.variable.name, Math.max(80, contentWidth - 70)));
            if (isHoverRegion(mouseX, mouseY, nameX, nameY - 1, Math.max(1, visibleNameWidth), 10)) {
                return new SourcePopupCopyHit(row.variable, false);
            }

            int valueY = rowY + 19;
            int valueHeight = Math.max(10, row.valueLines.size() * 10);
            if (isHoverRegion(mouseX, mouseY, contentX + 10, valueY - 1, Math.max(1, contentWidth - 20), valueHeight + 2)) {
                return new SourcePopupCopyHit(row.variable, true);
            }
            return null;
        }
        return null;
    }

    private void drawFooterStatus() {
        int statusWidth = panelWidth - 220;
        drawRect(panelX + 12, footerButtonY - 16, panelX + 12 + statusWidth, footerButtonY - 2, 0x2AFFFFFF);
        drawString(fontRenderer, fontRenderer.trimStringToWidth(statusText, statusWidth - 4), panelX + 14,
                footerButtonY - 13, 0xFFB8C9D9);
    }

    private void drawDivider(int mouseX, int mouseY) {
        boolean hovered = isHoverRegion(mouseX, mouseY, dividerX - 2, dividerY, DIVIDER_WIDTH + 4, dividerHeight);
        int accent = draggingDivider ? 0xFF7CD9FF : (hovered ? 0xFF63BFEF : 0xFF3E617A);

        drawRect(dividerX, dividerY, dividerX + DIVIDER_WIDTH, dividerY + dividerHeight, 0x77111922);
        drawRect(dividerX + 5, dividerY + 18, dividerX + 7, dividerY + dividerHeight - 18, accent);

        int centerY = dividerY + dividerHeight / 2 - 12;
        for (int i = 0; i < 4; i++) {
            drawRect(dividerX + 3, centerY + i * 7, dividerX + DIVIDER_WIDTH - 3, centerY + i * 7 + 2, accent);
        }
    }

    private void drawPaneBackground(int x, int y, int width, int height, int accentColor, int topColor,
            int bottomColor) {
        drawCardShell(x, y, width, height, accentColor, topColor, bottomColor, false, false);
        drawRect(x + 1, y + 1, x + width - 1, y + 22, 0x12000000);
    }

    private void drawCardShell(int x, int y, int width, int height, int accentColor, int topColor, int bottomColor,
            boolean hovered, boolean emphasized) {
        int border = emphasized ? accentColor : mixColors(accentColor, 0xFF2F465B, 0.55F);
        if (hovered && !emphasized) {
            border = mixColors(accentColor, 0xFFFFFFFF, 0.45F);
        }

        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, border);
        fillVerticalGradient(x, y, x + width, y + height, topColor, bottomColor);
        drawRect(x + 1, y + 1, x + width - 1, y + 2, 0x20FFFFFF);

        if (hovered) {
            drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0x14FFFFFF);
        }
    }

    private void drawScrollBar(int x, int y, int height, int scrollOffset, int maxScroll, int totalItems,
            int visibleItems) {
        if (totalItems <= visibleItems || maxScroll <= 0 || height <= 0) {
            return;
        }
        int thumbHeight = Math.max(24, (int) (height * (visibleItems / (float) totalItems)));
        int travel = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) (travel * (scrollOffset / (float) maxScroll));
        GuiTheme.drawScrollbar(x, y, 4, height, thumbY, thumbHeight);
    }

    private void drawPixelScrollBar(int x, int y, int height, int scrollOffset, int maxScroll, int contentHeight) {
        if (maxScroll <= 0 || contentHeight <= 0 || height <= 0) {
            return;
        }
        int thumbHeight = Math.max(18, (int) ((float) Math.max(1, height - 8) / Math.max(height - 8, contentHeight)
                * height));
        int thumbY = y + (int) ((float) scrollOffset / maxScroll * Math.max(1, height - thumbHeight));
        GuiTheme.drawScrollbar(x, y, 4, height, thumbY, thumbHeight);
    }

    private int drawChipAlignedRight(int rightX, int y, String text, int borderColor, int bgColor) {
        String safeText = safe(text);
        int width = fontRenderer.getStringWidth(safeText) + 12;
        int x = rightX - width;
        drawRect(x - 1, y - 1, rightX + 1, y + 11, borderColor);
        drawRect(x, y, rightX, y + 10, bgColor);
        drawString(fontRenderer, safeText, x + 6, y + 2, 0xFFFFFFFF);
        return x;
    }

    private void fillVerticalGradient(int left, int top, int right, int bottom, int topColor, int bottomColor) {
        int height = Math.max(1, bottom - top);
        for (int i = 0; i < height; i++) {
            float progress = i / (float) Math.max(1, height - 1);
            int color = mixColors(topColor, bottomColor, progress);
            drawRect(left, top + i, right, top + i + 1, color);
        }
    }

    private int mixColors(int c1, int c2, float factor) {
        float t = Math.max(0.0F, Math.min(1.0F, factor));

        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private void beginScissor(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        ScaledResolution scaled = new ScaledResolution(mc);
        int scale = scaled.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + height)) * scale, width * scale, height * scale);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private List<String> wrapText(String text, int width, int maxLines) {
        List<String> result = new ArrayList<>();
        if (fontRenderer == null) {
            result.add(safe(text));
            return result;
        }

        List<String> raw = fontRenderer.listFormattedStringToWidth(safe(text), Math.max(20, width));
        if (raw == null || raw.isEmpty()) {
            result.add("");
            return result;
        }

        int limit = Math.max(1, maxLines);
        for (int i = 0; i < raw.size() && i < limit; i++) {
            String line = raw.get(i);
            if (i == limit - 1 && raw.size() > limit) {
                line = fontRenderer.trimStringToWidth(line, Math.max(20, width - 6)) + "...";
            }
            result.add(line);
        }
        return result;
    }

    private String trimToChip(String text, int width) {
        return fontRenderer.trimStringToWidth(safe(text), Math.max(16, width));
    }

    private int getTreeRowIndexForVariable(int variableIndex) {
        for (int i = 0; i < treeRows.size(); i++) {
            TreeRow row = treeRows.get(i);
            if (!row.groupRow && row.variableIndex == variableIndex) {
                return i;
            }
        }
        return 0;
    }

    private boolean isKnownScope(String scopeKey) {
        for (String scope : SCOPE_ORDER) {
            if (scope.equalsIgnoreCase(safe(scopeKey))) {
                return true;
            }
        }
        return false;
    }

    private int getFocusedSourceCount() {
        ActionVariableRegistry.VariableEntry entry = getFocusedEntry();
        return entry == null ? 0 : entry.getSources().size();
    }

    private boolean hasGlobalValue(String variableName) {
        if (variableName == null || variableName.trim().isEmpty() || globalValues == null) {
            return false;
        }
        String scopeKey = ActionVariableRegistry.extractScopeKey(variableName);
        String baseName = ActionVariableRegistry.extractBaseName(variableName);
        return "global".equals(scopeKey)
                && globalValues.containsKey(baseName)
                && globalValues.get(baseName) != null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (sourcePopupState != null) {
            updateSourcePopupBounds();
            if (mouseButton == 0) {
                if (isHoverRegion(mouseX, mouseY, sourcePopupX, sourcePopupY, sourcePopupWidth, sourcePopupHeight)) {
                    if (handleSourcePopupCopyClick(mouseX, mouseY)) {
                        return;
                    }
                } else {
                    closeSourcePopup();
                    return;
                }
            } else if (mouseButton == 1
                    && !isHoverRegion(mouseX, mouseY, sourcePopupX, sourcePopupY, sourcePopupWidth,
                            sourcePopupHeight)) {
                closeSourcePopup();
            }
            return;
        }

        if (mouseButton == 0
                && isHoverRegion(mouseX, mouseY, dividerX - 2, dividerY, DIVIDER_WIDTH + 4, dividerHeight)) {
            draggingDivider = true;
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0
                && isHoverRegion(mouseX, mouseY, listViewportX + listViewportWidth - 8, listViewportY, 10,
                        listViewportHeight)
                && maxListScroll > 0) {
            draggingListScrollbar = true;
            dragListScrollbar(mouseY);
            return;
        }

        if (mouseButton == 0
                && isHoverRegion(mouseX, mouseY, detailViewportX + detailViewportWidth - 8, detailViewportY, 10,
                        detailViewportHeight)
                && maxDetailScroll > 0) {
            draggingDetailScrollbar = true;
            dragDetailScrollbar(mouseY);
            return;
        }

        if (mouseX >= listViewportX && mouseX <= listViewportX + listViewportWidth
                && mouseY >= listViewportY && mouseY <= listViewportY + listViewportHeight) {
            int clickedRowIndex = (mouseY - listViewportY) / TREE_ROW_HEIGHT + listScrollOffset;
            if (clickedRowIndex >= 0 && clickedRowIndex < treeRows.size()) {
                TreeRow row = treeRows.get(clickedRowIndex);
                if (row.groupRow) {
                    if (collapsedScopeKeys.contains(row.scopeKey)) {
                        collapsedScopeKeys.remove(row.scopeKey);
                    } else {
                        collapsedScopeKeys.add(row.scopeKey);
                    }
                    rebuildTreeRows();
                    refreshScrollLimits();
                } else if (row.variableIndex >= 0 && row.variableIndex < variables.size()) {
                    boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                            || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
                    if (ctrl) {
                        if (selectedIndices.contains(row.variableIndex)) {
                            selectedIndices.remove(row.variableIndex);
                        } else {
                            selectedIndices.add(row.variableIndex);
                        }
                    } else {
                        selectedIndices.clear();
                        selectedIndices.add(row.variableIndex);
                    }
                    focusedIndex = row.variableIndex;
                    selectedIndices.add(row.variableIndex);
                    detailScrollOffset = 0;
                    refreshScrollLimits();
                    updateStatusText();
                }
            }
            return;
        }

        if (mouseButton == 0) {
            ActionVariableRegistry.VariableEntry entry = getFocusedEntry();
            int sourceIndex = getSourceIndexAt(mouseX, mouseY);
            if (entry != null && sourceIndex >= 0) {
                List<ActionVariableRegistry.VariableSource> sources = entry.getSources();
                if (sourceIndex < sources.size()) {
                    openSourcePopup(sources.get(sourceIndex));
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingDivider) {
            applyDividerDrag(mouseX);
            return;
        }
        if (draggingListScrollbar) {
            dragListScrollbar(mouseY);
            return;
        }
        if (draggingDetailScrollbar) {
            dragDetailScrollbar(mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingDivider = false;
        draggingListScrollbar = false;
        draggingDetailScrollbar = false;
    }

    private void applyDividerDrag(int mouseX) {
        int availablePaneWidth = workspaceWidth - DIVIDER_WIDTH;
        int leftWidth = clamp(mouseX - workspaceX - DIVIDER_WIDTH / 2, MIN_LEFT_PANE_WIDTH,
                Math.max(MIN_LEFT_PANE_WIDTH, availablePaneWidth
                        - Math.min(MIN_RIGHT_PANE_WIDTH, Math.max(160, availablePaneWidth - MIN_LEFT_PANE_WIDTH))));
        splitRatio = leftWidth / (double) Math.max(1, availablePaneWidth);
        savedSplitRatio = splitRatio;
        recalcLayout();
        updateStatusText();
        savePreferences();
    }

    private void dragListScrollbar(int mouseY) {
        float percent = (mouseY - listViewportY) / (float) Math.max(1, listViewportHeight);
        listScrollOffset = clamp(Math.round(percent * maxListScroll), 0, maxListScroll);
    }

    private void dragDetailScrollbar(int mouseY) {
        float percent = (mouseY - detailViewportY) / (float) Math.max(1, detailViewportHeight);
        detailScrollOffset = clamp(Math.round(percent * maxDetailScroll), 0, maxDetailScroll);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (sourcePopupState != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                closeSourcePopup();
            }
            return;
        }
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            reloadVariables(true);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }

        if (sourcePopupState != null) {
            updateSourcePopupBounds();
            SourcePopupLayout layout = buildSourcePopupLayout(Math.max(120, sourcePopupViewportWidth - 20));
            maxSourcePopupScroll = Math.max(0, layout.contentHeight - Math.max(1, sourcePopupViewportHeight - 8));
            sourcePopupScrollOffset = clamp(sourcePopupScrollOffset - Integer.signum(dWheel) * SOURCE_POPUP_SCROLL_STEP,
                    0, maxSourcePopupScroll);
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        if (isHoverRegion(mouseX, mouseY, listViewportX, listViewportY, listViewportWidth, listViewportHeight)
                && maxListScroll > 0) {
            listScrollOffset = dWheel > 0 ? Math.max(0, listScrollOffset - 1)
                    : Math.min(maxListScroll, listScrollOffset + 1);
            return;
        }

        if (isHoverRegion(mouseX, mouseY, rightPaneX, rightPaneY, rightPaneWidth, rightPaneHeight)
                && maxDetailScroll > 0) {
            detailScrollOffset = dWheel > 0 ? Math.max(0, detailScrollOffset - 1)
                    : Math.min(maxDetailScroll, detailScrollOffset + 1);
        }
    }

    private void drawScreenTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        if (isMouseOverField(mouseX, mouseY, searchField)) {
            tooltip = "实时按变量名过滤左侧卡片。";
        } else if (isHoverRegion(mouseX, mouseY, dividerX - 2, dividerY, DIVIDER_WIDTH + 4, dividerHeight)) {
            tooltip = "按住拖动，调整左右布局宽度。";
        } else if (isHoverRegion(mouseX, mouseY, listViewportX, listViewportY, listViewportWidth, listViewportHeight)) {
            tooltip = "点击分组可展开/收起，点击变量名可切换右侧来源档案。";
        } else if (isHoverRegion(mouseX, mouseY, detailViewportX, detailViewportY, detailViewportWidth,
                detailViewportHeight)) {
            tooltip = "左键点击来源卡片可查看子变量，滚轮可浏览全部来源卡片。";
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }

    private String formatGlobalValue(String variableName) {
        if (variableName == null || variableName.trim().isEmpty()) {
            return "(无)";
        }
        String scopeKey = ActionVariableRegistry.extractScopeKey(variableName);
        String baseName = ActionVariableRegistry.extractBaseName(variableName);
        if (!"global".equals(scopeKey)) {
            return "(" + ActionVariableRegistry.scopeKeyToDisplay(scopeKey) + " 的运行时值仅在脚本执行时可见)";
        }
        if (globalValues == null) {
            return "(当前未写入全局作用域)";
        }
        Object value = globalValues.get(baseName);
        if (value == null) {
            return "(当前未写入全局作用域)";
        }
        String text = String.valueOf(value);
        return text == null || text.trim().isEmpty() ? "(空)" : text;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
