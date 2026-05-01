package com.zszl.zszlScriptMod.gui.components;

import com.zszl.zszlScriptMod.config.ChatOptimizationConfig.ImageQuality;
import com.zszl.zszlScriptMod.gui.theme.ThemeConfigManager;
import com.zszl.zszlScriptMod.gui.theme.ThemeConfigManager.ThemeProfile;
import com.zszl.zszlScriptMod.utils.TextureManagerHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class GuiTheme {

    private GuiTheme() {
    }

    public static int PANEL_BORDER = 0xFF3A5A78;
    public static int PANEL_BG_TOP = 0xD01A2532;
    public static int PANEL_BG_BOTTOM = 0xD0121B26;
    public static int TITLE_LEFT = 0xFF2E8BC0;
    public static int TITLE_RIGHT = 0xFF56CCF2;
    public static int TITLE_TEXT = 0xFFF2F8FF;
    public static int LABEL_TEXT = 0xFFE6EEF8;
    public static int SUB_TEXT = 0xFF9FB2C8;

    public static int resolveTextColor(String text, int originalColor) {
        int base = (text != null && text.indexOf('\u00A7') >= 0) ? originalColor : LABEL_TEXT;
        return applyOpacity(base, TEXT_OPACITY_PERCENT);
    }

    // Layout tokens
    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    public static final int SPACE_LG = 16;

    // State colors
    public static int STATE_SUCCESS = 0xFF2FAF5E;
    public static int STATE_WARNING = 0xFFE0A100;
    public static int STATE_DANGER = 0xFFB64A4A;
    public static int STATE_DISABLED = 0xFF4A4F56;
    public static int STATE_SELECTED = 0xFF3E85B5;

    public static int BUTTON_BG_NORMAL = 0xFF2F475F;
    public static int BUTTON_BG_HOVER = 0xFF385470;
    public static int BUTTON_BG_PRESSED = 0xFF2A4055;
    public static int BUTTON_BORDER_NORMAL = 0xFF466786;
    public static int BUTTON_BORDER_HOVER = 0xFF7ED0FF;

    public static int INPUT_BG = 0xFF1E242D;
    public static int INPUT_BORDER = 0xFF3E6A8D;
    public static int INPUT_BORDER_HOVER = 0xFF68B6E8;

    // 10-100, 10=最高透明度(90%透明)
    public static int PANEL_OPACITY_PERCENT = 100;
    public static int BUTTON_OPACITY_PERCENT = 100;
    public static int INPUT_OPACITY_PERCENT = 100;
    public static int TEXT_OPACITY_PERCENT = 100;

    public static String PANEL_IMAGE_PATH = "";
    public static String BUTTON_IMAGE_PATH = "";
    public static String INPUT_IMAGE_PATH = "";
    public static int PANEL_IMAGE_SCALE = 100;
    public static int BUTTON_IMAGE_SCALE = 100;
    public static int INPUT_IMAGE_SCALE = 100;
    public static int PANEL_CROP_X = 0;
    public static int PANEL_CROP_Y = 0;
    public static int BUTTON_CROP_X = 0;
    public static int BUTTON_CROP_Y = 0;
    public static int INPUT_CROP_X = 0;
    public static int INPUT_CROP_Y = 0;
    public static String PANEL_IMAGE_QUALITY = "MEDIUM";
    public static String BUTTON_IMAGE_QUALITY = "MEDIUM";
    public static String INPUT_IMAGE_QUALITY = "MEDIUM";

    static {
        ThemeConfigManager.ensureLoaded();
    }

    public enum UiState {
        NORMAL,
        HOVER,
        PRESSED,
        DISABLED,
        SUCCESS,
        WARNING,
        DANGER,
        SELECTED
    }

    public static void drawPanel(int x, int y, int width, int height) {
        drawPanelSegment(x, y, width, height, x, y, width, height);
    }

    /**
     * 绘制可拼接的面板分片。
     * <p>
     * 当多个面板希望看起来像“同一张背景图被切开”时，传入同一组 group 参数，
     * 即可让纹理采样保持连续。
     */
    public static void drawPanelSegment(int x, int y, int width, int height,
            int groupX, int groupY, int groupWidth, int groupHeight) {
        // 阴影
        Gui.drawRect(x - 3, y - 3, x + width + 3, y + height + 3, 0x30000000);
        Gui.drawRect(x - 2, y - 2, x + width + 2, y + height + 2, 0x45000000);

        // 边框
        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, PANEL_BORDER);

        if (!drawTextureBackgroundSegment(x, y, width, height,
                groupX, groupY, groupWidth, groupHeight,
                PANEL_IMAGE_PATH, PANEL_IMAGE_QUALITY, PANEL_IMAGE_SCALE,
                PANEL_CROP_X, PANEL_CROP_Y, PANEL_OPACITY_PERCENT)) {
            // 主背景（纵向渐变，兼容 1.12.2）
            drawVerticalGradientRect(x, y, x + width, y + height,
                    applyOpacity(PANEL_BG_TOP, PANEL_OPACITY_PERCENT),
                    applyOpacity(PANEL_BG_BOTTOM, PANEL_OPACITY_PERCENT));
        }
    }

    public static void drawSectionTitle(int x, int y, String title, FontRenderer fr) {
        fr.drawStringWithShadow(title, x, y, applyOpacity(LABEL_TEXT, TEXT_OPACITY_PERCENT));
    }

    public static void drawStatusText(int x, int y, String text, UiState state, FontRenderer fr) {
        fr.drawStringWithShadow(text, x, y, applyOpacity(getStateTextColor(state), TEXT_OPACITY_PERCENT));
    }

    public static void drawButtonFrame(int x, int y, int width, int height, UiState state) {
        int borderColor = applyOpacity(getButtonBorderColor(state), BUTTON_OPACITY_PERCENT);
        int bgColor = applyOpacity(getButtonBgColor(state), BUTTON_OPACITY_PERCENT);

        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        if (!drawTextureBackground(x, y, width, height, BUTTON_IMAGE_PATH, BUTTON_IMAGE_QUALITY, BUTTON_IMAGE_SCALE,
                BUTTON_CROP_X, BUTTON_CROP_Y, BUTTON_OPACITY_PERCENT)) {
            Gui.drawRect(x, y, x + width, y + height, bgColor);
        } else {
            // 有纹理时叠加一层色片即可；bgColor 已应用过一次按钮透明度，避免重复叠加导致发灰
            Gui.drawRect(x, y, x + width, y + height, bgColor);
        }

        if (state == UiState.HOVER || state == UiState.PRESSED) {
            Gui.drawRect(x, y, x + width, y + height, applyOpacity(0x33FFFFFF, BUTTON_OPACITY_PERCENT));
        }
    }

    // 安全版：不使用按钮纹理，仅使用纯色主题，避免外部纹理/裁切导致显示异常
    public static void drawButtonFrameSafe(int x, int y, int width, int height, UiState state) {
        int borderColor = applyOpacity(getButtonBorderColor(state), BUTTON_OPACITY_PERCENT);
        int bgColor = applyOpacity(getButtonBgColor(state), BUTTON_OPACITY_PERCENT);

        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        Gui.drawRect(x, y, x + width, y + height, bgColor);

        if (state == UiState.HOVER || state == UiState.PRESSED) {
            Gui.drawRect(x, y, x + width, y + height, applyOpacity(0x33FFFFFF, BUTTON_OPACITY_PERCENT));
        }
    }

    public static void drawToggleFrame(int x, int y, int width, int height, boolean on, UiState state) {
        UiState actualState = state;
        if (on && state != UiState.DISABLED) {
            actualState = (state == UiState.PRESSED) ? UiState.PRESSED : UiState.SUCCESS;
        }
        drawButtonFrame(x, y, width, height, actualState);
    }

    public static void drawInputFrame(int x, int y, int width, int height, boolean focused, boolean enabled) {
        UiState state = !enabled ? UiState.DISABLED : (focused ? UiState.HOVER : UiState.NORMAL);
        int border = applyOpacity(getInputBorderColor(state), INPUT_OPACITY_PERCENT);
        int bg = applyOpacity((state == UiState.DISABLED) ? 0xFF2A2E33 : INPUT_BG, INPUT_OPACITY_PERCENT);

        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, border);
        if (!drawTextureBackground(x, y, width, height, INPUT_IMAGE_PATH, INPUT_IMAGE_QUALITY, INPUT_IMAGE_SCALE,
                INPUT_CROP_X, INPUT_CROP_Y, INPUT_OPACITY_PERCENT)) {
            Gui.drawRect(x, y, x + width, y + height, bg);
        } else {
            Gui.drawRect(x, y, x + width, y + height, applyOpacity(bg, INPUT_OPACITY_PERCENT));
        }
    }

    // 安全版：不使用输入框纹理，仅使用纯色主题
    public static void drawInputFrameSafe(int x, int y, int width, int height, boolean focused, boolean enabled) {
        UiState state = !enabled ? UiState.DISABLED : (focused ? UiState.HOVER : UiState.NORMAL);
        int border = applyOpacity(getInputBorderColor(state), INPUT_OPACITY_PERCENT);
        int bg = applyOpacity((state == UiState.DISABLED) ? 0xFF2A2E33 : INPUT_BG, INPUT_OPACITY_PERCENT);

        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, border);
        Gui.drawRect(x, y, x + width, y + height, bg);
    }

    public static void applyProfile(ThemeProfile p) {
        if (p == null) {
            return;
        }
        PANEL_BORDER = p.panelBorder;
        PANEL_BG_TOP = p.panelBgTop;
        PANEL_BG_BOTTOM = p.panelBgBottom;
        TITLE_LEFT = p.titleLeft;
        TITLE_RIGHT = p.titleRight;
        TITLE_TEXT = p.titleText;
        LABEL_TEXT = p.labelText;
        SUB_TEXT = p.subText;

        STATE_SUCCESS = p.stateSuccess;
        STATE_WARNING = p.stateWarning;
        STATE_DANGER = p.stateDanger;
        STATE_DISABLED = p.stateDisabled;
        STATE_SELECTED = p.stateSelected;

        BUTTON_BG_NORMAL = p.buttonBgNormal;
        BUTTON_BG_HOVER = p.buttonBgHover;
        BUTTON_BG_PRESSED = p.buttonBgPressed;
        BUTTON_BORDER_NORMAL = p.buttonBorderNormal;
        BUTTON_BORDER_HOVER = p.buttonBorderHover;

        INPUT_BG = p.inputBg;
        INPUT_BORDER = p.inputBorder;
        INPUT_BORDER_HOVER = p.inputBorderHover;
        PANEL_OPACITY_PERCENT = clampPercent(p.panelOpacityPercent);
        BUTTON_OPACITY_PERCENT = clampPercent(p.buttonOpacityPercent);
        INPUT_OPACITY_PERCENT = clampPercent(p.inputOpacityPercent);
        TEXT_OPACITY_PERCENT = clampPercent(p.textOpacityPercent);

        PANEL_IMAGE_PATH = p.panelImagePath == null ? "" : p.panelImagePath;
        BUTTON_IMAGE_PATH = p.buttonImagePath == null ? "" : p.buttonImagePath;
        INPUT_IMAGE_PATH = p.inputImagePath == null ? "" : p.inputImagePath;
        PANEL_IMAGE_SCALE = p.panelImageScale;
        BUTTON_IMAGE_SCALE = p.buttonImageScale;
        INPUT_IMAGE_SCALE = p.inputImageScale;
        PANEL_CROP_X = p.panelCropX;
        PANEL_CROP_Y = p.panelCropY;
        BUTTON_CROP_X = p.buttonCropX;
        BUTTON_CROP_Y = p.buttonCropY;
        INPUT_CROP_X = p.inputCropX;
        INPUT_CROP_Y = p.inputCropY;
        PANEL_IMAGE_QUALITY = p.panelImageQuality == null ? "MEDIUM" : p.panelImageQuality;
        BUTTON_IMAGE_QUALITY = p.buttonImageQuality == null ? "MEDIUM" : p.buttonImageQuality;
        INPUT_IMAGE_QUALITY = p.inputImageQuality == null ? "MEDIUM" : p.inputImageQuality;

        // 预热缓存，避免首次显示时阻塞渲染线程
        TextureManagerHelper.prefetch(PANEL_IMAGE_PATH, parseQuality(PANEL_IMAGE_QUALITY));
        TextureManagerHelper.prefetch(BUTTON_IMAGE_PATH, parseQuality(BUTTON_IMAGE_QUALITY));
        TextureManagerHelper.prefetch(INPUT_IMAGE_PATH, parseQuality(INPUT_IMAGE_QUALITY));
    }

    public static void drawTitleBar(int x, int y, int width, String title, FontRenderer fr) {
        int titleBarHeight = 20;
        drawHorizontalGradientRect(x, y, x + width, y + titleBarHeight, TITLE_LEFT, TITLE_RIGHT);
        Gui.drawRect(x, y + titleBarHeight, x + width, y + titleBarHeight + 1, 0xAA0B121A);
        fr.drawStringWithShadow(title, x + (width - fr.getStringWidth(title)) / 2, y + 6,
                applyOpacity(TITLE_TEXT, TEXT_OPACITY_PERCENT));
    }

    public static void drawCardHighlight(int x, int y, int width, int height, boolean hovered) {
        if (!hovered) {
            return;
        }
        Gui.drawRect(x, y, x + width, y + height, 0x223EAEEA);
        Gui.drawRect(x, y, x + width, y + 1, 0xAA70D8FF);
    }

    public static void drawScrollbar(int x, int y, int width, int height, int thumbY, int thumbHeight) {
        Gui.drawRect(x, y, x + width, y + height, 0xAA1B2733);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xCC111A23);

        Gui.drawRect(x + 1, thumbY, x + width - 1, thumbY + thumbHeight, 0xFF4E7FA8);
        Gui.drawRect(x + 1, thumbY, x + width - 1, thumbY + 1, 0xFF82CFFF);
    }

    public static void drawEmptyState(int centerX, int y, String text, FontRenderer fr) {
        fr.drawStringWithShadow(text, centerX - fr.getStringWidth(text) / 2, y,
                applyOpacity(SUB_TEXT, TEXT_OPACITY_PERCENT));
    }

    public static int getStateTextColor(UiState state) {
        switch (state) {
            case SUCCESS:
                return 0xFFA8F5C5;
            case WARNING:
                return 0xFFFFE0A3;
            case DANGER:
                return 0xFFFFB7B7;
            case DISABLED:
                return 0xFF97A3B2;
            case HOVER:
            case PRESSED:
                return 0xFFF5FBFF;
            default:
                return 0xFFDDEAF6;
        }
    }

    private static int getButtonBorderColor(UiState state) {
        switch (state) {
            case DISABLED:
                return STATE_DISABLED;
            case HOVER:
            case PRESSED:
                return BUTTON_BORDER_HOVER;
            case SUCCESS:
                return 0xFF58CF86;
            case WARNING:
                return 0xFFFFC44D;
            case DANGER:
                return 0xFFE07B7B;
            case SELECTED:
                return 0xFF78C5FF;
            default:
                return BUTTON_BORDER_NORMAL;
        }
    }

    private static int getButtonBgColor(UiState state) {
        switch (state) {
            case DISABLED:
                return 0xFF323841;
            case PRESSED:
                return BUTTON_BG_PRESSED;
            case HOVER:
                return BUTTON_BG_HOVER;
            case SUCCESS:
                return 0xFF2A7F3E;
            case WARNING:
                return 0xFF8E6A17;
            case DANGER:
                return 0xFF7A3A3A;
            case SELECTED:
                return 0xFF2F5F85;
            default:
                return BUTTON_BG_NORMAL;
        }
    }

    private static int getInputBorderColor(UiState state) {
        switch (state) {
            case DISABLED:
                return 0xFF4A4F56;
            case HOVER:
                return INPUT_BORDER_HOVER;
            default:
                return INPUT_BORDER;
        }
    }

    private static boolean drawTextureBackground(int x, int y, int width, int height, String path, String qualityName,
            int scale, int cropX, int cropY, int opacityPercent) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        ImageQuality quality = parseQuality(qualityName);
        Identifier tex = TextureManagerHelper.getIdentifierForPath(path, quality);
        if (tex == null) {
            return false;
        }

        int safeScale = Math.max(10, Math.min(300, scale));
        float scaleFactor = 100.0F / safeScale;
        int[] textureSize = TextureManagerHelper.getTextureSizeForPath(path, quality);
        int textureWidth = textureSize != null ? textureSize[0] : Math.max(1, width);
        int textureHeight = textureSize != null ? textureSize[1] : Math.max(1, height);

        int sampleWidth = Math.max(1, Math.min(textureWidth, Math.round(width * scaleFactor)));
        int sampleHeight = Math.max(1, Math.min(textureHeight, Math.round(height * scaleFactor)));
        int sampleU = Math.max(0, cropX);
        int sampleV = Math.max(0, cropY);
        if (sampleU + sampleWidth > textureWidth) {
            sampleU = Math.max(0, textureWidth - sampleWidth);
        }
        if (sampleV + sampleHeight > textureHeight) {
            sampleV = Math.max(0, textureHeight - sampleHeight);
        }

        Gui.drawScaledCustomSizeModalRect(tex, x, y, sampleU, sampleV, sampleWidth, sampleHeight, width, height,
                textureWidth, textureHeight, textureColor(opacityPercent));
        return true;
    }

    /**
     * 与 drawTextureBackground 类似，但按“整组区域”进行统一采样，
     * 用于左右拼接面板保持背景连续。
     */
    private static boolean drawTextureBackgroundSegment(int x, int y, int width, int height,
            int groupX, int groupY, int groupWidth, int groupHeight,
            String path, String qualityName,
            int scale, int cropX, int cropY, int opacityPercent) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        if (width <= 0 || height <= 0 || groupWidth <= 0 || groupHeight <= 0) {
            return false;
        }
        ImageQuality quality = parseQuality(qualityName);
        Identifier tex = TextureManagerHelper.getIdentifierForPath(path, quality);
        int[] textureSize = TextureManagerHelper.getTextureSizeForPath(path, quality);
        if (tex == null || textureSize == null || textureSize.length < 2) {
            return false;
        }

        int textureWidth = Math.max(1, textureSize[0]);
        int textureHeight = Math.max(1, textureSize[1]);
        int safeScale = Math.max(10, Math.min(300, scale));
        float scaleFactor = 100.0F / safeScale;
        int groupSampleWidth = Math.max(1, Math.min(textureWidth, Math.round(groupWidth * scaleFactor)));
        int groupSampleHeight = Math.max(1, Math.min(textureHeight, Math.round(groupHeight * scaleFactor)));

        float groupSafeWidth = Math.max(1.0F, groupWidth);
        float groupSafeHeight = Math.max(1.0F, groupHeight);
        int segmentOffsetX = Math.max(0, Math.round(((x - groupX) / groupSafeWidth) * groupSampleWidth));
        int segmentOffsetY = Math.max(0, Math.round(((y - groupY) / groupSafeHeight) * groupSampleHeight));
        int segmentSampleWidth = Math.max(1, Math.round((width / groupSafeWidth) * groupSampleWidth));
        int segmentSampleHeight = Math.max(1, Math.round((height / groupSafeHeight) * groupSampleHeight));

        int baseU = Math.max(0, cropX);
        int baseV = Math.max(0, cropY);
        if (baseU + groupSampleWidth > textureWidth) {
            baseU = Math.max(0, textureWidth - groupSampleWidth);
        }
        if (baseV + groupSampleHeight > textureHeight) {
            baseV = Math.max(0, textureHeight - groupSampleHeight);
        }

        int sampleU = baseU + segmentOffsetX;
        int sampleV = baseV + segmentOffsetY;
        if (segmentOffsetX + segmentSampleWidth > groupSampleWidth) {
            segmentSampleWidth = Math.max(1, groupSampleWidth - segmentOffsetX);
        }
        if (segmentOffsetY + segmentSampleHeight > groupSampleHeight) {
            segmentSampleHeight = Math.max(1, groupSampleHeight - segmentOffsetY);
        }
        if (sampleU + segmentSampleWidth > textureWidth) {
            segmentSampleWidth = Math.max(1, textureWidth - sampleU);
        }
        if (sampleV + segmentSampleHeight > textureHeight) {
            segmentSampleHeight = Math.max(1, textureHeight - sampleV);
        }

        Gui.drawScaledCustomSizeModalRect(tex, x, y, sampleU, sampleV, segmentSampleWidth, segmentSampleHeight, width,
                height, textureWidth, textureHeight, textureColor(opacityPercent));
        return true;
    }

    private static int textureColor(int opacityPercent) {
        int alpha = Math.round(255.0F * clampPercent(opacityPercent) / 100.0F);
        return (alpha << 24) | 0x00FFFFFF;
    }

    private static ImageQuality parseQuality(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ImageQuality.MEDIUM;
        }
        try {
            return ImageQuality.valueOf(name.toUpperCase());
        } catch (Exception ignore) {
            return ImageQuality.MEDIUM;
        }
    }

    private static int clampPercent(int p) {
        return Math.max(10, Math.min(100, p));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int applyOpacity(int color, int percent) {
        int p = clampPercent(percent);
        int a = (color >>> 24) & 0xFF;
        if (a == 0) {
            a = 0xFF;
        }
        int na = Math.max(26, (a * p) / 100);
        return (na << 24) | (color & 0x00FFFFFF);
    }

    private static void drawVerticalGradientRect(int left, int top, int right, int bottom, int topColor,
            int bottomColor) {
        int height = Math.max(1, bottom - top);
        for (int i = 0; i < height; i++) {
            float t = i / (float) (height - 1 <= 0 ? 1 : height - 1);
            int c = lerpColor(topColor, bottomColor, t);
            Gui.drawRect(left, top + i, right, top + i + 1, c);
        }
    }

    private static void drawHorizontalGradientRect(int left, int top, int right, int bottom, int leftColor,
            int rightColor) {
        int width = Math.max(1, right - left);
        for (int i = 0; i < width; i++) {
            float t = i / (float) (width - 1 <= 0 ? 1 : width - 1);
            int c = lerpColor(leftColor, rightColor, t);
            Gui.drawRect(left + i, top, left + i + 1, bottom, c);
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
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
}




