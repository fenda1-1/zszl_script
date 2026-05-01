package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.path.template.ActionTemplateCatalog;
import com.zszl.zszlScriptMod.gui.path.template.ActionTemplateCatalog.ActionTemplate;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiActionTemplateLibrary extends ThemedGuiScreen {
    private static final int BTN_BACK = 0;
    private static final int BTN_INSERT_CURRENT_STEP = 1;
    private static final int BTN_INSERT_NEW_STEP = 2;
    private static final int BTN_NEW_CATEGORY = 3;
    private static final int BTN_NEW_TEMPLATE = 4;
    private static final int BTN_ADD_SELECTED_ACTIONS = 5;
    private static final int BTN_ADD_STEP_ACTIONS = 6;
    private static final int BTN_CLEAR_DRAFT = 7;
    private static final int BTN_SAVE_TEMPLATE = 8;
    private static final int BTN_DELETE_TEMPLATE = 9;
    private static final int SEARCH_FIELD_ID = 9300;
    private static final int DRAFT_CATEGORY_FIELD_ID = 9301;
    private static final int DRAFT_NAME_FIELD_ID = 9302;
    private static final int DRAFT_SUMMARY_FIELD_ID = 9303;
    private static final int DRAFT_USE_CASE_FIELD_ID = 9304;
    private static final int DRAFT_NOTE_FIELD_ID = 9305;
    private static final String ALL_CATEGORY = "全部";
    private static final int CATEGORY_ROW_HEIGHT = 24;
    private static final int TEMPLATE_CARD_HEIGHT = 66;
    private static final int TEMPLATE_CARD_GAP = 8;
    private static final int DETAIL_LINE_HEIGHT = 11;
    private static final int DETAIL_SCROLL_STEP = 24;
    private static final int DIVIDER_WIDTH = 12;
    private static final int DIVIDER_HIT_THICKNESS = 22;
    private static final int COLLAPSED_PANE_WIDTH = 34;
    private static final int MIN_LEFT_PANE_WIDTH = 140;
    private static final int MIN_TEMPLATE_PANE_WIDTH = 220;
    private static final int MIN_DETAIL_PANE_WIDTH = 170;
    private static final int CONTEXT_MENU_ITEM_HEIGHT = 18;
    private static double savedLeftPaneRatio = 0.18D;
    private static double savedTemplatePaneRatio = 0.32D;
    private static boolean leftPaneCollapsed = false;
    private static boolean templatePaneCollapsed = false;

    private static final class DetailLine {
        private final String text;
        private final int color;
        private final int height;
        private final int indent;
        private final boolean title;

        private DetailLine(String text, int color, int height, int indent, boolean title) {
            this.text = text == null ? "" : text;
            this.color = color;
            this.height = Math.max(1, height);
            this.indent = Math.max(0, indent);
            this.title = title;
        }
    }

    private static final class ContextMenuItem {
        private final String key;
        private final String label;
        private final boolean enabled;

        private ContextMenuItem(String key, String label, boolean enabled) {
            this.key = key == null ? "" : key;
            this.label = label == null ? "" : label;
            this.enabled = enabled;
        }
    }

    private final GuiPathManager parentScreen;
    private List<ActionTemplate> allTemplates = ActionTemplateCatalog.getTemplates();
    private final List<ActionTemplate> filteredTemplates = new ArrayList<ActionTemplate>();
    private final List<String> categories = new ArrayList<String>();
    private final String selectedSequenceName;
    private final String selectedStepText;
    private final boolean canInsertIntoStep;
    private final boolean canInsertAsNewStep;

    private GuiTextField searchField;
    private GuiButton btnBack;
    private GuiButton btnInsertCurrentStep;
    private GuiButton btnInsertNewStep;
    private GuiButton btnNewCategory;
    private GuiButton btnNewTemplate;
    private GuiButton btnAddSelectedActions;
    private GuiButton btnAddStepActions;
    private GuiButton btnClearDraft;
    private GuiButton btnSaveTemplate;
    private GuiButton btnDeleteTemplate;
    private GuiTextField draftCategoryField;
    private GuiTextField draftNameField;
    private GuiTextField draftSummaryField;
    private GuiTextField draftUseCaseField;
    private GuiTextField draftNoteField;

    private String selectedCategory = ALL_CATEGORY;
    private ActionTemplate selectedTemplate;
    private boolean draftMode = false;
    private final List<ActionData> draftActions = new ArrayList<ActionData>();
    private int categoryScroll = 0;
    private int templateScroll = 0;
    private int detailScroll = 0;
    private int draftActionScroll = 0;
    private int maxCategoryScroll = 0;
    private int maxTemplateScroll = 0;
    private int maxDetailScroll = 0;
    private int maxDraftActionScroll = 0;
    private String statusText = "选择模板后可查看说明，并插入到当前动作链。";
    private boolean draggingLeftDivider = false;
    private boolean draggingRightDivider = false;
    private boolean contextMenuVisible = false;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private int contextMenuWidth = 168;
    private String contextMenuTargetCategory = "";
    private ActionTemplate contextMenuTargetTemplate = null;
    private final List<ContextMenuItem> contextMenuItems = new ArrayList<ContextMenuItem>();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int footerY;
    private int leftPaneX;
    private int leftPaneY;
    private int leftPaneWidth;
    private int leftPaneHeight;
    private int categoryViewportX;
    private int categoryViewportY;
    private int categoryViewportWidth;
    private int categoryViewportHeight;
    private int templatePaneX;
    private int templatePaneY;
    private int templatePaneWidth;
    private int templatePaneHeight;
    private int templateViewportX;
    private int templateViewportY;
    private int templateViewportWidth;
    private int templateViewportHeight;
    private int detailPaneX;
    private int detailPaneY;
    private int detailPaneWidth;
    private int detailPaneHeight;
    private int detailViewportX;
    private int detailViewportY;
    private int detailViewportWidth;
    private int detailViewportHeight;
    private int draftActionViewportX;
    private int draftActionViewportY;
    private int draftActionViewportWidth;
    private int draftActionViewportHeight;
    private Rectangle leftDividerBounds;
    private Rectangle rightDividerBounds;
    private Rectangle leftPaneToggleBounds;
    private Rectangle templatePaneToggleBounds;

    public GuiActionTemplateLibrary(GuiPathManager parentScreen, PathSequence selectedSequence,
            PathStep selectedStep, int selectedStepIndex) {
        this.parentScreen = parentScreen;
        this.selectedSequenceName = selectedSequence == null ? "" : safe(selectedSequence.getName());
        this.selectedStepText = selectedStep == null || selectedStepIndex < 0 ? ""
                : "#" + (selectedStepIndex + 1) + " / 动作 " + selectedStep.getActions().size();
        this.canInsertIntoStep = selectedSequence != null && selectedStep != null;
        this.canInsertAsNewStep = selectedSequence != null;
        this.categories.add(ALL_CATEGORY);
        this.categories.addAll(ActionTemplateCatalog.getCategories());
        if (!allTemplates.isEmpty()) {
            this.selectedTemplate = allTemplates.get(0);
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        String previousSearch = searchField == null ? "" : searchField.getText();
        this.buttonList.clear();
        recalcLayout();

        searchField = new GuiTextField(SEARCH_FIELD_ID, this.fontRenderer, templatePaneX + 10, templatePaneY + 28,
                Math.max(20, templatePaneWidth - 20), 18);
        searchField.setMaxStringLength(80);
        searchField.setText(previousSearch);

        int buttonH = 20;
        btnBack = new ThemedButton(BTN_BACK, panelX + 12, footerY + 24, 68, buttonH, "返回");
        btnNewCategory = new ThemedButton(BTN_NEW_CATEGORY, 0, 0, 76, buttonH, "新建分类");
        btnNewTemplate = new ThemedButton(BTN_NEW_TEMPLATE, 0, 0, 76, buttonH, "新建模板");
        btnAddSelectedActions = new ThemedButton(BTN_ADD_SELECTED_ACTIONS, 0, 0, 118, buttonH, "加入选中动作");
        btnAddStepActions = new ThemedButton(BTN_ADD_STEP_ACTIONS, 0, 0, 104, buttonH, "加入当前步骤");
        btnClearDraft = new ThemedButton(BTN_CLEAR_DRAFT, 0, 0, 76, buttonH, "清空草稿");
        btnSaveTemplate = new ThemedButton(BTN_SAVE_TEMPLATE, 0, 0, 76, buttonH, "保存模板");
        btnDeleteTemplate = new ThemedButton(BTN_DELETE_TEMPLATE, 0, 0, 96, buttonH, "删除自定义");
        btnInsertCurrentStep = new ThemedButton(BTN_INSERT_CURRENT_STEP, panelX + panelWidth - 298, footerY + 24, 136,
                buttonH, "填入当前步骤");
        btnInsertNewStep = new ThemedButton(BTN_INSERT_NEW_STEP, panelX + panelWidth - 154, footerY + 24, 142, buttonH,
                "作为新步骤加入");
        this.buttonList.add(btnBack);
        this.buttonList.add(btnNewCategory);
        this.buttonList.add(btnNewTemplate);
        this.buttonList.add(btnAddSelectedActions);
        this.buttonList.add(btnAddStepActions);
        this.buttonList.add(btnClearDraft);
        this.buttonList.add(btnSaveTemplate);
        this.buttonList.add(btnDeleteTemplate);
        this.buttonList.add(btnInsertCurrentStep);
        this.buttonList.add(btnInsertNewStep);

        draftCategoryField = createDraftField(DRAFT_CATEGORY_FIELD_ID, previousDraftText(draftCategoryField));
        draftNameField = createDraftField(DRAFT_NAME_FIELD_ID, previousDraftText(draftNameField));
        draftSummaryField = createDraftField(DRAFT_SUMMARY_FIELD_ID, previousDraftText(draftSummaryField));
        draftUseCaseField = createDraftField(DRAFT_USE_CASE_FIELD_ID, previousDraftText(draftUseCaseField));
        draftNoteField = createDraftField(DRAFT_NOTE_FIELD_ID, previousDraftText(draftNoteField));
        updateControlLayout();

        refreshFilteredTemplates();
    }

    private void recalcLayout() {
        panelX = 10;
        panelY = 10;
        panelWidth = Math.max(520, this.width - 20);
        panelHeight = Math.max(320, this.height - 20);
        if (panelWidth > this.width - 16) {
            panelWidth = Math.max(1, this.width - 16);
            panelX = 8;
        }
        if (panelHeight > this.height - 16) {
            panelHeight = Math.max(1, this.height - 16);
            panelY = 8;
        }

        footerY = panelY + panelHeight - 54;
        int workspaceX = panelX + 10;
        int workspaceY = panelY + 60;
        int workspaceW = Math.max(220, panelWidth - 20);
        int workspaceH = Math.max(120, footerY - workspaceY - 8);
        int paneAvailableW = Math.max(120, workspaceW - DIVIDER_WIDTH * 2);

        leftPaneWidth = leftPaneCollapsed
                ? COLLAPSED_PANE_WIDTH
                : clamp((int) Math.round(paneAvailableW * savedLeftPaneRatio), MIN_LEFT_PANE_WIDTH,
                        Math.max(MIN_LEFT_PANE_WIDTH, paneAvailableW - MIN_TEMPLATE_PANE_WIDTH - MIN_DETAIL_PANE_WIDTH));
        templatePaneWidth = templatePaneCollapsed
                ? COLLAPSED_PANE_WIDTH
                : clamp((int) Math.round(paneAvailableW * savedTemplatePaneRatio), MIN_TEMPLATE_PANE_WIDTH,
                        Math.max(MIN_TEMPLATE_PANE_WIDTH, paneAvailableW - leftPaneWidth - MIN_DETAIL_PANE_WIDTH));
        detailPaneWidth = paneAvailableW - leftPaneWidth - templatePaneWidth;
        if (detailPaneWidth < MIN_DETAIL_PANE_WIDTH) {
            int shortage = MIN_DETAIL_PANE_WIDTH - detailPaneWidth;
            if (!templatePaneCollapsed) {
                int reduce = Math.min(shortage, Math.max(0, templatePaneWidth - MIN_TEMPLATE_PANE_WIDTH));
                templatePaneWidth -= reduce;
                shortage -= reduce;
            }
            if (shortage > 0 && !leftPaneCollapsed) {
                int reduce = Math.min(shortage, Math.max(0, leftPaneWidth - MIN_LEFT_PANE_WIDTH));
                leftPaneWidth -= reduce;
                shortage -= reduce;
            }
            detailPaneWidth = Math.max(COLLAPSED_PANE_WIDTH, paneAvailableW - leftPaneWidth - templatePaneWidth);
        }
        if (!leftPaneCollapsed) {
            savedLeftPaneRatio = leftPaneWidth / (double) Math.max(1, paneAvailableW);
        }
        if (!templatePaneCollapsed) {
            savedTemplatePaneRatio = templatePaneWidth / (double) Math.max(1, paneAvailableW);
        }

        leftPaneX = workspaceX;
        leftPaneY = workspaceY;
        leftPaneHeight = workspaceH;
        int leftDividerX = leftPaneX + leftPaneWidth;
        templatePaneX = leftDividerX + DIVIDER_WIDTH;
        templatePaneY = workspaceY;
        templatePaneHeight = workspaceH;
        int rightDividerX = templatePaneX + templatePaneWidth;
        detailPaneX = rightDividerX + DIVIDER_WIDTH;
        detailPaneY = workspaceY;
        detailPaneHeight = workspaceH;
        int dividerHitOffset = Math.max(0, (DIVIDER_HIT_THICKNESS - DIVIDER_WIDTH) / 2);
        leftDividerBounds = new Rectangle(leftDividerX - dividerHitOffset, workspaceY + 4, DIVIDER_HIT_THICKNESS,
                Math.max(40, workspaceH - 8));
        rightDividerBounds = new Rectangle(rightDividerX - dividerHitOffset, workspaceY + 4, DIVIDER_HIT_THICKNESS,
                Math.max(40, workspaceH - 8));

        categoryViewportX = leftPaneX + 8;
        categoryViewportY = leftPaneY + 30;
        categoryViewportWidth = Math.max(1, leftPaneWidth - 16);
        categoryViewportHeight = leftPaneHeight - 38;

        templateViewportX = templatePaneX + 8;
        templateViewportY = templatePaneY + 54;
        templateViewportWidth = Math.max(1, templatePaneWidth - 16);
        templateViewportHeight = templatePaneHeight - 62;

        detailViewportX = detailPaneX + 10;
        detailViewportY = detailPaneY + 98;
        detailViewportWidth = Math.max(1, detailPaneWidth - 20);
        detailViewportHeight = detailPaneHeight - 108;

        draftActionViewportX = detailPaneX + 10;
        draftActionViewportY = detailPaneY + 166;
        draftActionViewportWidth = Math.max(1, detailPaneWidth - 20);
        draftActionViewportHeight = Math.max(40, detailPaneHeight - 176);
    }

    private void updateControlLayout() {
        if (searchField != null) {
            searchField.x = templatePaneX + 10;
            searchField.y = templatePaneY + 28;
            searchField.width = Math.max(20, templatePaneWidth - 20);
            searchField.height = 18;
            if (templatePaneCollapsed) {
                searchField.setFocused(false);
            }
        }
        updateFooterButtonWidths();
        int gap = 6;
        int x = panelX + 12;
        int row1Y = footerY;
        layoutButton(btnNewCategory, x, row1Y);
        x += buttonWidth(btnNewCategory) + gap;
        layoutButton(btnNewTemplate, x, row1Y);
        x += buttonWidth(btnNewTemplate) + gap;
        layoutButton(btnAddSelectedActions, x, row1Y);
        x += buttonWidth(btnAddSelectedActions) + gap;
        layoutButton(btnAddStepActions, x, row1Y);
        x += buttonWidth(btnAddStepActions) + gap;
        layoutButton(btnClearDraft, x, row1Y);
        x += buttonWidth(btnClearDraft) + gap;
        layoutButton(btnSaveTemplate, x, row1Y);
        x += buttonWidth(btnSaveTemplate) + gap;
        layoutButton(btnDeleteTemplate, x, row1Y);
        if (btnBack != null) {
            btnBack.x = panelX + 12;
            btnBack.y = footerY + 24;
        }
        if (btnInsertCurrentStep != null && btnInsertNewStep != null) {
            btnInsertCurrentStep.x = panelX + panelWidth - btnInsertCurrentStep.width - btnInsertNewStep.width - gap
                    - 12;
            btnInsertCurrentStep.y = footerY + 24;
            btnInsertNewStep.x = panelX + panelWidth - btnInsertNewStep.width - 12;
            btnInsertNewStep.y = footerY + 24;
        }
        layoutDraftFields();
        updateButtonStates();
    }

    private void updateFooterButtonWidths() {
        boolean compact = compactFooterButtons();
        setButtonWidth(btnNewCategory, compact ? 58 : 76);
        setButtonWidth(btnNewTemplate, compact ? 58 : 76);
        setButtonWidth(btnAddSelectedActions, compact ? 76 : 118);
        setButtonWidth(btnAddStepActions, compact ? 68 : 104);
        setButtonWidth(btnClearDraft, compact ? 58 : 76);
        setButtonWidth(btnSaveTemplate, compact ? 58 : 76);
        setButtonWidth(btnDeleteTemplate, compact ? 72 : 96);
        setButtonWidth(btnBack, compact ? 58 : 68);
        setButtonWidth(btnInsertCurrentStep, compact ? 104 : 136);
        setButtonWidth(btnInsertNewStep, compact ? 108 : 142);
    }

    private boolean compactFooterButtons() {
        return panelWidth < 760;
    }

    private void setButtonWidth(GuiButton button, int width) {
        if (button != null) {
            button.width = width;
        }
    }

    private GuiTextField createDraftField(int id, String value) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 10, 18);
        field.setMaxStringLength(240);
        field.setText(value == null ? "" : value);
        return field;
    }

    private String previousDraftText(GuiTextField field) {
        return field == null ? "" : field.getText();
    }

    private int buttonWidth(GuiButton button) {
        return button == null ? 0 : button.width;
    }

    private void layoutButton(GuiButton button, int x, int y) {
        if (button != null) {
            button.x = x;
            button.y = y;
        }
    }

    private void layoutDraftFields() {
        int labelW = 64;
        int x = detailPaneX + 12 + labelW;
        int w = Math.max(80, detailPaneWidth - labelW - 26);
        layoutField(draftCategoryField, x, detailPaneY + 30, w);
        layoutField(draftNameField, x, detailPaneY + 54, w);
        layoutField(draftSummaryField, x, detailPaneY + 78, w);
        layoutField(draftUseCaseField, x, detailPaneY + 102, w);
        layoutField(draftNoteField, x, detailPaneY + 126, w);
    }

    private void layoutField(GuiTextField field, int x, int y, int width) {
        if (field != null) {
            field.x = x;
            field.y = y;
            field.width = width;
            field.height = 18;
            if (!draftMode) {
                field.setFocused(false);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
        updateDraftFieldCursor(draftCategoryField);
        updateDraftFieldCursor(draftNameField);
        updateDraftFieldCursor(draftSummaryField);
        updateDraftFieldCursor(draftUseCaseField);
        updateDraftFieldCursor(draftNoteField);
    }

    private void updateDraftFieldCursor(GuiTextField field) {
        if (field != null) {
            field.updateCursorCounter();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null) {
            return;
        }
        switch (button.id) {
            case BTN_BACK:
                this.mc.displayGuiScreen(parentScreen);
                return;
            case BTN_INSERT_CURRENT_STEP:
                insertIntoCurrentStep();
                return;
            case BTN_INSERT_NEW_STEP:
                insertAsNewStep();
                return;
            case BTN_NEW_CATEGORY:
                openNewCategoryInput();
                return;
            case BTN_NEW_TEMPLATE:
                startNewTemplateDraft(selectedCategory);
                return;
            case BTN_ADD_SELECTED_ACTIONS:
                addSelectedActionsToDraft();
                return;
            case BTN_ADD_STEP_ACTIONS:
                addSelectedStepActionsToDraft();
                return;
            case BTN_CLEAR_DRAFT:
                clearDraftActions();
                return;
            case BTN_SAVE_TEMPLATE:
                saveDraftTemplate();
                return;
            case BTN_DELETE_TEMPLATE:
                deleteSelectedCustomTemplate();
                return;
            default:
                break;
        }
    }

    private void insertIntoCurrentStep() {
        if (selectedTemplate == null) {
            statusText = "请先选择一个动作模板。";
            return;
        }
        if (!canInsertIntoStep) {
            statusText = "当前没有选中步骤，不能填入当前步骤。";
            updateButtonStates();
            return;
        }
        if (parentScreen.insertTemplateActionsIntoSelectedStep(selectedTemplate.getActions(), selectedTemplate.getName())) {
            this.mc.displayGuiScreen(parentScreen);
        } else {
            statusText = "插入失败：当前步骤或模板动作为空。";
            updateButtonStates();
        }
    }

    private void insertAsNewStep() {
        if (selectedTemplate == null) {
            statusText = "请先选择一个动作模板。";
            return;
        }
        if (!canInsertAsNewStep) {
            statusText = "当前没有选中序列，不能新增模板步骤。";
            updateButtonStates();
            return;
        }
        if (parentScreen.insertTemplateAsNewStep(selectedTemplate.getActions(), selectedTemplate.getName())) {
            this.mc.displayGuiScreen(parentScreen);
        } else {
            statusText = "插入失败：当前序列或模板动作为空。";
            updateButtonStates();
        }
    }

    private void openNewCategoryInput() {
        String initial = ALL_CATEGORY.equals(selectedCategory) ? "自定义" : selectedCategory;
        this.mc.displayGuiScreen(new GuiTextInput(this, "新建动作模板分类", initial, value -> {
            String category = safe(value).trim();
            if (ActionTemplateCatalog.addCustomCategory(category)) {
                reloadCatalog(category, null);
                statusText = "已新建自定义分类: " + category;
            } else {
                statusText = "分类创建失败：名称为空、已存在或属于内置分类。";
                updateButtonStates();
            }
        }));
    }

    private void openRenameCategoryInput(String category) {
        final String oldCategory = safe(category).trim();
        if (!ActionTemplateCatalog.isCustomCategory(oldCategory)) {
            statusText = "只能重命名自定义分类，内置分类不可修改。";
            updateButtonStates();
            return;
        }
        this.mc.displayGuiScreen(new GuiTextInput(this, "重命名动作模板分类", oldCategory, value -> {
            String newCategory = safe(value).trim();
            if (ActionTemplateCatalog.renameCustomCategory(oldCategory, newCategory)) {
                reloadCatalog(newCategory, null);
                statusText = "已重命名自定义分类: " + oldCategory + " -> " + newCategory;
            } else {
                statusText = "分类重命名失败：名称为空、已存在或属于内置分类。";
                updateButtonStates();
            }
        }));
    }

    private void startNewTemplateDraft(String category) {
        draftMode = true;
        draftActions.clear();
        draftActionScroll = 0;
        String draftCategory = normalizeDraftCategory(category);
        setFieldText(draftCategoryField, draftCategory);
        setFieldText(draftNameField, "自定义动作模板");
        setFieldText(draftSummaryField, "从当前路径动作生成的自定义模板。");
        setFieldText(draftUseCaseField, "适合复用当前选中动作链的同类场景。");
        setFieldText(draftNoteField, "保存前请按服务器实际菜单、文本、坐标和表达式调整参数。");
        statusText = "草稿模式：先 Ctrl/Shift 在路径管理器多选动作，再点“加入选中动作”。";
        updateButtonStates();
    }

    private void addSelectedActionsToDraft() {
        if (!draftMode) {
            startNewTemplateDraft(selectedCategory);
        }
        List<ActionData> actions = parentScreen.copySelectedActionsForTemplateDraft();
        if (actions.isEmpty()) {
            statusText = "当前没有选中动作。回到路径管理器后可 Ctrl/Shift 多选动作。";
            return;
        }
        draftActions.addAll(actions);
        draftActionScroll = Math.max(0, draftActions.size() - 1);
        statusText = "已加入 " + actions.size() + " 个选中动作，草稿当前 " + draftActions.size() + " 个动作。";
        updateButtonStates();
    }

    private void addSelectedStepActionsToDraft() {
        if (!draftMode) {
            startNewTemplateDraft(selectedCategory);
        }
        List<ActionData> actions = parentScreen.copySelectedStepActionsForTemplateDraft();
        if (actions.isEmpty()) {
            statusText = "当前步骤没有动作，不能加入模板草稿。";
            return;
        }
        draftActions.addAll(actions);
        draftActionScroll = Math.max(0, draftActions.size() - 1);
        statusText = "已加入当前步骤 " + actions.size() + " 个动作，草稿当前 " + draftActions.size() + " 个动作。";
        updateButtonStates();
    }

    private void clearDraftActions() {
        draftActions.clear();
        draftActionScroll = 0;
        statusText = "已清空模板草稿动作链。";
        updateButtonStates();
    }

    private void saveDraftTemplate() {
        if (!draftMode) {
            statusText = "请先点击“新建模板”进入草稿模式。";
            return;
        }
        String category = safe(draftCategoryField.getText()).trim();
        String name = safe(draftNameField.getText()).trim();
        if (name.isEmpty()) {
            statusText = "保存失败：模板名称不能为空。";
            return;
        }
        if (draftActions.isEmpty()) {
            statusText = "保存失败：模板动作链为空。";
            return;
        }
        ActionTemplate created = ActionTemplateCatalog.addCustomTemplate(category, name,
                draftSummaryField.getText(), draftUseCaseField.getText(), draftNoteField.getText(), draftActions);
        if (created == null) {
            statusText = "保存失败：模板名称或动作链无效。";
            return;
        }
        draftMode = false;
        draftActions.clear();
        draftActionScroll = 0;
        reloadCatalog(created.getCategory(), created.getId());
        statusText = "已保存自定义模板: " + created.getName();
        updateButtonStates();
    }

    private void deleteSelectedCustomTemplate() {
        if (selectedTemplate == null || !selectedTemplate.isCustom()) {
            statusText = "只能删除自定义模板，内置模板不可修改。";
            return;
        }
        String category = selectedTemplate.getCategory();
        if (ActionTemplateCatalog.deleteCustomTemplate(selectedTemplate.getId())) {
            reloadCatalog(category, null);
            statusText = "已删除自定义模板。";
        } else {
            statusText = "删除失败：模板不存在或不是自定义模板。";
        }
        updateButtonStates();
    }

    private void deleteCategory(String category) {
        if (ActionTemplateCatalog.deleteCustomCategory(category)) {
            reloadCatalog(ALL_CATEGORY, null);
            statusText = "已删除自定义分类及其中模板: " + category;
        } else {
            statusText = "只能删除自定义分类，内置分类不可修改。";
        }
        updateButtonStates();
    }

    private void reloadCatalog(String focusCategory, String focusTemplateId) {
        allTemplates = ActionTemplateCatalog.getTemplates();
        categories.clear();
        categories.add(ALL_CATEGORY);
        categories.addAll(ActionTemplateCatalog.getCategories());
        if (focusCategory != null && !focusCategory.trim().isEmpty() && categories.contains(focusCategory)) {
            selectedCategory = focusCategory;
        } else if (!categories.contains(selectedCategory)) {
            selectedCategory = ALL_CATEGORY;
        }
        refreshFilteredTemplates();
        if (focusTemplateId != null && !focusTemplateId.trim().isEmpty()) {
            for (ActionTemplate template : filteredTemplates) {
                if (focusTemplateId.equals(template.getId())) {
                    selectedTemplate = template;
                    detailScroll = 0;
                    break;
                }
            }
        }
    }

    private String normalizeDraftCategory(String category) {
        String value = safe(category).trim();
        return value.isEmpty() || ALL_CATEGORY.equals(value) ? "自定义" : value;
    }

    private void setFieldText(GuiTextField field, String value) {
        if (field != null) {
            field.setText(value == null ? "" : value);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (contextMenuVisible) {
                closeContextMenu();
                return;
            }
            if (draftMode && isAnyDraftFieldFocused()) {
                clearDraftFieldFocus();
                return;
            }
            this.mc.displayGuiScreen(parentScreen);
            return;
        }
        if (draftMode && typeDraftField(typedChar, keyCode)) {
            return;
        }
        if (keyCode == Keyboard.KEY_F
                && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            if (searchField != null) {
                if (templatePaneCollapsed) {
                    templatePaneCollapsed = false;
                    recalcLayout();
                    updateControlLayout();
                }
                searchField.setFocused(true);
            }
            return;
        }
        if (searchField != null && !templatePaneCollapsed && searchField.textboxKeyTyped(typedChar, keyCode)) {
            refreshFilteredTemplates();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean typeDraftField(char typedChar, int keyCode) {
        return textboxKeyTyped(draftCategoryField, typedChar, keyCode)
                || textboxKeyTyped(draftNameField, typedChar, keyCode)
                || textboxKeyTyped(draftSummaryField, typedChar, keyCode)
                || textboxKeyTyped(draftUseCaseField, typedChar, keyCode)
                || textboxKeyTyped(draftNoteField, typedChar, keyCode);
    }

    private boolean textboxKeyTyped(GuiTextField field, char typedChar, int keyCode) {
        return field != null && field.textboxKeyTyped(typedChar, keyCode);
    }

    private boolean isAnyDraftFieldFocused() {
        return isFocused(draftCategoryField) || isFocused(draftNameField) || isFocused(draftSummaryField)
                || isFocused(draftUseCaseField) || isFocused(draftNoteField);
    }

    private boolean isFocused(GuiTextField field) {
        return field != null && field.isFocused();
    }

    private void clearDraftFieldFocus() {
        setFocused(draftCategoryField, false);
        setFocused(draftNameField, false);
        setFocused(draftSummaryField, false);
        setFocused(draftUseCaseField, false);
        setFocused(draftNoteField, false);
    }

    private void setFocused(GuiTextField field, boolean focused) {
        if (field != null) {
            field.setFocused(focused);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (contextMenuVisible) {
            if (handleContextMenuClick(mouseX, mouseY, mouseButton)) {
                return;
            }
            closeContextMenu();
        }
        if (mouseButton == 1) {
            if (!leftPaneCollapsed && isInside(mouseX, mouseY, categoryViewportX, categoryViewportY, categoryViewportWidth,
                    categoryViewportHeight)) {
                int row = (mouseY - categoryViewportY) / CATEGORY_ROW_HEIGHT + categoryScroll;
                List<String> rows = getCategoryRows();
                String category = row >= 0 && row < rows.size() ? rows.get(row) : "";
                openCategoryContextMenu(mouseX, mouseY, category);
                return;
            }
            if (!templatePaneCollapsed && isInside(mouseX, mouseY, templateViewportX, templateViewportY, templateViewportWidth,
                    templateViewportHeight)) {
                ActionTemplate template = getTemplateAt(mouseY);
                if (template != null) {
                    selectedTemplate = template;
                    draftMode = false;
                    detailScroll = 0;
                }
                openTemplateContextMenu(mouseX, mouseY, template);
                return;
            }
        }
        if (mouseButton == 0 && leftPaneToggleBounds != null && leftPaneToggleBounds.contains(mouseX, mouseY)) {
            leftPaneCollapsed = !leftPaneCollapsed;
            recalcLayout();
            updateControlLayout();
            return;
        }
        if (mouseButton == 0 && templatePaneToggleBounds != null && templatePaneToggleBounds.contains(mouseX, mouseY)) {
            templatePaneCollapsed = !templatePaneCollapsed;
            recalcLayout();
            updateControlLayout();
            return;
        }
        if (mouseButton == 0 && leftDividerBounds != null && leftDividerBounds.contains(mouseX, mouseY)) {
            draggingLeftDivider = true;
            if (leftPaneCollapsed) {
                leftPaneCollapsed = false;
                recalcLayout();
                updateControlLayout();
            }
            return;
        }
        if (mouseButton == 0 && rightDividerBounds != null && rightDividerBounds.contains(mouseX, mouseY)) {
            draggingRightDivider = true;
            if (templatePaneCollapsed) {
                templatePaneCollapsed = false;
                recalcLayout();
                updateControlLayout();
            }
            return;
        }

        if (searchField != null && !templatePaneCollapsed) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (draftMode) {
            draftCategoryField.mouseClicked(mouseX, mouseY, mouseButton);
            draftNameField.mouseClicked(mouseX, mouseY, mouseButton);
            draftSummaryField.mouseClicked(mouseX, mouseY, mouseButton);
            draftUseCaseField.mouseClicked(mouseX, mouseY, mouseButton);
            draftNoteField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.mc.currentScreen != this || mouseButton != 0) {
            return;
        }
        if (!leftPaneCollapsed && isInside(mouseX, mouseY, categoryViewportX, categoryViewportY, categoryViewportWidth,
                categoryViewportHeight)) {
            int row = (mouseY - categoryViewportY) / CATEGORY_ROW_HEIGHT + categoryScroll;
            List<String> rows = getCategoryRows();
            if (row >= 0 && row < rows.size()) {
                selectedCategory = rows.get(row);
                categoryScroll = clamp(categoryScroll, 0, maxCategoryScroll);
                templateScroll = 0;
                refreshFilteredTemplates();
            }
            return;
        }
        if (!templatePaneCollapsed && isInside(mouseX, mouseY, templateViewportX, templateViewportY, templateViewportWidth,
                templateViewportHeight)) {
            int slot = (mouseY - templateViewportY) / (TEMPLATE_CARD_HEIGHT + TEMPLATE_CARD_GAP);
            int cardY = templateViewportY + slot * (TEMPLATE_CARD_HEIGHT + TEMPLATE_CARD_GAP);
            if (mouseY <= cardY + TEMPLATE_CARD_HEIGHT) {
                int index = templateScroll + slot;
                if (index >= 0 && index < filteredTemplates.size()) {
                    ActionTemplate next = filteredTemplates.get(index);
                    if (next != selectedTemplate) {
                        selectedTemplate = next;
                        draftMode = false;
                        detailScroll = 0;
                        updateButtonStates();
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int direction = dWheel > 0 ? -1 : 1;
        if (!leftPaneCollapsed && isInside(mouseX, mouseY, categoryViewportX, categoryViewportY, categoryViewportWidth,
                categoryViewportHeight)) {
            categoryScroll = clamp(categoryScroll + direction, 0, maxCategoryScroll);
        } else if (!templatePaneCollapsed && isInside(mouseX, mouseY, templateViewportX, templateViewportY, templateViewportWidth,
                templateViewportHeight)) {
            templateScroll = clamp(templateScroll + direction, 0, maxTemplateScroll);
        } else if (draftMode && isInside(mouseX, mouseY, draftActionViewportX, draftActionViewportY,
                draftActionViewportWidth, draftActionViewportHeight)) {
            draftActionScroll = clamp(draftActionScroll + direction, 0, maxDraftActionScroll);
        } else if (isInside(mouseX, mouseY, detailViewportX, detailViewportY, detailViewportWidth,
                detailViewportHeight)) {
            detailScroll = clamp(detailScroll + direction * DETAIL_SCROLL_STEP, 0, maxDetailScroll);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton == 0 && draggingLeftDivider) {
            applyLeftDividerDrag(mouseX);
            return;
        }
        if (clickedMouseButton == 0 && draggingRightDivider) {
            applyRightDividerDrag(mouseX);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            draggingLeftDivider = false;
            draggingRightDivider = false;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void applyLeftDividerDrag(int mouseX) {
        int workspaceX = panelX + 10;
        int paneAvailableW = Math.max(120, panelWidth - 20 - DIVIDER_WIDTH * 2);
        int minMiddle = templatePaneCollapsed ? COLLAPSED_PANE_WIDTH : MIN_TEMPLATE_PANE_WIDTH;
        int maxLeft = Math.max(MIN_LEFT_PANE_WIDTH, paneAvailableW - minMiddle - MIN_DETAIL_PANE_WIDTH);
        int nextLeft = clamp(mouseX - workspaceX, MIN_LEFT_PANE_WIDTH, maxLeft);
        savedLeftPaneRatio = nextLeft / (double) Math.max(1, paneAvailableW);
        recalcLayout();
        updateControlLayout();
    }

    private void applyRightDividerDrag(int mouseX) {
        int paneAvailableW = Math.max(120, panelWidth - 20 - DIVIDER_WIDTH * 2);
        int minLeft = leftPaneCollapsed ? COLLAPSED_PANE_WIDTH : MIN_LEFT_PANE_WIDTH;
        int minTemplate = MIN_TEMPLATE_PANE_WIDTH;
        int maxTemplate = Math.max(minTemplate, paneAvailableW - Math.max(minLeft, leftPaneWidth) - MIN_DETAIL_PANE_WIDTH);
        int nextTemplate = clamp(mouseX - templatePaneX, minTemplate, maxTemplate);
        savedTemplatePaneRatio = nextTemplate / (double) Math.max(1, paneAvailableW);
        recalcLayout();
        updateControlLayout();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawMainPanel(mouseX, mouseY);
        if (searchField != null && !templatePaneCollapsed) {
            searchField.drawTextBox();
            if (searchField.getText().trim().isEmpty() && !searchField.isFocused()) {
                drawString(fontRenderer, "搜索模板 / 动作类型 / 使用场景...", searchField.x + 4, searchField.y + 5,
                        0xFF7F93A8);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawFooterStatus();
        drawContextMenu(mouseX, mouseY);
    }

    private void drawMainPanel(int mouseX, int mouseY) {
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "动作模板库", this.fontRenderer);
        drawHeaderStrip();
        drawCategoryPane(mouseX, mouseY);
        drawDividerHandle(leftDividerBounds, mouseX, mouseY, draggingLeftDivider);
        drawTemplatePane(mouseX, mouseY);
        drawDividerHandle(rightDividerBounds, mouseX, mouseY, draggingRightDivider);
        drawDetailPane();
    }

    private void drawHeaderStrip() {
        int x = panelX + 12;
        int y = panelY + 27;
        int w = panelWidth - 24;
        int h = 24;
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, 0x884E7898);
        drawRect(x, y, x + w, y + h, 0x55223242);
        drawRect(x, y, x + 4, y + h, 0xFF56CCF2);
        String sequenceText = selectedSequenceName.trim().isEmpty()
                ? "当前序列: §c未选择"
                : "当前序列: §f" + selectedSequenceName;
        String stepText = selectedStepText.trim().isEmpty()
                ? "当前步骤: §c未选择"
                : "当前步骤: §f" + selectedStepText;
        String countText = "模板: §f" + allTemplates.size() + " §7(含自定义)";
        drawString(fontRenderer,
                fontRenderer.trimStringToWidth(sequenceText + "   §7|   " + stepText + "   §7|   " + countText,
                        w - 16),
                x + 12, y + 8, 0xFFD9E7F6);
    }

    private void drawCategoryPane(int mouseX, int mouseY) {
        drawPane(leftPaneX, leftPaneY, leftPaneWidth, leftPaneHeight, "模板分类");
        drawPaneToggle(leftPaneX, leftPaneY, leftPaneWidth, leftPaneCollapsed, true, mouseX, mouseY);
        if (leftPaneCollapsed) {
            drawCollapsedPaneHint(leftPaneX, leftPaneY, leftPaneWidth, leftPaneHeight, "分类");
            maxCategoryScroll = 0;
            categoryScroll = 0;
            return;
        }
        List<String> rows = getCategoryRows();
        int visibleRows = Math.max(1, categoryViewportHeight / CATEGORY_ROW_HEIGHT);
        maxCategoryScroll = Math.max(0, rows.size() - visibleRows);
        categoryScroll = clamp(categoryScroll, 0, maxCategoryScroll);

        beginScissor(categoryViewportX, categoryViewportY, categoryViewportWidth, categoryViewportHeight);
        for (int row = 0; row < visibleRows + 1; row++) {
            int index = categoryScroll + row;
            if (index >= rows.size()) {
                break;
            }
            String category = rows.get(index);
            int y = categoryViewportY + row * CATEGORY_ROW_HEIGHT;
            drawCategoryRow(category, y, mouseX, mouseY);
        }
        endScissor();

        drawScrollbar(categoryViewportX + categoryViewportWidth - 5, categoryViewportY + 1,
                categoryViewportHeight - 2, categoryScroll, maxCategoryScroll, rows.size(), visibleRows);
    }

    private void drawCategoryRow(String category, int y, int mouseX, int mouseY) {
        boolean selected = safe(category).equals(selectedCategory);
        boolean hovered = isInside(mouseX, mouseY, categoryViewportX, y, categoryViewportWidth - 6,
                CATEGORY_ROW_HEIGHT - 2);
        int accent = ALL_CATEGORY.equals(category) ? 0xFF56CCF2 : accentForCategory(category);
        int bg = selected ? 0xAA254B65 : (hovered ? 0x66294458 : 0x33202C3A);
        drawRect(categoryViewportX, y, categoryViewportX + categoryViewportWidth - 7, y + CATEGORY_ROW_HEIGHT - 3,
                bg);
        drawRect(categoryViewportX, y, categoryViewportX + 3, y + CATEGORY_ROW_HEIGHT - 3, accent);
        String label = fontRenderer.trimStringToWidth(category, Math.max(40, categoryViewportWidth - 52));
        drawString(fontRenderer, selected ? "§f" + label : "§b" + label, categoryViewportX + 9, y + 7,
                selected ? 0xFFFFFFFF : 0xFFD0E5F3);
        String count = String.valueOf(countTemplates(category));
        int countW = fontRenderer.getStringWidth(count) + 10;
        drawBadge(count, categoryViewportX + categoryViewportWidth - countW - 12, y + 5, countW, accent);
    }

    private void drawTemplatePane(int mouseX, int mouseY) {
        drawPane(templatePaneX, templatePaneY, templatePaneWidth, templatePaneHeight, "模板列表");
        drawPaneToggle(templatePaneX, templatePaneY, templatePaneWidth, templatePaneCollapsed, false, mouseX, mouseY);
        if (templatePaneCollapsed) {
            drawCollapsedPaneHint(templatePaneX, templatePaneY, templatePaneWidth, templatePaneHeight, "列表");
            maxTemplateScroll = 0;
            templateScroll = 0;
            return;
        }
        drawString(fontRenderer, "§7Ctrl+F 搜索，点击卡片查看详情", templatePaneX + 10, templatePaneY + 17,
                0xFF9FB2C8);

        int visibleCards = Math.max(1, templateViewportHeight / (TEMPLATE_CARD_HEIGHT + TEMPLATE_CARD_GAP));
        maxTemplateScroll = Math.max(0, filteredTemplates.size() - visibleCards);
        templateScroll = clamp(templateScroll, 0, maxTemplateScroll);

        drawRect(templateViewportX, templateViewportY, templateViewportX + templateViewportWidth,
                templateViewportY + templateViewportHeight, 0x33131C26);
        if (filteredTemplates.isEmpty()) {
            drawCenteredString(fontRenderer, "没有匹配的动作模板", templateViewportX + templateViewportWidth / 2,
                    templateViewportY + templateViewportHeight / 2 - 5, 0xFF9FB2C8);
        } else {
            beginScissor(templateViewportX, templateViewportY, templateViewportWidth, templateViewportHeight);
            for (int slot = 0; slot < visibleCards + 1; slot++) {
                int index = templateScroll + slot;
                if (index >= filteredTemplates.size()) {
                    break;
                }
                int y = templateViewportY + slot * (TEMPLATE_CARD_HEIGHT + TEMPLATE_CARD_GAP);
                drawTemplateCard(filteredTemplates.get(index), y, mouseX, mouseY);
            }
            endScissor();
        }
        drawScrollbar(templateViewportX + templateViewportWidth - 6, templateViewportY + 2,
                templateViewportHeight - 4, templateScroll, maxTemplateScroll, filteredTemplates.size(),
                visibleCards);
    }

    private void drawTemplateCard(ActionTemplate template, int y, int mouseX, int mouseY) {
        int x = templateViewportX + 4;
        int w = templateViewportWidth - 14;
        boolean selected = template == selectedTemplate;
        boolean hovered = isInside(mouseX, mouseY, x, y, w, TEMPLATE_CARD_HEIGHT);
        int accent = accentForCategory(template.getCategory());
        int border = selected ? 0xFF7ED0FF : (hovered ? 0xFF547895 : 0xFF30485D);
        int bg = selected ? 0xAA1E4258 : (hovered ? 0x88203344 : 0x6617232E);
        drawRect(x - 1, y - 1, x + w + 1, y + TEMPLATE_CARD_HEIGHT + 1, border);
        drawRect(x, y, x + w, y + TEMPLATE_CARD_HEIGHT, bg);
        drawRect(x, y, x + w, y + 2, accent);
        drawRect(x, y, x + 4, y + TEMPLATE_CARD_HEIGHT, accent);
        drawString(fontRenderer, fontRenderer.trimStringToWidth("§f" + template.getName(), w - 74), x + 10, y + 8,
                0xFFFFFFFF);
        String count = template.getActions().size() + " 动作";
        int countW = fontRenderer.getStringWidth(count) + 12;
        drawBadge(count, x + w - countW - 8, y + 6, countW, accent);
        if (template.isCustom()) {
            drawBadge("自定义", x + w - countW - 66, y + 6, 48, 0xFFFFB84D);
        }
        drawString(fontRenderer, fontRenderer.trimStringToWidth("§7" + template.getCategory(), w - 20),
                x + 10, y + 24, 0xFF9FB2C8);
        List<String> summaryLines = wrapText(template.getSummary(), w - 20, 2);
        int lineY = y + 39;
        for (String line : summaryLines) {
            drawString(fontRenderer, line, x + 10, lineY, 0xFFD3E6F5);
            lineY += DETAIL_LINE_HEIGHT;
        }
    }

    private void drawDetailPane() {
        drawPane(detailPaneX, detailPaneY, detailPaneWidth, detailPaneHeight, draftMode ? "创建模板" : "模板详情");
        if (draftMode) {
            drawDraftEditor();
            return;
        }
        if (selectedTemplate == null) {
            drawCenteredString(fontRenderer, "从左侧选择一个模板", detailPaneX + detailPaneWidth / 2,
                    detailPaneY + detailPaneHeight / 2 - 5, 0xFF9FB2C8);
            maxDetailScroll = 0;
            detailScroll = 0;
            return;
        }

        int accent = accentForCategory(selectedTemplate.getCategory());
        drawString(fontRenderer, fontRenderer.trimStringToWidth("§f§l" + selectedTemplate.getName(),
                detailPaneWidth - 24), detailPaneX + 12, detailPaneY + 30, 0xFFFFFFFF);
        drawBadge(selectedTemplate.getCategory(), detailPaneX + 12, detailPaneY + 48,
                fontRenderer.getStringWidth(selectedTemplate.getCategory()) + 14, accent);
        String actionCount = selectedTemplate.getActions().size() + " 个动作";
        drawBadge(actionCount, detailPaneX + 24 + fontRenderer.getStringWidth(selectedTemplate.getCategory()) + 14,
                detailPaneY + 48, fontRenderer.getStringWidth(actionCount) + 14, 0xFF3D95D4);
        if (selectedTemplate.isCustom()) {
            drawBadge("自定义", detailPaneX + detailPaneWidth - 64, detailPaneY + 48, 52, 0xFFFFB84D);
        }
        List<String> summaryLines = wrapText(selectedTemplate.getSummary(), detailPaneWidth - 24, 2);
        int summaryY = detailPaneY + 68;
        for (String line : summaryLines) {
            drawString(fontRenderer, line, detailPaneX + 12, summaryY, 0xFFD7E7F4);
            summaryY += DETAIL_LINE_HEIGHT;
        }

        List<DetailLine> lines = buildDetailLines(selectedTemplate, Math.max(80, detailViewportWidth - 14));
        int contentHeight = 0;
        for (DetailLine line : lines) {
            contentHeight += line.height;
        }
        maxDetailScroll = Math.max(0, contentHeight - detailViewportHeight + 8);
        detailScroll = clamp(detailScroll, 0, maxDetailScroll);

        drawRect(detailViewportX, detailViewportY, detailViewportX + detailViewportWidth,
                detailViewportY + detailViewportHeight, 0x33131C26);
        beginScissor(detailViewportX, detailViewportY, detailViewportWidth, detailViewportHeight);
        int y = detailViewportY + 6 - detailScroll;
        for (DetailLine line : lines) {
            if (y + line.height >= detailViewportY && y <= detailViewportY + detailViewportHeight) {
                if (line.title) {
                    drawRect(detailViewportX + 6, y + 1, detailViewportX + 9, y + line.height - 2, accent);
                }
                if (!line.text.isEmpty()) {
                    drawString(fontRenderer, line.text, detailViewportX + 12 + line.indent, y, line.color);
                }
            }
            y += line.height;
        }
        endScissor();
        drawScrollbar(detailViewportX + detailViewportWidth - 6, detailViewportY + 2,
                detailViewportHeight - 4, detailScroll, maxDetailScroll, Math.max(1, contentHeight),
                Math.max(1, detailViewportHeight));
    }

    private List<DetailLine> buildDetailLines(ActionTemplate template, int width) {
        List<DetailLine> lines = new ArrayList<DetailLine>();
        addTitle(lines, "使用场景");
        addWrapped(lines, template.getUseCase(), width, 0xFFE5F3FF, 0);
        addBlank(lines, 7);
        addTitle(lines, "备注");
        addWrapped(lines, template.getNote(), width, 0xFFFFE6A6, 0);
        addBlank(lines, 8);
        addTitle(lines, "动作链");
        List<ActionData> actions = template.getActions();
        for (int i = 0; i < actions.size(); i++) {
            ActionData action = actions.get(i);
            String type = action == null ? "" : safe(action.type);
            String desc = describeAction(action);
            String head = String.format(Locale.ROOT, "%02d. %s", i + 1, type);
            lines.add(new DetailLine("§f" + head, 0xFFFFFFFF, DETAIL_LINE_HEIGHT + 1, 0, false));
            addWrapped(lines, desc, width - 12, 0xFFBFD2E2, 12);
            addBlank(lines, 5);
        }
        return lines;
    }

    private void drawDraftEditor() {
        int labelX = detailPaneX + 12;
        drawString(fontRenderer, "§7来源: " + fontRenderer.trimStringToWidth(parentScreen.getTemplateDraftSourceText(),
                Math.max(60, detailPaneWidth - 62)), labelX, detailPaneY + 16, 0xFF9FB2C8);
        drawDraftLabel("分类", draftCategoryField);
        drawDraftLabel("名称", draftNameField);
        drawDraftLabel("摘要", draftSummaryField);
        drawDraftLabel("场景", draftUseCaseField);
        drawDraftLabel("备注", draftNoteField);
        drawDraftTextField(draftCategoryField);
        drawDraftTextField(draftNameField);
        drawDraftTextField(draftSummaryField);
        drawDraftTextField(draftUseCaseField);
        drawDraftTextField(draftNoteField);

        drawString(fontRenderer, "§b§l动作链草稿 §7(" + draftActions.size() + " 个动作)", labelX,
                draftActionViewportY - 13, 0xFFFFFFFF);
        drawRect(draftActionViewportX, draftActionViewportY, draftActionViewportX + draftActionViewportWidth,
                draftActionViewportY + draftActionViewportHeight, 0x33131C26);
        int rowH = 29;
        int visibleRows = Math.max(1, draftActionViewportHeight / rowH);
        maxDraftActionScroll = Math.max(0, draftActions.size() - visibleRows);
        draftActionScroll = clamp(draftActionScroll, 0, maxDraftActionScroll);
        if (draftActions.isEmpty()) {
            drawCenteredString(fontRenderer, "点击底部“加入选中动作”或“加入当前步骤”生成动作链",
                    draftActionViewportX + draftActionViewportWidth / 2,
                    draftActionViewportY + draftActionViewportHeight / 2 - 5, 0xFF9FB2C8);
        } else {
            beginScissor(draftActionViewportX, draftActionViewportY, draftActionViewportWidth,
                    draftActionViewportHeight);
            for (int row = 0; row < visibleRows + 1; row++) {
                int index = draftActionScroll + row;
                if (index >= draftActions.size()) {
                    break;
                }
                int y = draftActionViewportY + row * rowH;
                ActionData action = draftActions.get(index);
                drawRect(draftActionViewportX + 4, y + 3, draftActionViewportX + draftActionViewportWidth - 8,
                        y + rowH - 2, 0x6617232E);
                drawRect(draftActionViewportX + 4, y + 3, draftActionViewportX + 7, y + rowH - 2, 0xFF56CCF2);
                String head = String.format(Locale.ROOT, "%02d. %s", index + 1, action == null ? "" : safe(action.type));
                drawString(fontRenderer, "§f" + head, draftActionViewportX + 12, y + 7, 0xFFFFFFFF);
                drawString(fontRenderer,
                        fontRenderer.trimStringToWidth("§7" + describeAction(action),
                                Math.max(50, draftActionViewportWidth - 24)),
                        draftActionViewportX + 12, y + 18, 0xFFBFD2E2);
            }
            endScissor();
        }
        drawScrollbar(draftActionViewportX + draftActionViewportWidth - 6, draftActionViewportY + 2,
                draftActionViewportHeight - 4, draftActionScroll, maxDraftActionScroll, draftActions.size(),
                visibleRows);
    }

    private void drawDraftLabel(String label, GuiTextField field) {
        if (field != null) {
            drawString(fontRenderer, "§b" + label, detailPaneX + 12, field.y + 5, 0xFFEAF7FF);
        }
    }

    private void drawDraftTextField(GuiTextField field) {
        if (field != null) {
            field.drawTextBox();
        }
    }

    private void addTitle(List<DetailLine> lines, String text) {
        lines.add(new DetailLine("§b§l" + text, 0xFFFFFFFF, 15, 6, true));
    }

    private void addBlank(List<DetailLine> lines, int height) {
        lines.add(new DetailLine("", 0xFFFFFFFF, height, 0, false));
    }

    private void addWrapped(List<DetailLine> lines, String text, int width, int color, int indent) {
        List<String> wrapped = wrapText(text, Math.max(40, width - indent), 24);
        for (String line : wrapped) {
            lines.add(new DetailLine(line, color, DETAIL_LINE_HEIGHT, indent, false));
        }
    }

    private void drawFooterStatus() {
        int statusX = panelX + 92;
        int statusW = Math.max(80, btnInsertCurrentStep.x - statusX - 10);
        String text = buildStatusText();
        drawString(fontRenderer, fontRenderer.trimStringToWidth(text, statusW), statusX, footerY + 30, 0xFFBFD2E2);
    }

    private String buildStatusText() {
        if (!canInsertAsNewStep) {
            return "§e未选择序列：只能浏览模板，不能插入。";
        }
        if (!canInsertIntoStep) {
            return "§e未选择步骤：可作为新步骤加入，不能填入当前步骤。";
        }
        if (draftMode) {
            return "§b草稿模式：填写分类/名称/场景/备注，加入动作后保存为自定义模板。";
        }
        return statusText;
    }

    private void refreshFilteredTemplates() {
        String query = searchField == null ? "" : searchField.getText();
        String normalizedQuery = PinyinSearchHelper.normalizeQuery(query);
        filteredTemplates.clear();
        for (ActionTemplate template : allTemplates) {
            if (!ALL_CATEGORY.equals(selectedCategory) && !safe(template.getCategory()).equals(selectedCategory)) {
                continue;
            }
            if (!PinyinSearchHelper.matchesNormalized(template.getSearchText(), normalizedQuery)) {
                continue;
            }
            filteredTemplates.add(template);
        }

        ActionTemplate previous = selectedTemplate;
        if (!filteredTemplates.contains(selectedTemplate)) {
            selectedTemplate = filteredTemplates.isEmpty() ? null : filteredTemplates.get(0);
        }
        if (previous != selectedTemplate) {
            detailScroll = 0;
        }
        templateScroll = clamp(templateScroll, 0, maxTemplateScroll);
        statusText = filteredTemplates.isEmpty() ? "没有匹配的动作模板。" : "选择模板后可查看说明，并插入到当前动作链。";
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasTemplate = selectedTemplate != null;
        boolean compact = compactFooterButtons();
        if (btnInsertCurrentStep != null) {
            btnInsertCurrentStep.enabled = hasTemplate && !draftMode && canInsertIntoStep;
            String label = compact ? "填入步骤" : "填入当前步骤";
            btnInsertCurrentStep.displayString = (btnInsertCurrentStep.enabled ? "§a" : "§7") + label;
        }
        if (btnInsertNewStep != null) {
            btnInsertNewStep.enabled = hasTemplate && !draftMode && canInsertAsNewStep;
            String label = compact ? "新步骤加入" : "作为新步骤加入";
            btnInsertNewStep.displayString = (btnInsertNewStep.enabled ? "§b" : "§7") + label;
        }
        if (btnNewCategory != null) {
            btnNewCategory.displayString = "§b" + (compact ? "新分类" : "新建分类");
        }
        if (btnNewTemplate != null) {
            btnNewTemplate.displayString = "§6" + (compact ? "新模板" : "新建模板");
        }
        if (btnAddSelectedActions != null) {
            btnAddSelectedActions.enabled = true;
            btnAddSelectedActions.displayString = (draftMode ? "§a" : "§7")
                    + (compact ? "选中动作" : "加入选中动作");
        }
        if (btnAddStepActions != null) {
            btnAddStepActions.enabled = true;
            btnAddStepActions.displayString = (draftMode ? "§a" : "§7")
                    + (compact ? "当前步骤" : "加入当前步骤");
        }
        if (btnClearDraft != null) {
            btnClearDraft.enabled = draftMode;
            btnClearDraft.displayString = (draftMode ? "§e" : "§7") + (compact ? "清空" : "清空草稿");
        }
        if (btnSaveTemplate != null) {
            btnSaveTemplate.enabled = draftMode && !draftActions.isEmpty();
            btnSaveTemplate.displayString = (btnSaveTemplate.enabled ? "§a" : "§7")
                    + (compact ? "保存" : "保存模板");
        }
        if (btnDeleteTemplate != null) {
            btnDeleteTemplate.enabled = !draftMode && selectedTemplate != null && selectedTemplate.isCustom();
            btnDeleteTemplate.displayString = (btnDeleteTemplate.enabled ? "§c" : "§7")
                    + (compact ? "删除" : "删除自定义");
        }
    }

    private List<String> getCategoryRows() {
        return categories;
    }

    private int countTemplates(String category) {
        if (ALL_CATEGORY.equals(category)) {
            return allTemplates.size();
        }
        int count = 0;
        for (ActionTemplate template : allTemplates) {
            if (safe(template.getCategory()).equals(category)) {
                count++;
            }
        }
        return count;
    }

    private ActionTemplate getTemplateAt(int mouseY) {
        int slot = (mouseY - templateViewportY) / (TEMPLATE_CARD_HEIGHT + TEMPLATE_CARD_GAP);
        int cardY = templateViewportY + slot * (TEMPLATE_CARD_HEIGHT + TEMPLATE_CARD_GAP);
        if (mouseY > cardY + TEMPLATE_CARD_HEIGHT) {
            return null;
        }
        int index = templateScroll + slot;
        return index >= 0 && index < filteredTemplates.size() ? filteredTemplates.get(index) : null;
    }

    private void openCategoryContextMenu(int mouseX, int mouseY, String category) {
        contextMenuVisible = true;
        contextMenuX = mouseX;
        contextMenuY = mouseY;
        contextMenuTargetCategory = safe(category);
        contextMenuTargetTemplate = null;
        contextMenuItems.clear();
        contextMenuItems.add(new ContextMenuItem("new_category", "新建自定义分类", true));
        contextMenuItems.add(new ContextMenuItem("new_template", "在此分类新建模板", !ALL_CATEGORY.equals(category)));
        contextMenuItems.add(new ContextMenuItem("rename_category", "重命名自定义分类",
                ActionTemplateCatalog.isCustomCategory(category)));
        contextMenuItems.add(new ContextMenuItem("delete_category", "删除自定义分类",
                ActionTemplateCatalog.isCustomCategory(category)));
    }

    private void openTemplateContextMenu(int mouseX, int mouseY, ActionTemplate template) {
        contextMenuVisible = true;
        contextMenuX = mouseX;
        contextMenuY = mouseY;
        contextMenuTargetCategory = template == null ? selectedCategory : template.getCategory();
        contextMenuTargetTemplate = template;
        contextMenuItems.clear();
        contextMenuItems.add(new ContextMenuItem("new_template", "新建模板草稿", true));
        contextMenuItems.add(new ContextMenuItem("add_selected", "选中动作加入草稿", true));
        contextMenuItems.add(new ContextMenuItem("add_step", "当前步骤加入草稿", true));
        contextMenuItems.add(new ContextMenuItem("insert_current", "填入当前步骤",
                template != null && canInsertIntoStep));
        contextMenuItems.add(new ContextMenuItem("insert_new_step", "作为新步骤加入",
                template != null && canInsertAsNewStep));
        contextMenuItems.add(new ContextMenuItem("delete_template", "删除自定义模板",
                template != null && template.isCustom()));
    }

    private void closeContextMenu() {
        contextMenuVisible = false;
        contextMenuTargetCategory = "";
        contextMenuTargetTemplate = null;
        contextMenuItems.clear();
    }

    private boolean handleContextMenuClick(int mouseX, int mouseY, int mouseButton) {
        if (!contextMenuVisible) {
            return false;
        }
        int height = contextMenuItems.size() * CONTEXT_MENU_ITEM_HEIGHT + 4;
        int x = Math.min(contextMenuX, this.width - contextMenuWidth - 6);
        int y = Math.min(contextMenuY, this.height - height - 6);
        if (mouseX < x || mouseX > x + contextMenuWidth || mouseY < y || mouseY > y + height) {
            return false;
        }
        if (mouseButton != 0) {
            return true;
        }
        int index = (mouseY - (y + 2)) / CONTEXT_MENU_ITEM_HEIGHT;
        if (index < 0 || index >= contextMenuItems.size()) {
            closeContextMenu();
            return true;
        }
        ContextMenuItem item = contextMenuItems.get(index);
        String targetCategory = contextMenuTargetCategory;
        ActionTemplate targetTemplate = contextMenuTargetTemplate;
        closeContextMenu();
        if (!item.enabled) {
            return true;
        }
        handleContextMenuAction(item.key, targetCategory, targetTemplate);
        return true;
    }

    private void handleContextMenuAction(String key, String targetCategory, ActionTemplate targetTemplate) {
        if ("new_category".equals(key)) {
            openNewCategoryInput();
        } else if ("new_template".equals(key)) {
            startNewTemplateDraft(targetCategory);
        } else if ("rename_category".equals(key)) {
            openRenameCategoryInput(targetCategory);
        } else if ("delete_category".equals(key)) {
            deleteCategory(targetCategory);
        } else if ("delete_template".equals(key)) {
            if (targetTemplate != null) {
                selectedTemplate = targetTemplate;
            }
            deleteSelectedCustomTemplate();
        } else if ("add_selected".equals(key)) {
            addSelectedActionsToDraft();
        } else if ("add_step".equals(key)) {
            addSelectedStepActionsToDraft();
        } else if ("insert_current".equals(key)) {
            if (targetTemplate != null) {
                selectedTemplate = targetTemplate;
            }
            insertIntoCurrentStep();
        } else if ("insert_new_step".equals(key)) {
            if (targetTemplate != null) {
                selectedTemplate = targetTemplate;
            }
            insertAsNewStep();
        }
    }

    private void drawContextMenu(int mouseX, int mouseY) {
        if (!contextMenuVisible || contextMenuItems.isEmpty()) {
            return;
        }
        int height = contextMenuItems.size() * CONTEXT_MENU_ITEM_HEIGHT + 4;
        int x = Math.min(contextMenuX, this.width - contextMenuWidth - 6);
        int y = Math.min(contextMenuY, this.height - height - 6);
        drawRect(x, y, x + contextMenuWidth, y + height, 0xEE111A22);
        drawHorizontalLine(x, x + contextMenuWidth, y, 0xFF6FB8FF);
        drawHorizontalLine(x, x + contextMenuWidth, y + height, 0xFF35536C);
        drawVerticalLine(x, y, y + height, 0xFF35536C);
        drawVerticalLine(x + contextMenuWidth, y, y + height, 0xFF35536C);
        for (int i = 0; i < contextMenuItems.size(); i++) {
            ContextMenuItem item = contextMenuItems.get(i);
            int itemY = y + 2 + i * CONTEXT_MENU_ITEM_HEIGHT;
            boolean hovered = mouseX >= x + 2 && mouseX <= x + contextMenuWidth - 2
                    && mouseY >= itemY && mouseY <= itemY + CONTEXT_MENU_ITEM_HEIGHT - 1;
            if (hovered && item.enabled) {
                drawRect(x + 2, itemY, x + contextMenuWidth - 2, itemY + CONTEXT_MENU_ITEM_HEIGHT - 1, 0xCC2B5A7C);
            }
            drawString(fontRenderer, (item.enabled ? "§f" : "§7") + item.label, x + 8, itemY + 5,
                    item.enabled ? 0xFFEAF7FF : 0xFF778899);
        }
    }

    private String describeAction(ActionData action) {
        if (action == null) {
            return "";
        }
        try {
            return action.getDescription();
        } catch (Exception e) {
            return safe(action.type);
        }
    }

    private void drawPane(int x, int y, int width, int height, String title) {
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0x99476C88);
        drawRect(x, y, x + width, y + height, 0xAA17222E);
        drawRect(x, y, x + width, y + 22, 0x7730475F);
        drawRect(x, y + 22, x + width, y + 23, 0x664E7898);
        if (width > 70) {
            drawString(fontRenderer, "§b§l" + title, x + 9, y + 7, 0xFFFFFFFF);
        }
    }

    private void drawPaneToggle(int x, int y, int width, boolean collapsed, boolean leftPane, int mouseX, int mouseY) {
        int size = 14;
        Rectangle bounds = new Rectangle(x + Math.max(4, width - size - 6), y + 4, size, size);
        if (leftPane) {
            leftPaneToggleBounds = bounds;
        } else {
            templatePaneToggleBounds = bounds;
        }
        boolean hovered = bounds.contains(mouseX, mouseY);
        drawRect(bounds.x - 1, bounds.y - 1, bounds.x + bounds.width + 1, bounds.y + bounds.height + 1,
                hovered ? 0xFF7ED0FF : 0xFF3E617A);
        drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
                hovered ? 0xAA28465C : 0xAA182633);
        drawCenteredString(fontRenderer, collapsed ? ">" : "<", bounds.x + bounds.width / 2, bounds.y + 3,
                0xFFEAF7FF);
    }

    private void drawCollapsedPaneHint(int x, int y, int width, int height, String label) {
        drawString(fontRenderer, "§b" + label, x + 7, y + 28, 0xFFEAF7FF);
        drawString(fontRenderer, "§7展开", x + 5, y + 42, 0xFF9FB2C8);
        drawRect(x + width / 2 - 1, y + 62, x + width / 2 + 1, y + height - 12, 0x774E7898);
    }

    private void drawDividerHandle(Rectangle bounds, int mouseX, int mouseY, boolean dragging) {
        if (bounds == null) {
            return;
        }
        boolean hovered = bounds.contains(mouseX, mouseY);
        int accent = dragging ? 0xFF7CD9FF : (hovered ? 0xFF63BFEF : 0xFF3E617A);
        int actualX = bounds.x + Math.max(0, (bounds.width - DIVIDER_WIDTH) / 2);
        if (hovered || dragging) {
            drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0x35111922);
        }
        drawRect(actualX, bounds.y, actualX + DIVIDER_WIDTH, bounds.y + bounds.height, 0x77111922);
        drawRect(actualX + 5, bounds.y + 18, actualX + 7, bounds.y + bounds.height - 18, accent);
        int centerY = bounds.y + bounds.height / 2 - 12;
        for (int i = 0; i < 4; i++) {
            drawRect(actualX + 3, centerY + i * 7, actualX + DIVIDER_WIDTH - 3, centerY + i * 7 + 2, accent);
        }
    }

    private void drawBadge(String text, int x, int y, int width, int accent) {
        int w = Math.max(width, fontRenderer.getStringWidth(text) + 10);
        drawRect(x - 1, y - 1, x + w + 1, y + 11, accent);
        drawRect(x, y, x + w, y + 10, 0xCC15212B);
        drawString(fontRenderer, text, x + 5, y + 2, 0xFFEAF7FF);
    }

    private void drawScrollbar(int x, int y, int height, int scrollOffset, int maxScroll, int totalItems,
            int visibleItems) {
        if (maxScroll <= 0 || height <= 0) {
            return;
        }
        int thumbHeight = Math.max(10, (int) ((float) Math.max(1, visibleItems) / Math.max(1, totalItems) * height));
        thumbHeight = Math.min(height, thumbHeight);
        int thumbY = y + (int) ((float) scrollOffset / Math.max(1, maxScroll) * (height - thumbHeight));
        GuiTheme.drawScrollbar(x, y, 4, height, thumbY, thumbHeight);
    }

    private List<String> wrapText(String text, int width, int maxLines) {
        List<String> raw = fontRenderer.listFormattedStringToWidth(safe(text), Math.max(20, width));
        List<String> lines = new ArrayList<String>();
        if (raw == null || raw.isEmpty()) {
            lines.add("");
            return lines;
        }
        int limit = Math.max(1, maxLines);
        for (int i = 0; i < raw.size() && i < limit; i++) {
            String line = raw.get(i);
            if (i == limit - 1 && raw.size() > limit) {
                line = fontRenderer.trimStringToWidth(line, Math.max(20, width - 8)) + "...";
            }
            lines.add(line);
        }
        return lines;
    }

    private int accentForCategory(String category) {
        String value = safe(category);
        if (value.contains("背包") || value.contains("容器")) {
            return 0xFFFFB84D;
        }
        if (value.contains("NPC") || value.contains("传送")) {
            return 0xFF7DE38D;
        }
        if (value.contains("抓包") || value.contains("等待")) {
            return 0xFF8AA8FF;
        }
        if (value.contains("条件") || value.contains("闭环")) {
            return 0xFFFF7DAA;
        }
        return 0xFF56CCF2;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private void beginScissor(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        int scale = scaledResolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + height)) * scale, width * scale, height * scale);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }
}
