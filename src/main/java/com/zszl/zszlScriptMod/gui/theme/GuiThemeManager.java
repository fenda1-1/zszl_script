// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/theme/GuiThemeManager.java
package com.zszl.zszlScriptMod.gui.theme;

import com.zszl.zszlScriptMod.config.ChatOptimizationConfig.ImageQuality;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.theme.ThemeConfigManager.ThemeProfile;
import com.zszl.zszlScriptMod.utils.ExternalLinkOpener;
import com.zszl.zszlScriptMod.utils.TextureManagerHelper;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config.GuiUtils;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config.GuiSlider;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuiThemeManager extends ThemedGuiScreen {

    private final GuiScreen parent;
    private List<ThemeProfile> profiles;

    // 左侧列表
    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int listScroll = 0, listMaxScroll = 0;
    private static final int ROW_H = 20;
    private final Set<Integer> selectedIndices = new LinkedHashSet<>();
    private int anchorIndex = -1;
    private int primarySelected = -1;

    // 右侧编辑滚动区
    private int editorX, editorY, editorW, editorH;
    private int editorContentW;
    private int editorScroll = 0, editorMaxScroll = 0;
    private int editorContentHeight = 760;
    private boolean draggingEditorScrollbar = false;
    private int editorDragStartMouseY = 0;
    private int editorDragStartScroll = 0;

    private boolean draggingListScrollbar = false;
    private int listDragStartMouseY = 0;
    private int listDragStartScroll = 0;

    // 随机草稿（不立即入列表）
    private ThemeProfile randomDraft = null;

    // 文本框
    private GuiTextField nameField;
    private GuiTextField panelImgField;
    private GuiTextField buttonImgField;
    private GuiTextField inputImgField;
    private GuiTextField panelScaleField;
    private GuiTextField buttonScaleField;
    private GuiTextField inputScaleField;
    private GuiTextField panelCropXField;
    private GuiTextField panelCropYField;
    private GuiTextField buttonCropXField;
    private GuiTextField buttonCropYField;
    private GuiTextField inputCropXField;
    private GuiTextField inputCropYField;

    // 滑块（颜色 + 透明度）
    private GuiSlider pR, pG, pB;
    private GuiSlider bR, bG, bB;
    private GuiSlider iR, iG, iB;
    private GuiSlider tR, tG, tB;
    private GuiSlider opPanel, opButton, opInput, opText;

    // 按钮
    private GuiButton btnDone, btnRandom, btnNew, btnDelete, btnApply, btnRightSave;
    private GuiButton panelQualityBtn, buttonQualityBtn, inputQualityBtn, btnWallpaperPage;

    public GuiThemeManager(GuiScreen parent) {
        this.parent = parent;
    }

    private void returnToMainMenuOverlay() {
        if (parent != null) {
            mc.setScreen(parent);
            return;
        }
        ThemeConfigManager.save();
        GuiInventory.openOverlayScreen();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        ThemeConfigManager.ensureLoaded();
        ThemeConfigManager.ensureDefaultPresets();
        profiles = ThemeConfigManager.getProfiles();

        panelW = Math.max(360, Math.min(980, this.width - 16));
        panelH = Math.max(260, Math.min(600, this.height - 16));
        if (panelW > this.width - 8) {
            panelW = Math.max(300, this.width - 8);
        }
        if (panelH > this.height - 8) {
            panelH = Math.max(220, this.height - 8);
        }
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        listX = panelX + 10;
        listY = panelY + 38;
        listW = Math.max(160, Math.min((int) (panelW * 0.34f), panelW - 220));
        listH = panelH - 80;

        editorX = listX + listW + 12;
        editorY = listY;
        editorW = panelX + panelW - editorX - 10;
        editorH = listH;
        editorContentW = Math.max(260, editorW - 10);
        editorContentW = Math.min(editorContentW, Math.max(220, editorW - 2));

        this.buttonList.clear();

        int bottomY = panelY + panelH - 28;
        int btnW = (listW - 18) / 4;
        btnDone = new ThemedButton(1000, listX, bottomY, btnW, 20, I18n.format("gui.common.done"));
        btnRandom = new ThemedButton(1001, listX + btnW + 6, bottomY, btnW, 20, I18n.format("gui.theme.random"));
        btnNew = new ThemedButton(1002, listX + 2 * (btnW + 6), bottomY, btnW, 20, I18n.format("gui.common.new"));
        btnDelete = new ThemedButton(1003, listX + 3 * (btnW + 6), bottomY, btnW, 20, I18n.format("gui.common.delete"));
        this.buttonList.add(btnDone);
        this.buttonList.add(btnRandom);
        this.buttonList.add(btnNew);
        this.buttonList.add(btnDelete);

        btnRightSave = new ThemedButton(1102, editorX + editorContentW - 100, panelY + panelH - 28, 100, 20,
                I18n.format("gui.common.save"));
        this.buttonList.add(btnRightSave);

        btnApply = new ThemedButton(1101, editorX, editorY, 120, 20, I18n.format("gui.theme.apply_selected"));
        this.buttonList.add(btnApply);

        int fieldW = Math.max(120, editorContentW - 126);
        int qualityW = Math.max(80, Math.min(120, editorContentW - fieldW - 6));

        nameField = makeField(2001, editorX, 42, editorContentW, 20, 64);
        panelImgField = makeField(2002, editorX, 78, fieldW, 20, 1024);
        buttonImgField = makeField(2003, editorX, 104, fieldW, 20, 1024);
        inputImgField = makeField(2004, editorX, 130, fieldW, 20, 1024);

        panelQualityBtn = new ThemedButton(1201, editorX + fieldW + 6, editorY + 78, qualityW, 20, "");
        buttonQualityBtn = new ThemedButton(1202, editorX + fieldW + 6, editorY + 104, qualityW, 20, "");
        inputQualityBtn = new ThemedButton(1203, editorX + fieldW + 6, editorY + 130, qualityW, 20, "");
        this.buttonList.add(panelQualityBtn);
        this.buttonList.add(buttonQualityBtn);
        this.buttonList.add(inputQualityBtn);

        btnWallpaperPage = new ThemedButton(1204, editorX + fieldW + 6, editorY + 154, qualityW, 20,
            "进入壁纸页挑选壁纸");
        this.buttonList.add(btnWallpaperPage);

        // --- 核心修改：重新布局缩放和裁剪输入框 ---
        int y_scale = 172;
        int fieldW_s = 52;
        panelScaleField = makeField(2011, editorX, y_scale, fieldW_s, 18, 4);
        buttonScaleField = makeField(2012, editorX + fieldW_s + 4, y_scale, fieldW_s, 18, 4);
        inputScaleField = makeField(2013, editorX + 2 * (fieldW_s + 4), y_scale, fieldW_s, 18, 4);

        int y_crop = y_scale + 40;
        int fieldW_c = 42;
        int group_gap = 10;
        int x_pos = editorX;
        panelCropXField = makeField(2021, x_pos, y_crop, fieldW_c, 18, 4);
        panelCropYField = makeField(2022, x_pos + fieldW_c + 4, y_crop, fieldW_c, 18, 4);
        x_pos += 2 * (fieldW_c + 4) + group_gap;
        buttonCropXField = makeField(2023, x_pos, y_crop, fieldW_c, 18, 4);
        buttonCropYField = makeField(2024, x_pos + fieldW_c + 4, y_crop, fieldW_c, 18, 4);
        x_pos += 2 * (fieldW_c + 4) + group_gap;
        inputCropXField = makeField(2025, x_pos, y_crop, fieldW_c, 18, 4);
        inputCropYField = makeField(2026, x_pos + fieldW_c + 4, y_crop, fieldW_c, 18, 4);
        // --- 修改结束 ---

        createSlidersFromValues(58, 90, 120, 47, 71, 95, 30, 36, 45, 242, 248, 255, 100, 100, 100, 100);

        primarySelected = Math.max(0, Math.min(ThemeConfigManager.getActiveIndex(), profiles.size() - 1));
        selectedIndices.clear();
        selectedIndices.add(primarySelected);
        anchorIndex = primarySelected;
        ensureSelectedVisible();
        bindProfileToEditor(getEditingTarget());
        refreshQualityBtnText();
        applyAutoButtonWidth();
    }

    private GuiTextField makeField(int id, int x, int yRel, int w, int h, int maxLen) {
        GuiTextField f = new GuiTextField(id, this.fontRenderer, x, editorY + yRel, w, h);
        f.setMaxStringLength(Integer.MAX_VALUE);
        return f;
    }

    private void createSlidersFromValues(int pr, int pg, int pb, int br, int bg, int bb, int ir, int ig, int ib,
            int tr, int tg, int tb, int opp, int opb, int opi, int opt) {
        removeSliderButtons();
        int colW = Math.max(72, (editorContentW - 20) / 3);
        int colGap = 10;

        int yColor = 230 + 40; // 向下移动以腾出空间
        pR = new GuiSlider(3001, editorX, editorY + yColor, colW, 20, "背景 R: ", "", 0, 255, pr, false, true);
        pG = new GuiSlider(3002, editorX, editorY + yColor + 24, colW, 20, "背景 G: ", "", 0, 255, pg, false, true);
        pB = new GuiSlider(3003, editorX, editorY + yColor + 48, colW, 20, "背景 B: ", "", 0, 255, pb, false, true);

        int bx = editorX + colW + colGap;
        bR = new GuiSlider(3011, bx, editorY + yColor, colW, 20, "按键 R: ", "", 0, 255, br, false, true);
        bG = new GuiSlider(3012, bx, editorY + yColor + 24, colW, 20, "按键 G: ", "", 0, 255, bg, false, true);
        bB = new GuiSlider(3013, bx, editorY + yColor + 48, colW, 20, "按键 B: ", "", 0, 255, bb, false, true);

        int ix = editorX + 2 * (colW + colGap);
        iR = new GuiSlider(3021, ix, editorY + yColor, colW, 20, "输入框 R: ", "", 0, 255, ir, false, true);
        iG = new GuiSlider(3022, ix, editorY + yColor + 24, colW, 20, "输入框 G: ", "", 0, 255, ig, false, true);
        iB = new GuiSlider(3023, ix, editorY + yColor + 48, colW, 20, "输入框 B: ", "", 0, 255, ib, false, true);

        int yText = yColor + 84;
        tR = new GuiSlider(3031, editorX, editorY + yText, colW, 20, "文本 R: ", "", 0, 255, tr, false, true);
        tG = new GuiSlider(3032, editorX, editorY + yText + 24, colW, 20, "文本 G: ", "", 0, 255, tg, false, true);
        tB = new GuiSlider(3033, editorX, editorY + yText + 48, colW, 20, "文本 B: ", "", 0, 255, tb, false, true);

        int yOp = yText;
        opPanel = new GuiSlider(3041, bx, editorY + yOp, colW, 20, "背景透明度: ", "%", 10, 100, opp, false, true);
        opButton = new GuiSlider(3042, bx, editorY + yOp + 24, colW, 20, "按键透明度: ", "%", 10, 100, opb, false,
                true);
        opInput = new GuiSlider(3043, bx, editorY + yOp + 48, colW, 20, "输入框透明度: ", "%", 10, 100, opi, false,
                true);
        opText = new GuiSlider(3044, ix, editorY + yOp, colW, 20, "文本透明度: ", "%", 10, 100, opt, false, true);

        this.buttonList.add(pR);
        this.buttonList.add(pG);
        this.buttonList.add(pB);
        this.buttonList.add(bR);
        this.buttonList.add(bG);
        this.buttonList.add(bB);
        this.buttonList.add(iR);
        this.buttonList.add(iG);
        this.buttonList.add(iB);
        this.buttonList.add(tR);
        this.buttonList.add(tG);
        this.buttonList.add(tB);
        this.buttonList.add(opPanel);
        this.buttonList.add(opButton);
        this.buttonList.add(opInput);
        this.buttonList.add(opText);
    }

    private void removeSliderButtons() {
        this.buttonList.remove(pR);
        this.buttonList.remove(pG);
        this.buttonList.remove(pB);
        this.buttonList.remove(bR);
        this.buttonList.remove(bG);
        this.buttonList.remove(bB);
        this.buttonList.remove(iR);
        this.buttonList.remove(iG);
        this.buttonList.remove(iB);
        this.buttonList.remove(tR);
        this.buttonList.remove(tG);
        this.buttonList.remove(tB);
        this.buttonList.remove(opPanel);
        this.buttonList.remove(opButton);
        this.buttonList.remove(opInput);
        this.buttonList.remove(opText);
    }

    private ThemeProfile getEditingTarget() {
        if (randomDraft != null) {
            return randomDraft;
        }
        if (primarySelected >= 0 && primarySelected < profiles.size()) {
            return profiles.get(primarySelected);
        }
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    private boolean isReadOnlyTarget() {
        return randomDraft == null && ThemeConfigManager.isBuiltInProfile(getEditingTarget());
    }

    private boolean hasDeletableSelection() {
        for (Integer idx : selectedIndices) {
            if (idx == null || idx < 0 || idx >= profiles.size()) {
                continue;
            }
            if (!ThemeConfigManager.isBuiltInProfile(profiles.get(idx))) {
                return true;
            }
        }
        return false;
    }

    private void updateEditorInteractivity() {
        boolean readOnly = isReadOnlyTarget();
        boolean canDelete = (randomDraft != null && selectedIndices.isEmpty()) || hasDeletableSelection();

        for (GuiTextField field : allFields()) {
            if (field != null) {
                field.setEnabled(!readOnly);
            }
        }

        setButtonEnabled(btnDelete, canDelete);
        setButtonEnabled(btnApply, !readOnly);
        setButtonEnabled(btnRightSave, !readOnly);
        setButtonEnabled(panelQualityBtn, !readOnly);
        setButtonEnabled(buttonQualityBtn, !readOnly);
        setButtonEnabled(inputQualityBtn, !readOnly);
        setButtonEnabled(btnWallpaperPage, !readOnly);

        setButtonEnabled(pR, !readOnly);
        setButtonEnabled(pG, !readOnly);
        setButtonEnabled(pB, !readOnly);
        setButtonEnabled(bR, !readOnly);
        setButtonEnabled(bG, !readOnly);
        setButtonEnabled(bB, !readOnly);
        setButtonEnabled(iR, !readOnly);
        setButtonEnabled(iG, !readOnly);
        setButtonEnabled(iB, !readOnly);
        setButtonEnabled(tR, !readOnly);
        setButtonEnabled(tG, !readOnly);
        setButtonEnabled(tB, !readOnly);
        setButtonEnabled(opPanel, !readOnly);
        setButtonEnabled(opButton, !readOnly);
        setButtonEnabled(opInput, !readOnly);
        setButtonEnabled(opText, !readOnly);
    }

    private void setButtonEnabled(GuiButton button, boolean enabled) {
        if (button != null) {
            button.enabled = enabled;
        }
    }

    private void bindProfileToEditor(ThemeProfile p) {
        if (p == null) {
            return;
        }
        nameField.setText(safe(p.name));
        panelImgField.setText(safe(p.panelImagePath));
        buttonImgField.setText(safe(p.buttonImagePath));
        inputImgField.setText(safe(p.inputImagePath));

        panelScaleField.setText(String.valueOf(p.panelImageScale));
        buttonScaleField.setText(String.valueOf(p.buttonImageScale));
        inputScaleField.setText(String.valueOf(p.inputImageScale));

        panelCropXField.setText(String.valueOf(p.panelCropX));
        panelCropYField.setText(String.valueOf(p.panelCropY));
        buttonCropXField.setText(String.valueOf(p.buttonCropX));
        buttonCropYField.setText(String.valueOf(p.buttonCropY));
        inputCropXField.setText(String.valueOf(p.inputCropX));
        inputCropYField.setText(String.valueOf(p.inputCropY));

        int pc = p.panelBorder;
        int bc = p.buttonBgNormal;
        int ic = p.inputBg;
        int tc = p.labelText;

        createSlidersFromValues((pc >> 16) & 0xFF, (pc >> 8) & 0xFF, pc & 0xFF,
                (bc >> 16) & 0xFF, (bc >> 8) & 0xFF, bc & 0xFF,
                (ic >> 16) & 0xFF, (ic >> 8) & 0xFF, ic & 0xFF,
                (tc >> 16) & 0xFF, (tc >> 8) & 0xFF, tc & 0xFF,
                clampOpacity(p.panelOpacityPercent), clampOpacity(p.buttonOpacityPercent),
                clampOpacity(p.inputOpacityPercent), clampOpacity(p.textOpacityPercent));

        refreshQualityBtnText();
        updateEditorInteractivity();
    }

    private void applyEditorToTarget(boolean saveNow) {
        ThemeProfile p = getEditingTarget();
        if (p == null) {
            return;
        }
        if (isReadOnlyTarget()) {
            updateEditorInteractivity();
            return;
        }

        p.name = nameField.getText().trim().isEmpty() ? I18n.format("gui.theme.unnamed") : nameField.getText().trim();

        int panelColor = toArgb(pR.getValueInt(), pG.getValueInt(), pB.getValueInt());
        int buttonColor = toArgb(bR.getValueInt(), bG.getValueInt(), bB.getValueInt());
        int inputColor = toArgb(iR.getValueInt(), iG.getValueInt(), iB.getValueInt());
        int textColor = toArgb(tR.getValueInt(), tG.getValueInt(), tB.getValueInt());

        p.panelBorder = panelColor;
        p.panelBgTop = withAlpha(panelColor, 0xD0);
        p.panelBgBottom = withAlpha(darken(panelColor, 36), 0xD0);
        p.titleLeft = panelColor;
        p.titleRight = lighten(panelColor, 28);

        p.buttonBgNormal = buttonColor;
        p.buttonBgHover = lighten(buttonColor, 20);
        p.buttonBgPressed = darken(buttonColor, 24);
        p.buttonBorderNormal = lighten(buttonColor, 18);
        p.buttonBorderHover = lighten(buttonColor, 45);

        p.inputBg = inputColor;
        p.inputBorder = lighten(inputColor, 20);
        p.inputBorderHover = lighten(inputColor, 50);

        p.titleText = textColor;
        p.labelText = textColor;
        p.subText = darken(textColor, 40);

        p.panelOpacityPercent = clampOpacity(opPanel.getValueInt());
        p.buttonOpacityPercent = clampOpacity(opButton.getValueInt());
        p.inputOpacityPercent = clampOpacity(opInput.getValueInt());
        p.textOpacityPercent = clampOpacity(opText.getValueInt());

        String panelRaw = panelImgField.getText() == null ? "" : panelImgField.getText().trim();
        String buttonRaw = buttonImgField.getText() == null ? "" : buttonImgField.getText().trim();
        String inputRaw = inputImgField.getText() == null ? "" : inputImgField.getText().trim();

        String panelPath = TextureManagerHelper.canonicalizeImagePath(panelRaw);
        String buttonPath = TextureManagerHelper.canonicalizeImagePath(buttonRaw);
        String inputPath = TextureManagerHelper.canonicalizeImagePath(inputRaw);
        p.panelImagePath = panelPath;
        p.buttonImagePath = buttonPath;
        p.inputImagePath = inputPath;

        if (!panelPath.equals(panelRaw)) {
            panelImgField.setText(panelPath);
        }
        if (!buttonPath.equals(buttonRaw)) {
            buttonImgField.setText(buttonPath);
        }
        if (!inputPath.equals(inputRaw)) {
            inputImgField.setText(inputPath);
        }

        p.panelImageScale = clamp(parseIntOr(panelScaleField.getText(), 100), 10, 300);
        p.buttonImageScale = clamp(parseIntOr(buttonScaleField.getText(), 100), 10, 300);
        p.inputImageScale = clamp(parseIntOr(inputScaleField.getText(), 100), 10, 300);

        p.panelCropX = Math.max(0, parseIntOr(panelCropXField.getText(), 0));
        p.panelCropY = Math.max(0, parseIntOr(panelCropYField.getText(), 0));
        p.buttonCropX = Math.max(0, parseIntOr(buttonCropXField.getText(), 0));
        p.buttonCropY = Math.max(0, parseIntOr(buttonCropYField.getText(), 0));
        p.inputCropX = Math.max(0, parseIntOr(inputCropXField.getText(), 0));
        p.inputCropY = Math.max(0, parseIntOr(inputCropYField.getText(), 0));

        if (randomDraft == null && primarySelected >= 0 && primarySelected < profiles.size()) {
            ThemeConfigManager.setActiveIndex(primarySelected);
        }

        GuiTheme.applyProfile(p);

        if (saveNow) {
            if (randomDraft != null) {
                ThemeConfigManager.addProfile(randomDraft.copy());
                randomDraft = null;
                profiles = ThemeConfigManager.getProfiles();
                primarySelected = ThemeConfigManager.getActiveIndex();
                selectedIndices.clear();
                selectedIndices.add(primarySelected);
                anchorIndex = primarySelected;
            }
            ThemeConfigManager.save();
        }
    }

    private void refreshQualityBtnText() {
        ThemeProfile p = getEditingTarget();
        if (p == null) {
            return;
        }
        panelQualityBtn.displayString = I18n.format("gui.theme.panel_quality", safe(p.panelImageQuality));
        buttonQualityBtn.displayString = I18n.format("gui.theme.button_quality", safe(p.buttonImageQuality));
        inputQualityBtn.displayString = I18n.format("gui.theme.input_quality", safe(p.inputImageQuality));
        applyAutoButtonWidth();
    }

    private void applyAutoButtonWidth() {
        autoWidth(btnApply, 120, editorContentW / 2);
        autoWidth(panelQualityBtn, 120, 180);
        autoWidth(buttonQualityBtn, 120, 180);
        autoWidth(inputQualityBtn, 120, 180);
    }

    private void autoWidth(GuiButton b, int minW, int maxW) {
        int w = this.fontRenderer.getStringWidth(b.displayString) + 16;
        b.width = Math.max(minW, Math.min(maxW, w));
    }

    private String nextThemeName(String base) {
        String candidate = base;
        int idx = 2;
        while (containsThemeName(candidate)) {
            candidate = base + " " + idx++;
        }
        return candidate;
    }

    private boolean containsThemeName(String name) {
        for (ThemeProfile p : profiles) {
            if (p != null && p.name != null && p.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1000:
                returnToMainMenuOverlay();
                return;
            case 1001:
                randomDraft = ThemeConfigManager
                        .createRandomProfile(I18n.format("gui.theme.random_name", profiles.size() + 1));
                bindProfileToEditor(randomDraft);
                applyEditorToTarget(false);
                TextureManagerHelper.clearCache();
                return;
            case 1002:
                applyEditorToTarget(false);
                ThemeProfile src = getEditingTarget();
                if (src != null) {
                    ThemeProfile copy = src.copy();
                    String base = copy.name == null || copy.name.trim().isEmpty()
                            ? I18n.format("gui.theme.unnamed")
                            : copy.name.trim();
                    copy.name = nextThemeName(base);
                    copy.builtIn = false;
                    copy.builtInId = "";
                    ThemeConfigManager.addProfile(copy);
                    // 新增后应切回真实列表项编辑，避免继续改到随机草稿导致不持久化
                    randomDraft = null;
                    profiles = ThemeConfigManager.getProfiles();
                    primarySelected = ThemeConfigManager.getActiveIndex();
                    selectedIndices.clear();
                    selectedIndices.add(primarySelected);
                    anchorIndex = primarySelected;
                    bindProfileToEditor(getEditingTarget());
                    ThemeConfigManager.save();
                }
                TextureManagerHelper.clearCache();
                return;
            case 1102:
                applyEditorToTarget(true);
                TextureManagerHelper.clearCache();
                return;
            case 1003:
                if (randomDraft != null && selectedIndices.isEmpty()) {
                    randomDraft = null;
                    bindProfileToEditor(getEditingTarget());
                } else {
                    List<Integer> del = new ArrayList<>(selectedIndices);
                    ThemeConfigManager.deleteIndices(del);
                    ThemeConfigManager.save();
                    profiles = ThemeConfigManager.getProfiles();
                    selectedIndices.clear();
                    primarySelected = ThemeConfigManager.getActiveIndex();
                    selectedIndices.add(primarySelected);
                    anchorIndex = primarySelected;
                    bindProfileToEditor(getEditingTarget());
                }
                return;
            case 1101:
                applyEditorToTarget(false);
                return;
            case 1201:
            case 1202:
            case 1203:
                cycleQuality(button.id);
                refreshQualityBtnText();
                applyEditorToTarget(false);
                TextureManagerHelper.clearCache();
                return;
            case 1204:
                ExternalLinkOpener.open("https://haowallpaper.com/homeView", "theme-wallpaper");
                return;
            default:
                super.actionPerformed(button);
        }
    }

    private void cycleQuality(int id) {
        ThemeProfile p = getEditingTarget();
        if (p == null || isReadOnlyTarget()) {
            return;
        }
        if (id == 1201) {
            p.panelImageQuality = nextQuality(p.panelImageQuality);
        } else if (id == 1202) {
            p.buttonImageQuality = nextQuality(p.buttonImageQuality);
        } else if (id == 1203) {
            p.inputImageQuality = nextQuality(p.inputImageQuality);
        }
    }

    private String nextQuality(String q) {
        ImageQuality cur;
        try {
            cur = ImageQuality.valueOf(q);
        } catch (Exception e) {
            cur = ImageQuality.MEDIUM;
        }
        return cur.next().name();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1 && tryPickImageByRightClick(mouseX, mouseY)) {
            applyEditorToTarget(false);
            TextureManagerHelper.clearCache();
            return;
        }

        if (mouseButton == 0) {
            if (isOnEditorScrollbar(mouseX, mouseY)) {
                draggingEditorScrollbar = true;
                editorDragStartMouseY = mouseY;
                editorDragStartScroll = editorScroll;
                return;
            }
            if (isOnListScrollbar(mouseX, mouseY)) {
                draggingListScrollbar = true;
                listDragStartMouseY = mouseY;
                listDragStartScroll = listScroll;
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        clickFields(mouseX, mouseY, mouseButton);

        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            int local = (mouseY - listY) / ROW_H;
            int idx = listScroll + local;
            if (idx >= 0 && idx < profiles.size()) {
                handleListSelection(idx);
            }
        }
    }

    private boolean tryPickImageByRightClick(int mouseX, int mouseY) {
        if (pickImageIntoFieldIfHovered(panelImgField, mouseX, mouseY)) {
            return true;
        }
        if (pickImageIntoFieldIfHovered(buttonImgField, mouseX, mouseY)) {
            return true;
        }
        return pickImageIntoFieldIfHovered(inputImgField, mouseX, mouseY);
    }

    private boolean pickImageIntoFieldIfHovered(GuiTextField field, int mouseX, int mouseY) {
        if (!isFieldHovered(field, mouseX, mouseY) || isReadOnlyTarget()) {
            return false;
        }

        try {
            JFileChooser chooser = new JFileChooser();
            File defaultDir = TextureManagerHelper.getThemeImageCacheDir().toFile();
            if (defaultDir.exists() && defaultDir.isDirectory()) {
                chooser.setCurrentDirectory(defaultDir);
            }
            chooser.setDialogTitle("选择图片");
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileFilter(new FileNameExtensionFilter("图片文件", "png", "jpg", "jpeg", "bmp", "gif", "webp"));

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                String selected = chooser.getSelectedFile().getAbsolutePath();
                field.setText(selected);
            }
        } catch (Exception ignored) {
        }

        return true;
    }

    private void handleListSelection(int idx) {
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        if (shift && anchorIndex >= 0) {
            selectedIndices.clear();
            int a = Math.min(anchorIndex, idx);
            int b = Math.max(anchorIndex, idx);
            for (int i = a; i <= b; i++) {
                selectedIndices.add(i);
            }
            primarySelected = idx;
        } else if (ctrl) {
            if (selectedIndices.contains(idx)) {
                selectedIndices.remove(idx);
                if (primarySelected == idx) {
                    primarySelected = selectedIndices.isEmpty() ? -1 : selectedIndices.iterator().next();
                }
            } else {
                selectedIndices.add(idx);
                primarySelected = idx;
            }
            anchorIndex = idx;
        } else {
            selectedIndices.clear();
            selectedIndices.add(idx);
            primarySelected = idx;
            anchorIndex = idx;
        }

        if (primarySelected >= 0 && primarySelected < profiles.size()) {
            randomDraft = null; // 切回真实主题编辑
            ThemeConfigManager.setActiveIndex(primarySelected);
            bindProfileToEditor(getEditingTarget());
            refreshQualityBtnText();
        }
    }

    private void clickFields(int mouseX, int mouseY, int mouseButton) {
        for (GuiTextField f : allFields()) {
            if (f.getVisible()) {
                f.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            returnToMainMenuOverlay();
            return;
        }

        boolean edited = false;
        for (GuiTextField f : allFields()) {
            if (f.getVisible() && f.textboxKeyTyped(typedChar, keyCode)) {
                edited = true;
            }
        }
        if (edited) {
            applyEditorToTarget(false);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private List<GuiTextField> allFields() {
        List<GuiTextField> list = new ArrayList<>();
        Collections.addAll(list,
                nameField, panelImgField, buttonImgField, inputImgField,
                panelScaleField, buttonScaleField, inputScaleField,
                panelCropXField, panelCropYField, buttonCropXField, buttonCropYField, inputCropXField,
                inputCropYField);
        return list;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }

        int mx = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int my = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        if (mx >= editorX && mx <= editorX + editorW && my >= editorY && my <= editorY + editorH) {
            int step = 36;
            if (dWheel > 0) {
                editorScroll = Math.max(0, editorScroll - step);
            } else {
                editorScroll = Math.min(editorMaxScroll, editorScroll + step);
            }
            relayoutEditorWithScroll();
        } else if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            if (dWheel > 0) {
                listScroll = Math.max(0, listScroll - 1);
            } else {
                listScroll = Math.min(listMaxScroll, listScroll + 1);
            }
        }
    }

    @Override
    public void updateScreen() {
        for (GuiTextField f : allFields()) {
            if (f.getVisible()) {
                f.updateCursorCounter();
            }
        }
        applyEditorToTarget(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, I18n.format("gui.theme.title"), this.fontRenderer);

        drawLeftList();

        // 右侧编辑区外框
        drawString(this.fontRenderer, I18n.format("gui.theme.editor"), editorX, panelY + 26, GuiTheme.LABEL_TEXT);
        if (isReadOnlyTarget()) {
            drawString(this.fontRenderer, "内置主题已锁定，若要修改请先复制一份", editorX + 52, panelY + 26, 0xFFF6D28B);
        }
        drawRect(editorX, editorY, editorX + editorContentW, editorY + editorH, 0x44314156);

        relayoutEditorWithScroll();

        String fieldTooltipKey = getFieldTooltipKey(mouseX, mouseY);

        startScissor(editorX, editorY, editorContentW, editorH);
        drawEditorLabelsAndFields();
        drawEditorPreview();
        endScissor();

        drawEditorScrollbar();
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (fieldTooltipKey != null) {
            String normalized = I18n.format(fieldTooltipKey).replace("\\n", "\n");
            GuiUtils.drawHoveringText(Arrays.asList(normalized.split("\n")), mouseX, mouseY,
                    this.width, this.height, -1, this.fontRenderer);
        }
    }

    private void drawLeftList() {
        drawString(this.fontRenderer, I18n.format("gui.theme.preset_list"), listX, panelY + 26, GuiTheme.LABEL_TEXT);
        drawRect(listX, listY, listX + listW, listY + listH, 0x55314156);

        int visible = Math.max(1, listH / ROW_H);
        listMaxScroll = Math.max(0, profiles.size() - visible);

        for (int i = 0; i < visible; i++) {
            int idx = listScroll + i;
            if (idx >= profiles.size()) {
                break;
            }
            int y = listY + i * ROW_H;
            boolean selected = selectedIndices.contains(idx);
            int bg = selected ? 0xAA3A6D92 : 0x66314256;
            drawRect(listX + 2, y + 1, listX + listW - 10, y + ROW_H - 1, bg);
            if (idx == primarySelected) {
                drawRect(listX + 2, y + 1, listX + listW - 10, y + 2, 0xFF8ED8FF);
            }
            String n = safe(profiles.get(idx).name);
            if (n.isEmpty()) {
                n = I18n.format("gui.theme.unnamed");
            }
            drawString(this.fontRenderer, (idx + 1) + ". " + n, listX + 8, y + 6, 0xFFFFFFFF);
        }

        if (listMaxScroll > 0) {
            int sbX = listX + listW - 8;
            int sbH = listH;
            int thumbH = Math.max(18, (int) ((visible / (float) profiles.size()) * sbH));
            int thumbY = listY + (int) ((listScroll / (float) listMaxScroll) * (sbH - thumbH));
            GuiTheme.drawScrollbar(sbX, listY, 6, sbH, thumbY, thumbH);
        }

        if (randomDraft != null) {
            drawString(this.fontRenderer, I18n.format("gui.theme.random") + " Draft", listX + 90, panelY + panelH - 46,
                    0xFF9FFFB0);
        }
    }

    private void relayoutEditorWithScroll() {
        editorMaxScroll = Math.max(0, editorContentHeight - editorH);
        editorScroll = Math.max(0, Math.min(editorScroll, editorMaxScroll));

        int base = editorY - editorScroll;
        btnApply.x = editorX;
        btnApply.y = base;

        if (btnRightSave != null) {
            btnRightSave.x = editorX + editorContentW - btnRightSave.width;
            btnRightSave.y = panelY + panelH - 28;
            btnRightSave.visible = true;
        }

        int fieldW = Math.max(120, editorContentW - 126);
        int qualityW = Math.max(80, Math.min(120, editorContentW - fieldW - 6));

        nameField.x = editorX;
        nameField.y = base + 30;
        nameField.width = editorContentW;

        panelImgField.x = editorX;
        panelImgField.y = base + 66;
        panelImgField.width = fieldW;
        buttonImgField.x = editorX;
        buttonImgField.y = base + 92;
        buttonImgField.width = fieldW;
        inputImgField.x = editorX;
        inputImgField.y = base + 118;
        inputImgField.width = fieldW;

        panelQualityBtn.x = editorX + fieldW + 6;
        panelQualityBtn.y = base + 66;
        panelQualityBtn.width = qualityW;
        buttonQualityBtn.x = editorX + fieldW + 6;
        buttonQualityBtn.y = base + 92;
        buttonQualityBtn.width = qualityW;
        inputQualityBtn.x = editorX + fieldW + 6;
        inputQualityBtn.y = base + 118;
        inputQualityBtn.width = qualityW;

        if (btnWallpaperPage != null) {
            btnWallpaperPage.x = editorX + fieldW + 6;
            btnWallpaperPage.y = base + 154;
            btnWallpaperPage.width = qualityW;
        }

        // --- 核心修改：重新布局缩放和裁剪输入框 ---
        int y_scale = base + 172;
        int fieldW_s = 52;
        panelScaleField.x = editorX;
        panelScaleField.y = y_scale;
        buttonScaleField.x = editorX + fieldW_s + 4;
        buttonScaleField.y = y_scale;
        inputScaleField.x = editorX + 2 * (fieldW_s + 4);
        inputScaleField.y = y_scale;

        int y_crop = y_scale + 40;
        int fieldW_c = 42;
        int group_gap = 10;
        int x_pos = editorX;
        panelCropXField.x = x_pos;
        panelCropXField.y = y_crop;
        panelCropYField.x = x_pos + fieldW_c + 4;
        panelCropYField.y = y_crop;

        x_pos += 2 * (fieldW_c + 4) + group_gap;
        buttonCropXField.x = x_pos;
        buttonCropXField.y = y_crop;
        buttonCropYField.x = x_pos + fieldW_c + 4;
        buttonCropYField.y = y_crop;

        x_pos += 2 * (fieldW_c + 4) + group_gap;
        inputCropXField.x = x_pos;
        inputCropXField.y = y_crop;
        inputCropYField.x = x_pos + fieldW_c + 4;
        inputCropYField.y = y_crop;
        // --- 修改结束 ---

        relayoutSlider(pR, base + 230 + 40);
        relayoutSlider(pG, base + 254 + 40);
        relayoutSlider(pB, base + 278 + 40);

        relayoutSlider(bR, base + 230 + 40);
        relayoutSlider(bG, base + 254 + 40);
        relayoutSlider(bB, base + 278 + 40);

        relayoutSlider(iR, base + 230 + 40);
        relayoutSlider(iG, base + 254 + 40);
        relayoutSlider(iB, base + 278 + 40);

        relayoutSlider(tR, base + 334 + 40);
        relayoutSlider(tG, base + 358 + 40);
        relayoutSlider(tB, base + 382 + 40);

        relayoutSlider(opPanel, base + 334 + 40);
        relayoutSlider(opButton, base + 358 + 40);
        relayoutSlider(opInput, base + 382 + 40);
        relayoutSlider(opText, base + 334 + 40);

        updateControlVisibility();
        applyAutoButtonWidth();
    }

    private void relayoutSlider(GuiSlider s, int y) {
        if (s != null) {
            s.y = y;
        }
    }

    private void updateControlVisibility() {
        for (GuiButton b : this.buttonList) {
            if (b.id >= 1000 && b.id <= 1003) {
                b.visible = true;
            } else {
                b.visible = intersectsEditor(b.y, b.height);
            }
        }
        for (GuiTextField f : allFields()) {
            f.setVisible(intersectsEditor(f.y, f.height));
        }
        updateEditorInteractivity();
    }

    private boolean intersectsEditor(int y, int h) {
        return y + h >= editorY && y <= editorY + editorH;
    }

    private void drawEditorLabelsAndFields() {
        int base = editorY - editorScroll;

        drawLabelIfVisible(I18n.format("gui.theme.name"), editorX, base + 20);

        // --- 核心修改：重新绘制标签 ---
        drawLabelIfVisible(I18n.format("gui.theme.image_transform"), editorX, base + 154);

        int y_scale_row = base + 172;
        if (intersectsEditor(y_scale_row, 18)) {
            drawString(this.fontRenderer, I18n.format("gui.theme.label.scale") + ":", editorX, y_scale_row - 12,
                    0xFFBDBDBD);
        }

        int y_crop_row = base + 172 + 40;
        if (intersectsEditor(y_crop_row, 18)) {
            drawString(this.fontRenderer, I18n.format("gui.theme.label.crop") + ":", editorX, y_crop_row - 12,
                    0xFFBDBDBD);
        }

        drawLabelIfVisible(I18n.format("gui.theme.color_pick"), editorX, base + 210 + 40);
        drawLabelIfVisible(I18n.format("gui.theme.text_color"), editorX, base + 314 + 40);
        drawLabelIfVisible(I18n.format("gui.theme.opacity"), editorX + (editorW - 20) / 3 + 10, base + 314 + 40);
        // --- 修改结束 ---

        for (GuiTextField f : allFields()) {
            if (f.getVisible()) {
                drawThemedTextField(f);
            }
        }

        drawPathPlaceholder(panelImgField, "§8例如: D:\\背景.png");
        drawPathPlaceholder(buttonImgField, "§8例如: D:\\按键.png");
        drawPathPlaceholder(inputImgField, "§8例如: D:\\输入框.png");
    }

    private void drawLabelIfVisible(String text, int x, int y) {
        if (intersectsEditor(y, 10)) {
            drawString(this.fontRenderer, text, x, y, GuiTheme.SUB_TEXT);
        }
    }

    private void drawPathPlaceholder(GuiTextField f, String placeholder) {
        if (f == null || !f.getVisible() || f.isFocused()) {
            return;
        }
        String v = f.getText();
        if (v != null && !v.trim().isEmpty()) {
            return;
        }
        this.fontRenderer.drawStringWithShadow(placeholder, f.x + 4, f.y + (f.height - 8) / 2, 0xFFFFFFFF);
    }

    private String getFieldTooltipKey(int mouseX, int mouseY) {
        if (isFieldHovered(panelImgField, mouseX, mouseY)) {
            return "gui.theme.panel_image";
        }
        if (isFieldHovered(buttonImgField, mouseX, mouseY)) {
            return "gui.theme.button_image";
        }
        if (isFieldHovered(inputImgField, mouseX, mouseY)) {
            return "gui.theme.input_image";
        }

        if (isFieldHovered(panelScaleField, mouseX, mouseY)) {
            return "gui.theme.tip.panel_scale";
        }
        if (isFieldHovered(buttonScaleField, mouseX, mouseY)) {
            return "gui.theme.tip.button_scale";
        }
        if (isFieldHovered(inputScaleField, mouseX, mouseY)) {
            return "gui.theme.tip.input_scale";
        }

        if (isFieldHovered(panelCropXField, mouseX, mouseY)) {
            return "gui.theme.tip.panel_crop_left";
        }
        if (isFieldHovered(panelCropYField, mouseX, mouseY)) {
            return "gui.theme.tip.panel_crop_right";
        }
        if (isFieldHovered(buttonCropXField, mouseX, mouseY)) {
            return "gui.theme.tip.button_crop_left";
        }
        if (isFieldHovered(buttonCropYField, mouseX, mouseY)) {
            return "gui.theme.tip.button_crop_right";
        }
        if (isFieldHovered(inputCropXField, mouseX, mouseY)) {
            return "gui.theme.tip.input_crop_left";
        }
        if (isFieldHovered(inputCropYField, mouseX, mouseY)) {
            return "gui.theme.tip.input_crop_right";
        }
        return null;
    }

    private boolean isFieldHovered(GuiTextField f, int mouseX, int mouseY) {
        return f != null && f.getVisible()
                && mouseX >= f.x && mouseX <= f.x + f.width
                && mouseY >= f.y && mouseY <= f.y + f.height;
    }

    private void drawEditorPreview() {
        int base = editorY - editorScroll;
        int previewX = editorX;
        int previewY = base + 450 + 40; // 向下移动
        int previewW = Math.min(320, editorContentW - 4);
        int previewH = 130;
        if (!intersectsEditor(previewY, previewH)) {
            return;
        }

        GuiTheme.drawPanel(previewX, previewY, previewW, previewH);
        GuiTheme.drawTitleBar(previewX, previewY, previewW, I18n.format("gui.theme.preview"), this.fontRenderer);
        GuiTheme.drawButtonFrame(previewX + 14, previewY + 34, 90, 18, GuiTheme.UiState.NORMAL);
        drawCenteredString(this.fontRenderer, I18n.format("gui.theme.button_sample"), previewX + 59, previewY + 39,
                0xFFFFFFFF);
        GuiTheme.drawInputFrame(previewX + 14, previewY + 60, 160, 18, true, true);
        drawString(this.fontRenderer, I18n.format("gui.theme.input_sample"), previewX + 18, previewY + 66, 0xFFCCD8E6);
    }

    private void drawEditorScrollbar() {
        if (editorMaxScroll <= 0) {
            return;
        }
        int sbX = editorX + editorContentW + 2;
        int sbH = editorH;
        int thumbH = Math.max(22, (int) ((editorH / (float) editorContentHeight) * sbH));
        int thumbY = editorY + (int) ((editorScroll / (float) editorMaxScroll) * (sbH - thumbH));
        GuiTheme.drawScrollbar(sbX, editorY, 6, sbH, thumbY, thumbH);
    }

    private boolean isOnEditorScrollbar(int mx, int my) {
        if (editorMaxScroll <= 0) {
            return false;
        }
        int sbX = editorX + editorContentW + 2;
        return mx >= sbX && mx <= sbX + 6 && my >= editorY && my <= editorY + editorH;
    }

    private boolean isOnListScrollbar(int mx, int my) {
        if (listMaxScroll <= 0) {
            return false;
        }
        int sbX = listX + listW - 8;
        return mx >= sbX && mx <= sbX + 6 && my >= listY && my <= listY + listH;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton != 0) {
            return;
        }

        if (draggingEditorScrollbar && editorMaxScroll > 0) {
            int sbH = editorH;
            int thumbH = Math.max(22, (int) ((editorH / (float) editorContentHeight) * sbH));
            int track = Math.max(1, sbH - thumbH);
            int dy = mouseY - editorDragStartMouseY;
            int delta = Math.round(dy * (editorMaxScroll / (float) track));
            editorScroll = Math.max(0, Math.min(editorMaxScroll, editorDragStartScroll + delta));
            relayoutEditorWithScroll();
        }

        if (draggingListScrollbar && listMaxScroll > 0) {
            int visible = Math.max(1, listH / ROW_H);
            int thumbH = Math.max(18, (int) ((visible / (float) profiles.size()) * listH));
            int track = Math.max(1, listH - thumbH);
            int dy = mouseY - listDragStartMouseY;
            int delta = Math.round(dy * (listMaxScroll / (float) track));
            listScroll = Math.max(0, Math.min(listMaxScroll, listDragStartScroll + delta));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingEditorScrollbar = false;
        draggingListScrollbar = false;
    }

    private void startScissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(this.mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + h)) * scale, w * scale, h * scale);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void ensureSelectedVisible() {
        int visible = Math.max(1, listH / ROW_H);
        listMaxScroll = Math.max(0, profiles.size() - visible);
        if (primarySelected < listScroll) {
            listScroll = primarySelected;
        } else if (primarySelected >= listScroll + visible) {
            listScroll = primarySelected - visible + 1;
        }
        listScroll = Math.max(0, Math.min(listScroll, listMaxScroll));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private int toArgb(int r, int g, int b) {
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private int lighten(int color, int delta) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + delta);
        int g = Math.min(255, ((color >> 8) & 0xFF) + delta);
        int b = Math.min(255, (color & 0xFF) + delta);
        return toArgb(r, g, b);
    }

    private int darken(int color, int delta) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - delta);
        int g = Math.max(0, ((color >> 8) & 0xFF) - delta);
        int b = Math.max(0, (color & 0xFF) - delta);
        return toArgb(r, g, b);
    }

    private int parseIntOr(String s, int fallback) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private int clampOpacity(int v) {
        return clamp(v, 10, 100);
    }
}







