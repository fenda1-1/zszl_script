package com.zszl.zszlScriptMod.otherfeatures.gui.common;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SimpleFeatureConfigScreen extends ThemedGuiScreen {

    protected static final int CONTROL_HEIGHT = 20;
    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;
    private static final String[] ON_OFF_OPTIONS = { "开启", "关闭" };

    protected final GuiScreen parentScreen;
    protected final String featureId;
    protected final String title;

    private final List<String> instructionLines = new ArrayList<>();
    private final List<String> descriptionLines = new ArrayList<>();

    private ConfigDropdown stateDropdown;
    private ConfigDropdown hudDropdown;
    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int stateHintY;
    private int stateDropdownY;
    private int hudHintY;
    private int hudDropdownY;
    private int infoTextStartY;
    private int infoRuntimeY;
    private int footerY;

    protected boolean draftEnabled;
    protected boolean draftStatusHudEnabled;

    protected SimpleFeatureConfigScreen(GuiScreen parentScreen, String featureId, String title) {
        this.parentScreen = parentScreen;
        this.featureId = featureId == null ? "" : featureId.trim();
        this.title = title == null ? getDefaultTitle() : title.trim();
    }

    protected static final class FeatureView {
        public final String name;
        public final String description;
        public final boolean enabled;
        public final boolean statusHudEnabled;
        public final boolean behaviorImplemented;
        public final String runtimeSummary;

        public FeatureView(String name, String description, boolean enabled, boolean statusHudEnabled,
                boolean behaviorImplemented, String runtimeSummary) {
            this.name = safe(name);
            this.description = safe(description);
            this.enabled = enabled;
            this.statusHudEnabled = statusHudEnabled;
            this.behaviorImplemented = behaviorImplemented;
            this.runtimeSummary = safe(runtimeSummary);
        }

        private static String safe(String text) {
            return text == null ? "" : text.trim();
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        FeatureView view = getFeatureView();
        this.draftEnabled = view != null && view.enabled;
        this.draftStatusHudEnabled = view == null || view.statusHudEnabled;

        rebuildText(view);
        computeLayout();
        initControls();
        refreshControlTexts();
    }

    private void rebuildText(FeatureView view) {
        this.instructionLines.clear();
        this.descriptionLines.clear();

        String instruction = view == null
                ? "这里可以调整当前功能的草稿状态与 HUD 显示，修改后通过底部按钮保存并返回。"
                : "左键其他功能页中的“" + view.name
                        + "”为总开关；这里可切换状态、配置单项状态 HUD，并查看当前运行状态。修改后通过底部按钮保存。";
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(instruction, 480));

        String description = view == null ? "§7当前功能信息不可用，无法读取说明。"
                : "§7" + (view.description.isEmpty() ? "暂无说明。" : view.description);
        this.descriptionLines.addAll(this.fontRenderer.listFormattedStringToWidth(description, 470));
    }

    private void computeLayout() {
        int textStep = this.fontRenderer.FONT_HEIGHT + 2;
        int instructionHeight = this.instructionLines.size() * textStep;
        int descriptionHeight = this.descriptionLines.size() * textStep;

        this.panelWidth = Math.min(540, Math.max(360, this.width - 12));
        int requiredHeight = 190 + instructionHeight + descriptionHeight;
        this.panelHeight = Math.min(Math.max(300, requiredHeight), Math.max(260, this.height - 10));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        int currentY = this.panelY + 24 + instructionHeight + 34;
        this.stateHintY = currentY;
        this.stateDropdownY = this.stateHintY + textStep + 4;
        this.hudHintY = this.stateDropdownY + CONTROL_HEIGHT + 10;
        this.hudDropdownY = this.hudHintY + textStep + 4;
        this.infoTextStartY = this.hudDropdownY + CONTROL_HEIGHT + 36;
        this.infoRuntimeY = this.infoTextStartY + descriptionHeight + 8;
        this.footerY = this.panelY + this.panelHeight - 32;
    }

    private void initControls() {
        int fieldX = this.panelX + 18;
        int fieldWidth = this.panelWidth - 36;

        this.stateDropdown = new ConfigDropdown(this, "功能状态", ON_OFF_OPTIONS);
        this.hudDropdown = new ConfigDropdown(this, "状态HUD", ON_OFF_OPTIONS);
        this.stateDropdown.setBounds(fieldX, this.stateDropdownY, fieldWidth, CONTROL_HEIGHT);
        this.hudDropdown.setBounds(fieldX, this.hudDropdownY, fieldWidth, CONTROL_HEIGHT);

        int gap = 6;
        int footerButtonW = Math.max(72, (this.panelWidth - 24 - gap * 2) / 3);
        int totalW = footerButtonW * 3 + gap * 2;
        int startX = this.panelX + (this.panelWidth - totalW) / 2;

        this.saveButton = new ThemedButton(BTN_SAVE, startX, this.footerY, footerButtonW, CONTROL_HEIGHT, "§a保存并关闭");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, startX + footerButtonW + gap, this.footerY, footerButtonW,
                CONTROL_HEIGHT, "§e恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, startX + (footerButtonW + gap) * 2, this.footerY, footerButtonW,
                CONTROL_HEIGHT, "取消");

        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.cancelButton);
    }

    private void refreshControlTexts() {
        this.stateDropdown.setSelectedIndex(this.draftEnabled ? 0 : 1);
        this.hudDropdown.setSelectedIndex(this.draftStatusHudEnabled ? 0 : 1);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case BTN_SAVE:
            saveDraftState(this.draftEnabled, this.draftStatusHudEnabled);
            this.mc.setScreen(this.parentScreen);
            return;
        case BTN_DEFAULT:
            resetFeatureState();
            FeatureView resetView = getFeatureView();
            this.draftEnabled = resetView != null && resetView.enabled;
            this.draftStatusHudEnabled = resetView == null || resetView.statusHudEnabled;
            refreshControlTexts();
            return;
        case BTN_CANCEL:
            this.mc.setScreen(this.parentScreen);
            return;
        default:
            break;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (handleMousePressed(mouseX, mouseY, mouseButton)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean handleMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.stateDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            this.hudDropdown.collapse();
            this.draftEnabled = this.stateDropdown.getSelectedIndex() == 0;
            return true;
        }
        if (this.hudDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            this.stateDropdown.collapse();
            this.draftStatusHudEnabled = this.hudDropdown.getSelectedIndex() == 0;
            return true;
        }
        if (mouseButton == 0 && handleButtonActivation(mouseX, mouseY)) {
            return true;
        }
        if (!this.stateDropdown.isMouseOver(mouseX, mouseY) && !this.hudDropdown.isMouseOver(mouseX, mouseY)) {
            this.stateDropdown.collapse();
            this.hudDropdown.collapse();
        }
        return false;
    }

    private boolean handleButtonActivation(int mouseX, int mouseY) throws IOException {
        for (GuiButton button : this.buttonList) {
            if (button == null || !button.visible || !button.enabled) {
                continue;
            }
            if (!isInside(mouseX, mouseY, button.x, button.y, button.width, button.height)) {
                continue;
            }
            button.playPressSound(this.mc.getSoundManager());
            actionPerformed(button);
            return true;
        }
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.setScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FeatureView view = getFeatureView();
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, resolveTitle(view), this.fontRenderer);
        drawInstructionText();
        drawStatusStrip(view);
        drawSectionBoxes();
        drawBody(view, mouseX, mouseY);
        drawButtons(mouseX, mouseY, partialTicks);
        drawTooltips(mouseX, mouseY);
    }

    private void drawInstructionText() {
        int textY = this.panelY + 22;
        int lineStep = this.fontRenderer.FONT_HEIGHT + 2;
        for (String line : this.instructionLines) {
            drawString(this.fontRenderer, line, this.panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += lineStep;
        }
    }

    private void drawStatusStrip(FeatureView view) {
        int stripX = this.panelX + 14;
        int stripY = this.panelY + 24 + this.instructionLines.size() * (this.fontRenderer.FONT_HEIGHT + 2) + 8;
        int stripW = this.panelWidth - 28;
        int stripH = 18;
        drawRect(stripX, stripY, stripX + stripW, stripY + stripH, 0x33202A36);
        drawHorizontalLine(stripX, stripX + stripW, stripY, 0x664FA6D9);
        drawHorizontalLine(stripX, stripX + stripW, stripY + stripH, 0x4435536C);

        String status = "状态: " + (this.draftEnabled ? "§a开启" : "§c关闭")
                + (view == null ? " §7| 功能: §c未找到" : " §7| 功能: §f" + view.name)
                + " §7| HUD: " + (this.draftStatusHudEnabled ? "§a开" : "§c关")
                + " §7| 逻辑: " + (view != null && view.behaviorImplemented ? "§a已接入" : "§6占位");
        drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(status, stripW - 12), stripX + 6, stripY + 5,
                0xFFFFFFFF);
    }

    private void drawSectionBoxes() {
        int left = this.panelX + 12;
        int right = this.panelX + this.panelWidth - 12;
        drawSectionBox("基础状态", left, this.stateHintY, right, this.hudDropdownY + CONTROL_HEIGHT + 10);
        drawSectionBox("状态提示", left, this.infoTextStartY, right, this.infoRuntimeY + this.fontRenderer.FONT_HEIGHT + 10);
    }

    private void drawBody(FeatureView view, int mouseX, int mouseY) {
        int textX = this.panelX + 18;
        drawString(this.fontRenderer, "§7切换后需用底部“保存并关闭”提交。", textX, this.stateHintY, 0xFFB6C5D6);
        this.stateDropdown.draw(mouseX, mouseY);
        drawString(this.fontRenderer, "§7单项 HUD 仍受顶部“总状态HUD”控制。", textX, this.hudHintY, 0xFFB6C5D6);
        this.hudDropdown.draw(mouseX, mouseY);

        int currentY = this.infoTextStartY;
        for (String line : this.descriptionLines) {
            drawString(this.fontRenderer, line, textX, currentY, 0xFFE2F2FF);
            currentY += this.fontRenderer.FONT_HEIGHT + 2;
        }

        String runtime = view == null ? "§6运行状态: 未找到对应功能" : "§e运行状态: §f" + safe(view.runtimeSummary, "待机");
        drawString(this.fontRenderer, runtime, textX, this.infoRuntimeY, 0xFFFFFFFF);
    }

    private void drawButtons(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton button : this.buttonList) {
            if (button != null && button.visible) {
                button.drawButton(this.mc, mouseX, mouseY, partialTicks);
            }
        }
    }

    private void drawTooltips(int mouseX, int mouseY) {
        if (this.stateDropdown.isMouseOver(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e功能状态",
                    "§7当前草稿: " + (this.draftEnabled ? "§a开启" : "§c关闭"),
                    "§7使用下拉框切换状态。"), mouseX, mouseY);
        } else if (this.hudDropdown.isMouseOver(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e状态HUD",
                    "§7当前草稿: " + (this.draftStatusHudEnabled ? "§a开启" : "§c关闭"),
                    "§7控制这个功能是否在 HUD 中显示状态。"), mouseX, mouseY);
        }
    }

    private void drawSectionBox(String title, int minX, int minY, int maxX, int maxY) {
        int boxX = minX - 6;
        int boxY = minY - 18;
        int boxW = (maxX - minX) + 12;
        int boxH = (maxY - minY) + 24;
        drawRect(boxX, boxY, boxX + boxW, boxY + boxH, 0x44202A36);
        drawHorizontalLine(boxX, boxX + boxW, boxY, 0xFF4FA6D9);
        drawHorizontalLine(boxX, boxX + boxW, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX, boxY, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX + boxW, boxY, boxY + boxH, 0xFF35536C);
        drawString(this.fontRenderer, "§b" + title, boxX + 6, boxY + 5, 0xFFE8F6FF);
    }

    private String resolveTitle(FeatureView view) {
        return view == null || view.name.isEmpty() ? this.title : view.name + "设置";
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    protected abstract FeatureView getFeatureView();

    protected abstract void saveDraftState(boolean enabled, boolean statusHudEnabled);

    protected abstract void resetFeatureState();

    protected abstract String getDefaultTitle();

    private static final class ConfigDropdown {

        private static final int ITEM_HEIGHT = 18;

        private final ThemedGuiScreen owner;
        private final String label;
        private final String[] options;

        private int x;
        private int y;
        private int width;
        private int height;
        private boolean expanded;
        private int selectedIndex;

        private ConfigDropdown(ThemedGuiScreen owner, String label, String[] options) {
            this.owner = owner;
            this.label = label == null ? "" : label;
            this.options = options == null ? new String[0] : options;
        }

        private void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private void setSelectedIndex(int selectedIndex) {
            if (this.options.length == 0) {
                this.selectedIndex = 0;
                return;
            }
            this.selectedIndex = Math.max(0, Math.min(selectedIndex, this.options.length - 1));
        }

        private int getSelectedIndex() {
            return this.selectedIndex;
        }

        private void collapse() {
            this.expanded = false;
        }

        private boolean isMouseOver(int mouseX, int mouseY) {
            if (isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
                return true;
            }
            return this.expanded && isInside(mouseX, mouseY, this.x, this.y + this.height + 2, this.width,
                    this.options.length * ITEM_HEIGHT);
        }

        private void draw(int mouseX, int mouseY) {
            GuiTheme.UiState state = isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)
                    ? GuiTheme.UiState.HOVER
                    : GuiTheme.UiState.NORMAL;
            GuiTheme.drawButtonFrameSafe(this.x, this.y, this.width, this.height, state);
            String text = this.label + ": " + (this.options.length == 0 ? "" : this.options[this.selectedIndex]);
            this.owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font),
                    new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font).trimStringToWidth(text, Math.max(10, this.width - 18)), this.x + 6,
                    this.y + 6, 0xFFFFFFFF);
            this.owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font), this.expanded ? "▲" : "▼", this.x + this.width - 10,
                    this.y + 6, 0xFF9FDFFF);

            if (!this.expanded) {
                return;
            }

            int menuY = this.y + this.height + 2;
            int totalHeight = this.options.length * ITEM_HEIGHT;
            Gui.drawRect(this.x, menuY, this.x + this.width, menuY + totalHeight, 0xEE111A22);
            Gui.drawRect(this.x, menuY, this.x + this.width, menuY + 1, 0xFF6FB8FF);
            Gui.drawRect(this.x, menuY + totalHeight - 1, this.x + this.width, menuY + totalHeight, 0xFF35536C);
            Gui.drawRect(this.x, menuY, this.x + 1, menuY + totalHeight, 0xFF35536C);
            Gui.drawRect(this.x + this.width - 1, menuY, this.x + this.width, menuY + totalHeight, 0xFF35536C);

            for (int i = 0; i < this.options.length; i++) {
                int itemY = menuY + i * ITEM_HEIGHT;
                boolean hovered = isInside(mouseX, mouseY, this.x, itemY, this.width, ITEM_HEIGHT);
                boolean selected = i == this.selectedIndex;
                if (selected || hovered) {
                    Gui.drawRect(this.x + 1, itemY, this.x + this.width - 1, itemY + ITEM_HEIGHT,
                            selected ? 0xCC2B5A7C : 0xAA2E4258);
                }
                this.owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font), this.options[i], this.x + 6, itemY + 5, 0xFFFFFFFF);
            }
        }

        private boolean handleClick(int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0) {
                return false;
            }
            if (isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
                this.expanded = !this.expanded;
                return true;
            }
            if (!this.expanded) {
                return false;
            }

            int menuY = this.y + this.height + 2;
            int totalHeight = this.options.length * ITEM_HEIGHT;
            if (isInside(mouseX, mouseY, this.x, menuY, this.width, totalHeight)) {
                int index = (mouseY - menuY) / ITEM_HEIGHT;
                if (index >= 0 && index < this.options.length) {
                    this.selectedIndex = index;
                }
                this.expanded = false;
                return true;
            }

            this.expanded = false;
            return false;
        }

        private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}









