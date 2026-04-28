package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager.FeatureDef;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager.GroupDef;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiOtherFeatureSelector extends ThemedGuiScreen {

    private static final int BTN_CANCEL = 0;

    private final GuiScreen parentScreen;
    private final Consumer<FeatureDef> onSelect;
    private final String currentFeatureId;

    private List<GroupDef> groups = new ArrayList<>();
    private List<FeatureDef> featuresInGroup = new ArrayList<>();
    private int selectedGroupIndex = -1;

    private int groupScrollOffset = 0;
    private int featureScrollOffset = 0;
    private int maxGroupScroll = 0;
    private int maxFeatureScroll = 0;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listY;
    private int listHeight;
    private int groupListX;
    private int groupListWidth;
    private int featureListX;
    private int featureListWidth;
    private int itemHeight;
    private int bottomButtonY;
    private int bottomButtonW;
    private int bottomButtonH;

    public GuiOtherFeatureSelector(GuiScreen parentScreen, String currentFeatureId, Consumer<FeatureDef> onSelect) {
        this.parentScreen = parentScreen;
        this.currentFeatureId = currentFeatureId == null ? "" : currentFeatureId.trim();
        this.onSelect = onSelect;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        computeLayout();
        loadData();
        int centerX = this.width / 2;
        this.buttonList.add(new ThemedButton(BTN_CANCEL, centerX - bottomButtonW / 2, bottomButtonY, bottomButtonW,
                bottomButtonH, I18n.format("gui.common.cancel")));
    }

    private void loadData() {
        OtherFeatureGroupManager.reload();
        this.groups = OtherFeatureGroupManager.getGroups();
        this.selectedGroupIndex = resolveInitialGroupIndex();
        refreshFeatureList();
    }

    private int resolveInitialGroupIndex() {
        if (groups == null || groups.isEmpty()) {
            return -1;
        }
        if (!currentFeatureId.isEmpty()) {
            for (int i = 0; i < groups.size(); i++) {
                GroupDef group = groups.get(i);
                if (group == null || group.features == null) {
                    continue;
                }
                for (FeatureDef feature : group.features) {
                    if (feature != null && currentFeatureId.equalsIgnoreCase(feature.id)) {
                        return i;
                    }
                }
            }
        }
        return 0;
    }

    private void refreshFeatureList() {
        if (selectedGroupIndex < 0 || selectedGroupIndex >= groups.size()) {
            this.featuresInGroup = new ArrayList<>();
        } else {
            GroupDef group = groups.get(selectedGroupIndex);
            this.featuresInGroup = group == null || group.features == null ? new ArrayList<>() : new ArrayList<>(group.features);
        }
        this.featureScrollOffset = 0;
    }

    private float uiScale() {
        float sx = this.width / 430.0f;
        float sy = this.height / 280.0f;
        return Math.max(0.72f, Math.min(1.0f, Math.min(sx, sy)));
    }

    private int s(int base) {
        return Math.max(1, Math.round(base * uiScale()));
    }

    private void computeLayout() {
        panelWidth = Math.max(320, Math.min(s(420), this.width - s(20)));
        panelHeight = Math.max(200, Math.min(s(250), this.height - s(20)));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int pad = s(10);
        itemHeight = s(20);
        listY = panelY + s(30);
        bottomButtonH = s(20);
        bottomButtonW = s(140);
        bottomButtonY = panelY + panelHeight - bottomButtonH - s(10);
        listHeight = Math.max(s(90), bottomButtonY - listY - s(10));

        groupListX = panelX + pad;
        groupListWidth = Math.max(s(92), Math.min(s(120), panelWidth / 3));

        int gap = s(10);
        featureListX = groupListX + groupListWidth + gap;
        featureListWidth = panelX + panelWidth - pad - featureListX;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CANCEL) {
            finishSelection(null);
        }
    }

    private void finishSelection(FeatureDef feature) {
        if (onSelect != null) {
            onSelect.accept(feature);
        }
        if (mc.screen == this) {
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        drawCenteredString(fontRenderer, I18n.format("gui.path.select_other_feature"), this.width / 2, panelY + 10,
                0xFFFFFF);

        drawGroupList(mouseX, mouseY);
        drawFeatureList(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawGroupList(int mouseX, int mouseY) {
        GuiTheme.drawInputFrameSafe(groupListX, listY, groupListWidth, listHeight, false, true);

        int visibleItems = listHeight / itemHeight;
        maxGroupScroll = Math.max(0, groups.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + groupScrollOffset;
            if (index >= groups.size()) {
                break;
            }

            GroupDef group = groups.get(index);
            int itemY = listY + i * itemHeight;
            boolean selected = index == selectedGroupIndex;
            boolean hovered = isInside(mouseX, mouseY, groupListX, itemY, groupListWidth, itemHeight);

            GuiTheme.drawButtonFrameSafe(groupListX + 2, itemY + 1, groupListWidth - 4, itemHeight - 2,
                    resolveListState(selected, hovered));
            drawCenteredString(fontRenderer, group == null ? "" : group.name, groupListX + groupListWidth / 2,
                    itemY + (itemHeight - 8) / 2, 0xFFFFFFFF);
        }
    }

    private void drawFeatureList(int mouseX, int mouseY) {
        GuiTheme.drawInputFrameSafe(featureListX, listY, featureListWidth, listHeight, false, true);

        if (featuresInGroup.isEmpty()) {
            GuiTheme.drawEmptyState(featureListX + featureListWidth / 2, listY + 12,
                    I18n.format("gui.inventory.other_features.category_empty"), fontRenderer);
            return;
        }

        int visibleItems = listHeight / itemHeight;
        maxFeatureScroll = Math.max(0, featuresInGroup.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + featureScrollOffset;
            if (index >= featuresInGroup.size()) {
                break;
            }

            FeatureDef feature = featuresInGroup.get(index);
            int itemY = listY + i * itemHeight;
            boolean selected = feature != null && currentFeatureId.equalsIgnoreCase(feature.id);
            boolean hovered = isInside(mouseX, mouseY, featureListX, itemY, featureListWidth, itemHeight);

            GuiTheme.drawButtonFrameSafe(featureListX + 2, itemY + 1, featureListWidth - 4, itemHeight - 2,
                    resolveListState(selected, hovered));
            String display = feature == null ? "" : feature.name;
            if (feature != null && feature.id != null && !feature.id.trim().isEmpty()) {
                display = display + " §8(" + feature.id.trim() + ")";
            }
            drawCenteredString(fontRenderer, display, featureListX + featureListWidth / 2,
                    itemY + (itemHeight - 8) / 2, 0xFFFFFFFF);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (isInside(mouseX, mouseY, groupListX, listY, groupListWidth, listHeight)) {
            int clickedIndex = (mouseY - listY) / itemHeight + groupScrollOffset;
            if (clickedIndex >= 0 && clickedIndex < groups.size()) {
                selectedGroupIndex = clickedIndex;
                refreshFeatureList();
            }
            return;
        }

        if (isInside(mouseX, mouseY, featureListX, listY, featureListWidth, listHeight)) {
            int clickedIndex = (mouseY - listY) / itemHeight + featureScrollOffset;
            if (clickedIndex >= 0 && clickedIndex < featuresInGroup.size()) {
                finishSelection(featuresInGroup.get(clickedIndex));
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            finishSelection(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        if (isInside(mouseX, mouseY, groupListX, listY, groupListWidth, listHeight)) {
            groupScrollOffset = dWheel > 0
                    ? Math.max(0, groupScrollOffset - 1)
                    : Math.min(maxGroupScroll, groupScrollOffset + 1);
            return;
        }

        if (isInside(mouseX, mouseY, featureListX, listY, featureListWidth, listHeight)) {
            featureScrollOffset = dWheel > 0
                    ? Math.max(0, featureScrollOffset - 1)
                    : Math.min(maxFeatureScroll, featureScrollOffset + 1);
        }
    }

    private GuiTheme.UiState resolveListState(boolean selected, boolean hovered) {
        if (selected) {
            return GuiTheme.UiState.SELECTED;
        }
        return hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}








