// 文件: src/main/java/com/keycommand2/zszlScriptMod/gui/path/GuiCategoryManager.java
// (这是一个新文件)

package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.path.PathSequenceManager;

import java.io.IOException;
import java.util.List;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;

public class GuiCategoryManager extends ThemedGuiScreen {

    private static String defaultCategory() {
        return I18n.format("path.category.default");
    }

    private static String builtinCategory() {
        return I18n.format("path.category.builtin");
    }

    private final GuiScreen parentScreen;
    private List<String> categories;
    private int selectedCategoryIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private GuiButton btnRename, btnDelete;
    private GuiButton btnHideShow;

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 270;
    private static final int LIST_ROW_HEIGHT = 22;
    private static final int LIST_VISIBLE_ROWS = 6;
    private static final int LIST_INNER_PADDING = 8;

    public GuiCategoryManager(GuiScreen parent) {
        this.parentScreen = parent;
        this.categories = PathSequenceManager.getAllCategories();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = PANEL_WIDTH;

        this.buttonList
                .add(new ThemedButton(0, panelX + 12, panelY + 204, 84, 20, "§a" + I18n.format("gui.common.add")));
        btnRename = new ThemedButton(1, panelX + 108, panelY + 204, 84, 20, "§e" + I18n.format("gui.path.rename"));
        btnDelete = new ThemedButton(2, panelX + 204, panelY + 204, 84, 20,
                "§c" + I18n.format("gui.common.delete"));
        this.buttonList.add(btnRename);
        this.buttonList.add(btnDelete);

        btnHideShow = new ThemedButton(4, panelX + 12, panelY + 176, panelWidth - 24, 20,
                I18n.format("gui.path.category.hide"));
        this.buttonList.add(btnHideShow);

        this.buttonList.add(
                new ThemedButton(3, panelX + panelWidth / 2 - 50, panelY + PANEL_HEIGHT - 28, 100, 20,
                        I18n.format("gui.common.done")));

        updateButtonStates();
    }

    private int getPanelX() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int getPanelY() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private int getListX() {
        return getPanelX() + 12;
    }

    private int getListY() {
        return getPanelY() + 38;
    }

    private int getListWidth() {
        return PANEL_WIDTH - 24;
    }

    private int getListHeight() {
        return LIST_VISIBLE_ROWS * LIST_ROW_HEIGHT + 12;
    }

    private void updateButtonStates() {
        boolean isSelected = selectedCategoryIndex != -1;
        boolean isProtected = isSelected && defaultCategory().equals(categories.get(selectedCategoryIndex));
        btnRename.enabled = isSelected && !isProtected;
        btnDelete.enabled = isSelected && !isProtected;

        if (btnHideShow != null) {
            if (!isSelected) {
                btnHideShow.enabled = false;
                btnHideShow.displayString = I18n.format("gui.path.category.hide");
            } else {
                String category = categories.get(selectedCategoryIndex);
                btnHideShow.enabled = true;
                boolean hidden = PathSequenceManager.isCategoryHidden(category);
                btnHideShow.displayString = hidden
                        ? I18n.format("gui.path.category.show")
                        : I18n.format("gui.path.category.hide");
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // 新增
                mc.setScreen(new GuiTextInput(this, I18n.format("gui.path.input_new_category"), newName -> {
                    if (newName != null && !newName.trim().isEmpty()) {
                        PathSequenceManager.addCategory(newName.trim());
                        this.categories = PathSequenceManager.getAllCategories();
                        selectedCategoryIndex = categories.indexOf(newName.trim());
                    }
                    mc.setScreen(this);
                }));
                break;
            case 1: // 重命名
                if (btnRename.enabled) {
                    String oldName = categories.get(selectedCategoryIndex);
                    mc.setScreen(
                            new GuiTextInput(this, I18n.format("gui.path.input_rename_category"), oldName, newName -> {
                                if (newName != null && !newName.trim().isEmpty() && !oldName.equals(newName)) {
                                    PathSequenceManager.renameCategory(oldName, newName.trim());
                                    this.categories = PathSequenceManager.getAllCategories();
                                    selectedCategoryIndex = categories.indexOf(newName.trim());
                                }
                                mc.setScreen(this);
                            }));
                }
                break;
            case 2: // 删除
                if (btnDelete.enabled) {
                    PathSequenceManager.deleteCategory(categories.get(selectedCategoryIndex));
                    this.categories = PathSequenceManager.getAllCategories();
                    selectedCategoryIndex = -1;
                    updateButtonStates();
                }
                break;
            case 3: // 完成
                // ========修改开始==========
                // !! 核心修复：在返回父界面之前，将其标记为“脏”，以便刷新数据 !!
                if (this.parentScreen instanceof GuiPathManager) {
                    ((GuiPathManager) this.parentScreen).markDirty();
                }
                // ==========修改结束===========
                mc.setScreen(parentScreen);
                break;
            case 4: // 隐藏/显示
                if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categories.size()) {
                    String category = categories.get(selectedCategoryIndex);
                    boolean hidden = PathSequenceManager.isCategoryHidden(category);
                    PathSequenceManager.setCategoryHidden(category, !hidden);
                    this.categories = PathSequenceManager.getAllCategories();
                    updateButtonStates();
                }
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = PANEL_WIDTH;
        int panelHeight = PANEL_HEIGHT;
        int panelX = getPanelX();
        int panelY = getPanelY();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.path.manage_categories"), this.fontRenderer);

        int listX = getListX();
        int listY = getListY();
        int listWidth = getListWidth();
        int listHeight = getListHeight();
        int itemHeight = LIST_ROW_HEIGHT;
        int visibleItems = LIST_VISIBLE_ROWS;
        maxScroll = Math.max(0, categories.size() - visibleItems);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        drawRect(listX, listY, listX + listWidth, listY + listHeight, 0x33263546);
        GuiTheme.drawButtonFrame(listX, listY, listWidth, listHeight, GuiTheme.UiState.NORMAL);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + scrollOffset;
            if (index >= categories.size())
                break;

            String category = categories.get(index);
            String display = PathSequenceManager.isCategoryHidden(category)
                    ? category + " " + I18n.format("gui.path.category.hidden_tag")
                    : category;
            int itemY = listY + 6 + i * itemHeight;
            boolean isHovered = mouseX >= listX + 4 && mouseX <= listX + listWidth - 12 && mouseY >= itemY
                    && mouseY <= itemY + itemHeight - 2;
            GuiTheme.UiState state = index == selectedCategoryIndex
                    ? GuiTheme.UiState.SELECTED
                    : (isHovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            GuiTheme.drawButtonFrame(listX + 4, itemY, listWidth - 14, itemHeight - 2, state);
            this.drawCenteredString(fontRenderer,
                    fontRenderer.trimStringToWidth(display, listWidth - 24),
                    panelX + panelWidth / 2, itemY + (itemHeight - fontRenderer.FONT_HEIGHT) / 2 - 1, 0xFFFFFF);
        }

        if (maxScroll > 0) {
            int scrollbarX = listX + listWidth - 8;
            int scrollbarY = listY + 4;
            int scrollbarHeight = listHeight - 8;
            int thumbHeight = Math.max(12, (int) ((float) visibleItems / categories.size() * scrollbarHeight));
            int thumbY = scrollbarY
                    + (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 6, scrollbarHeight, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int listX = getListX();
        int listY = getListY();
        int listWidth = getListWidth();
        int listHeight = getListHeight();
        int itemHeight = LIST_ROW_HEIGHT;

        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - (listY + 6)) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < categories.size()) {
                selectedCategoryIndex = clickedIndex;
                updateButtonStates();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            if (dWheel > 0)
                scrollOffset = Math.max(0, scrollOffset - 1);
            else
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }
}






