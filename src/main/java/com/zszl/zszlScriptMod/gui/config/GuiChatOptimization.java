package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.GlobalEventListener;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config.GuiSlider;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.config.ChatOptimizationConfig;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.packet.PacketInterceptConfig;
import com.zszl.zszlScriptMod.utils.TextureManagerHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiChatOptimization extends ThemedGuiScreen {

    private static final int BTN_SAVE = 100;
    private static final int BTN_RESET = 101;
    private static final int BTN_CANCEL = 102;

    private static final int BTN_ANTI_SPAM = 2;
    private static final int BTN_TIMESTAMP = 3;
    private static final int BTN_ANTI_SPAM_SCROLL = 4;
    private static final int BTN_BLACKLIST = 6;
    private static final int BTN_WHITELIST = 7;
    private static final int BTN_BG_ALPHA = 10;
    private static final int BTN_SMOOTH = 11;
    private static final int BTN_SCALE = 12;
    private static final int BTN_WIDTH = 13;
    private static final int BTN_IMAGE_QUALITY = 15;
    private static final int BTN_TIMED_ENABLE = 16;
    private static final int BTN_TIMED_MODE = 19;
    private static final int BTN_TIMED_ADD = 20;
    private static final int BTN_TIMED_EDIT = 21;
    private static final int BTN_TIMED_DELETE = 22;

    private final GuiScreen parentScreen;
    private ChatOptimizationConfig settings;

    private final List<Component> exampleChat = new ArrayList<>();
    private final List<GuiButton> scrollableButtons = new ArrayList<>();
    private final List<GuiTextField> textFields = new ArrayList<>();
    private final List<GuiSlider> sliders = new ArrayList<>();
    private final Map<GuiButton, String> tooltips = new HashMap<>();

    private GuiTextField blacklistField;
    private GuiTextField whitelistField;
    private GuiTextField antiSpamThresholdField;
    private GuiTextField timedMessageIntervalField;
    private GuiTextField backgroundImagePathField;
    private GuiTextField backgroundScaleField;
    private GuiTextField backgroundCropXField;
    private GuiTextField backgroundCropYField;

    private GuiSlider backgroundTransparencySlider;
    private GuiSlider scaleSlider;
    private GuiSlider widthSlider;
    private GuiButton timedMessageModeButton;
    private GuiButton imageQualityButton;

    private int panelX;
    private int panelWidth;
    private int panelTopY;
    private int panelBottomY;
    private int contentViewportTop;
    private int viewportHeight;
    private int scrollOffset;
    private int maxScroll;
    private int contentHeight;

    private int groupBarX;
    private int groupBarY;
    private int groupBarW;
    private int groupBarH;

    private boolean draggingScrollbar;
    private boolean draggingTimedMessageScrollbar;

    private int timedListX = -2000;
    private int timedListY = -2000;
    private int timedListW;
    private int timedListH;
    private int selectedTimedMessageIndex = -1;
    private int timedMessageScrollOffset;
    private int maxTimedMessageScroll;

    private boolean draggingPreview;
    private int chatLeft;
    private int chatRight;
    private int chatTop;
    private int chatBottom;
    private int dragStartX;
    private int dragStartY;

    private double originalChatScale;
    private double originalChatWidth;
    private boolean committed;
    private double lastAppliedScale = -1.0D;
    private double lastAppliedWidth = -1.0D;

    private ConfigGroup selectedGroup = ConfigGroup.BASIC;

    public GuiChatOptimization(GuiScreen parent) {
        this.parentScreen = parent;
        this.settings = ChatOptimizationConfig.INSTANCE;
        this.exampleChat.add(Component.literal(I18n.format("gui.chatopt.preview.line1")));
        this.exampleChat.add(Component.literal(I18n.format("gui.chatopt.preview.line2")));
        this.exampleChat.add(Component.literal(I18n.format("gui.chatopt.preview.line3")));
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.settings = ChatOptimizationConfig.INSTANCE;
        this.buttonList.clear();
        this.scrollableButtons.clear();
        this.textFields.clear();
        this.sliders.clear();
        this.tooltips.clear();
        this.scrollOffset = 0;
        this.maxScroll = 0;
        this.contentHeight = 0;
        this.timedMessageScrollOffset = 0;
        this.maxTimedMessageScroll = 0;
        this.draggingScrollbar = false;
        this.draggingTimedMessageScrollbar = false;
        this.draggingPreview = false;

        this.originalChatScale = this.mc.options.chatScale().get();
        this.originalChatWidth = this.mc.options.chatWidth().get();
        this.lastAppliedScale = this.originalChatScale;
        this.lastAppliedWidth = this.originalChatWidth;

        this.panelWidth = Math.min(560, Math.max(380, this.width - 24));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelTopY = 32;
        this.panelBottomY = this.height - 55;
        if (this.panelBottomY - this.panelTopY < 180) {
            this.panelTopY = Math.max(16, this.panelBottomY - 180);
        }

        this.groupBarX = this.panelX + 10;
        this.groupBarY = this.panelTopY + 26;
        this.groupBarW = this.panelWidth - 20;
        this.groupBarH = 28;
        this.contentViewportTop = this.groupBarY + this.groupBarH + 8;
        this.viewportHeight = Math.max(80, this.panelBottomY - this.contentViewportTop - 4);

        int controlX = 10;
        int controlWidth = this.panelWidth - 20;
        int halfWidth = (controlWidth - 10) / 2;
        int currentY = 0;

        ToggleGuiButton btnAntiSpam = new ToggleGuiButton(BTN_ANTI_SPAM, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.anti_spam"), settings.enableAntiSpam);
        this.scrollableButtons.add(btnAntiSpam);
        this.tooltips.put(btnAntiSpam, I18n.format("gui.chatopt.tip.anti_spam"));

        ToggleGuiButton btnTimestamp = new ToggleGuiButton(BTN_TIMESTAMP, controlX + halfWidth + 10, currentY,
                halfWidth, 20, I18n.format("gui.chatopt.timestamp"), settings.enableTimestamp);
        this.scrollableButtons.add(btnTimestamp);
        this.tooltips.put(btnTimestamp, I18n.format("gui.chatopt.tip.timestamp"));
        currentY += 25;

        ToggleGuiButton btnAntiSpamScroll = new ToggleGuiButton(BTN_ANTI_SPAM_SCROLL, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.scroll_bottom"), settings.antiSpamScrollToBottom);
        this.scrollableButtons.add(btnAntiSpamScroll);
        this.tooltips.put(btnAntiSpamScroll, I18n.format("gui.chatopt.tip.scroll_bottom"));

        this.antiSpamThresholdField = createField(5, controlX + controlWidth - 48, currentY, 48,
                String.valueOf(settings.antiSpamThresholdSeconds));
        currentY += 30;

        ToggleGuiButton btnTimedEnable = new ToggleGuiButton(BTN_TIMED_ENABLE, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.timed_message"), settings.enableTimedMessage);
        this.scrollableButtons.add(btnTimedEnable);
        this.tooltips.put(btnTimedEnable, I18n.format("gui.chatopt.tip.timed_message"));

        this.timedMessageModeButton = new ThemedButton(BTN_TIMED_MODE, controlX + halfWidth + 10, currentY, halfWidth,
                20, I18n.format("gui.chatopt.mode", settings.timedMessageMode.getDisplayName()));
        this.scrollableButtons.add(this.timedMessageModeButton);
        this.tooltips.put(this.timedMessageModeButton, I18n.format("gui.chatopt.tip.mode"));
        currentY += 25;

        this.timedMessageIntervalField = createField(17, controlX + controlWidth - 48, currentY, 48,
                String.valueOf(settings.timedMessageIntervalSeconds));
        currentY += 30;

        int listButtonWidth = (controlWidth - 20) / 3;
        this.scrollableButtons.add(new ThemedButton(BTN_TIMED_ADD, controlX, currentY + 90, listButtonWidth, 20,
                "§a" + I18n.format("gui.common.add")));
        this.scrollableButtons.add(new ThemedButton(BTN_TIMED_EDIT, controlX + listButtonWidth + 10, currentY + 90,
                listButtonWidth, 20, "§e" + I18n.format("gui.common.edit")));
        this.scrollableButtons.add(new ThemedButton(BTN_TIMED_DELETE, controlX + 2 * (listButtonWidth + 10),
                currentY + 90, listButtonWidth, 20, "§c" + I18n.format("gui.common.delete")));
        currentY += 120;

        ToggleGuiButton btnBlacklist = new ToggleGuiButton(BTN_BLACKLIST, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.enable_blacklist"), settings.enableBlacklist);
        this.scrollableButtons.add(btnBlacklist);
        this.tooltips.put(btnBlacklist, I18n.format("gui.chatopt.tip.enable_blacklist"));

        ToggleGuiButton btnWhitelist = new ToggleGuiButton(BTN_WHITELIST, controlX + halfWidth + 10, currentY,
                halfWidth, 20, I18n.format("gui.chatopt.enable_whitelist"), settings.enableWhitelist);
        this.scrollableButtons.add(btnWhitelist);
        this.tooltips.put(btnWhitelist, I18n.format("gui.chatopt.tip.enable_whitelist"));
        currentY += 32;

        this.blacklistField = createField(8, controlX, currentY, controlWidth, String.join(", ", settings.blacklist));
        currentY += 34;
        this.whitelistField = createField(9, controlX, currentY, controlWidth, String.join(", ", settings.whitelist));
        currentY += 30;

        ToggleGuiButton btnSmooth = new ToggleGuiButton(BTN_SMOOTH, controlX, currentY, controlWidth, 20,
                I18n.format("gui.chatopt.smooth",
                        I18n.format(settings.smooth ? "gui.common.enabled" : "gui.common.disabled")),
                settings.smooth);
        this.scrollableButtons.add(btnSmooth);
        this.tooltips.put(btnSmooth, I18n.format("gui.chatopt.tip.smooth"));
        currentY += 28;

        this.backgroundTransparencySlider = new GuiSlider(BTN_BG_ALPHA, controlX, currentY, controlWidth, 20,
                I18n.format("gui.chatopt.bg_alpha"), "%", 0, 100, settings.backgroundTransparencyPercent, false, true);
        this.sliders.add(this.backgroundTransparencySlider);
        this.tooltips.put(this.backgroundTransparencySlider, I18n.format("gui.chatopt.tip.bg_alpha"));
        currentY += 32;

        int pathFieldWidth = controlWidth - 110;
        this.backgroundImagePathField = createField(14, controlX, currentY, pathFieldWidth,
                settings.backgroundImagePath);
        this.imageQualityButton = new ThemedButton(BTN_IMAGE_QUALITY, controlX + pathFieldWidth + 10, currentY, 100, 20,
                I18n.format("gui.chatopt.quality", settings.imageQuality.getDisplayName()));
        this.scrollableButtons.add(this.imageQualityButton);
        this.tooltips.put(this.imageQualityButton,
                I18n.format("gui.chatopt.tip.quality", settings.imageQuality.getDescription()));
        currentY += 32;

        int smallFieldWidth = (controlWidth - 8) / 3;
        this.backgroundScaleField = createField(23, controlX, currentY, smallFieldWidth,
                String.valueOf(settings.backgroundImageScale));
        this.backgroundCropXField = createField(24, controlX + smallFieldWidth + 4, currentY, smallFieldWidth,
                String.valueOf(settings.backgroundCropX));
        this.backgroundCropYField = createField(25, controlX + 2 * (smallFieldWidth + 4), currentY, smallFieldWidth,
                String.valueOf(settings.backgroundCropY));
        currentY += 32;

        this.scaleSlider = new GuiSlider(BTN_SCALE, controlX, currentY, controlWidth, 20,
                I18n.format("gui.chatopt.scale"), "%", 0, 100, this.originalChatScale * 100.0D, false, true);
        this.sliders.add(this.scaleSlider);
        currentY += 25;

        this.widthSlider = new GuiSlider(BTN_WIDTH, controlX, currentY, controlWidth, 20,
                I18n.format("gui.chatopt.width"), "px", 40, 320, ChatComponent.getWidth(this.originalChatWidth), false,
                true);
        this.sliders.add(this.widthSlider);

        int bottomButtonY = this.panelBottomY + 10;
        int bottomButtonWidth = (this.panelWidth - 20) / 3;
        this.buttonList.add(new ThemedButton(BTN_SAVE, this.panelX, bottomButtonY, bottomButtonWidth, 20,
                "§a" + I18n.format("gui.common.save_and_close")));
        this.buttonList.add(new ThemedButton(BTN_RESET, this.panelX + 10 + bottomButtonWidth, bottomButtonY,
                bottomButtonWidth, 20, "§c" + I18n.format("gui.chatopt.reset_all")));
        this.buttonList.add(new ThemedButton(BTN_CANCEL, this.panelX + 20 + bottomButtonWidth * 2, bottomButtonY,
                bottomButtonWidth, 20, I18n.format("gui.common.cancel")));

        relayoutGroupedContent();
    }

    private GuiTextField createField(int id, int x, int y, int width, String text) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, x, y, width, 20);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setText(text == null ? "" : text);
        this.textFields.add(field);
        return field;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null) {
            return;
        }
        if (button instanceof ToggleGuiButton toggle) {
            toggle.setEnabledState(!toggle.getEnabledState());
            if (button.id == BTN_SMOOTH) {
                button.displayString = I18n.format("gui.chatopt.smooth",
                        I18n.format(toggle.getEnabledState() ? "gui.common.enabled" : "gui.common.disabled"));
            }
            return;
        }

        switch (button.id) {
        case BTN_IMAGE_QUALITY:
            this.settings.imageQuality = this.settings.imageQuality.next();
            this.imageQualityButton.displayString = I18n.format("gui.chatopt.quality",
                    this.settings.imageQuality.getDisplayName());
            this.tooltips.put(this.imageQualityButton,
                    I18n.format("gui.chatopt.tip.quality", this.settings.imageQuality.getDescription()));
            TextureManagerHelper.clearCache();
            return;
        case BTN_TIMED_MODE:
            this.settings.timedMessageMode = this.settings.timedMessageMode.next();
            this.timedMessageModeButton.displayString = I18n.format("gui.chatopt.mode",
                    this.settings.timedMessageMode.getDisplayName());
            return;
        case BTN_TIMED_ADD:
            this.mc.setScreen(new GuiTextInput(this, I18n.format("gui.chatopt.input_new"), newMessage -> {
                if (newMessage != null && !newMessage.trim().isEmpty()) {
                    if (this.settings.timedMessages.size() == 1
                            && this.settings.timedMessages.get(0).trim().isEmpty()) {
                        this.settings.timedMessages.clear();
                    }
                    this.settings.timedMessages.add(newMessage);
                }
                this.mc.setScreen(this);
            }));
            return;
        case BTN_TIMED_EDIT:
            if (this.selectedTimedMessageIndex >= 0
                    && this.selectedTimedMessageIndex < this.settings.timedMessages.size()) {
                String oldMessage = this.settings.timedMessages.get(this.selectedTimedMessageIndex);
                this.mc.setScreen(new GuiTextInput(this, I18n.format("gui.chatopt.input_edit"), oldMessage, edited -> {
                    if (edited != null && !edited.trim().isEmpty() && this.selectedTimedMessageIndex >= 0
                            && this.selectedTimedMessageIndex < this.settings.timedMessages.size()) {
                        this.settings.timedMessages.set(this.selectedTimedMessageIndex, edited);
                    }
                    this.mc.setScreen(this);
                }));
            }
            return;
        case BTN_TIMED_DELETE:
            if (this.selectedTimedMessageIndex >= 0
                    && this.selectedTimedMessageIndex < this.settings.timedMessages.size()) {
                this.settings.timedMessages.remove(this.selectedTimedMessageIndex);
                if (this.settings.timedMessages.isEmpty()) {
                    this.settings.timedMessages.add("");
                }
                this.selectedTimedMessageIndex = -1;
                this.timedMessageScrollOffset = Math.min(this.timedMessageScrollOffset, this.maxTimedMessageScroll);
            }
            return;
        case BTN_SAVE:
            saveAndClose();
            return;
        case BTN_RESET:
            ChatOptimizationConfig.resetToDefaults();
            this.settings = ChatOptimizationConfig.INSTANCE;
            this.selectedTimedMessageIndex = -1;
            this.timedMessageScrollOffset = 0;
            applyChatPreviewOptions(1.0D, 1.0D);
            this.initGui();
            return;
        case BTN_CANCEL:
            restoreOriginalPreviewOptions();
            ChatOptimizationConfig.load();
            this.mc.setScreen(this.parentScreen);
            return;
        default:
            return;
        }
    }

    private void saveAndClose() {
        for (GuiButton button : this.scrollableButtons) {
            if (!(button instanceof ToggleGuiButton toggle)) {
                continue;
            }
            switch (button.id) {
            case BTN_ANTI_SPAM:
                this.settings.enableAntiSpam = toggle.getEnabledState();
                break;
            case BTN_TIMESTAMP:
                this.settings.enableTimestamp = toggle.getEnabledState();
                break;
            case BTN_ANTI_SPAM_SCROLL:
                this.settings.antiSpamScrollToBottom = toggle.getEnabledState();
                break;
            case BTN_BLACKLIST:
                this.settings.enableBlacklist = toggle.getEnabledState();
                break;
            case BTN_WHITELIST:
                this.settings.enableWhitelist = toggle.getEnabledState();
                break;
            case BTN_SMOOTH:
                this.settings.smooth = toggle.getEnabledState();
                break;
            case BTN_TIMED_ENABLE:
                this.settings.enableTimedMessage = toggle.getEnabledState();
                break;
            default:
                break;
            }
        }

        String oldPath = ChatOptimizationConfig.INSTANCE.backgroundImagePath;
        String newPath = TextureManagerHelper.canonicalizeImagePath(this.backgroundImagePathField.getText().trim());
        if (!oldPath.equals(newPath)) {
            TextureManagerHelper.unloadTexture(oldPath);
        }
        this.backgroundImagePathField.setText(newPath);
        this.settings.backgroundImagePath = newPath;
        this.settings.backgroundImageScale = Mth.clamp(parseIntOr(this.backgroundScaleField.getText(), 100), 10, 300);
        this.settings.backgroundCropX = Math.max(0, parseIntOr(this.backgroundCropXField.getText(), 0));
        this.settings.backgroundCropY = Math.max(0, parseIntOr(this.backgroundCropYField.getText(), 0));
        this.settings.backgroundTransparencyPercent = this.backgroundTransparencySlider.getValueInt();
        TextureManagerHelper.clearCache();

        this.settings.blacklist = parseListField(this.blacklistField);
        this.settings.whitelist = parseListField(this.whitelistField);
        this.settings.antiSpamThresholdSeconds = Math.max(1, parseIntOr(this.antiSpamThresholdField.getText(), 15));
        this.settings.timedMessageIntervalSeconds = Math.max(1,
                parseIntOr(this.timedMessageIntervalField.getText(), 60));
        if (this.settings.enableTimedMessage) {
            GlobalEventListener.timedMessageTickCounter = 0;
        }

        ChatOptimizationConfig.save();
        PacketInterceptConfig.load();
        PacketInterceptConfig.save();

        this.committed = true;
        this.mc.options.save();
        this.mc.setScreen(this.parentScreen);
    }

    private List<String> parseListField(GuiTextField field) {
        String raw = field == null ? "" : field.getText();
        List<String> values = new ArrayList<>();
        for (String part : raw.replace('，', ',').split("\\s*,\\s*")) {
            String normalized = part == null ? "" : part.trim();
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return values;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelTopY, this.panelWidth, this.panelBottomY - this.panelTopY);
        GuiTheme.drawTitleBar(this.panelX, this.panelTopY, this.panelWidth, I18n.format("gui.chatopt.title"),
                this.fontRenderer);
        drawGroupTabs(mouseX, mouseY);

        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.enableScissor(this.panelX, this.contentViewportTop, this.panelX + this.panelWidth,
                    this.contentViewportTop + this.viewportHeight);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.panelX, this.contentViewportTop - this.scrollOffset);

            int localMouseX = mouseX - this.panelX;
            int localMouseY = mouseY - (this.contentViewportTop - this.scrollOffset);

            for (GuiButton button : this.scrollableButtons) {
                if (button.visible) {
                    button.drawButton(this.mc, localMouseX, localMouseY, partialTicks);
                }
            }
            for (GuiSlider slider : this.sliders) {
                if (slider.visible) {
                    slider.drawButton(this.mc, localMouseX, localMouseY, partialTicks);
                }
            }
            for (GuiTextField field : this.textFields) {
                if (isTextFieldVisible(field)) {
                    drawThemedTextField(field);
                }
            }

            drawActiveGroupLabels();
            if (this.selectedGroup == ConfigGroup.TIMED) {
                drawTimedMessageList(localMouseX, localMouseY);
            }
            if (this.selectedGroup == ConfigGroup.STYLE && this.backgroundImagePathField.getText().isEmpty()
                    && !this.backgroundImagePathField.isFocused()) {
                this.drawString(this.fontRenderer, I18n.format("gui.chatopt.bg_path_hint"),
                        this.backgroundImagePathField.x + 4,
                        this.backgroundImagePathField.y + (this.backgroundImagePathField.height - 8) / 2, 0xFF808080);
            }

            graphics.pose().popMatrix();
            graphics.disableScissor();
        }

        if (this.maxScroll > 0) {
            int scrollbarX = this.panelX + this.panelWidth - 6;
            int thumbHeight = Math.max(10, (int) ((float) this.viewportHeight
                    / Math.max(this.viewportHeight, this.contentHeight) * this.viewportHeight));
            int thumbY = this.contentViewportTop
                    + (int) ((float) this.scrollOffset / this.maxScroll * (this.viewportHeight - thumbHeight));
            GuiTheme.drawScrollbar(scrollbarX, this.contentViewportTop, 5, this.viewportHeight, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.draggingPreview) {
            this.settings.xOffset += mouseX - this.dragStartX;
            this.settings.yOffset += mouseY - this.dragStartY;
            this.dragStartX = mouseX;
            this.dragStartY = mouseY;
        }

        applyPreviewOptionsFromSliders();

        int bottomButtonY = this.panelBottomY + 10;
        drawCenteredString(this.fontRenderer, I18n.format("gui.chatopt.drag_preview"), this.width / 2,
                bottomButtonY - 15, 0xFFFFFF);
        drawExampleChat();
        drawHoverTooltips(mouseX, mouseY);
    }

    private void applyPreviewOptionsFromSliders() {
        this.settings.backgroundTransparencyPercent = this.backgroundTransparencySlider.getValueInt();
        double chatScale = this.scaleSlider.getValue() / 100.0D;
        double chatWidth = (this.widthSlider.getValue() - 40.0D) / 280.0D;
        applyChatPreviewOptions(chatScale, chatWidth);
    }

    private void applyChatPreviewOptions(double chatScale, double chatWidth) {
        double clampedScale = Mth.clamp(chatScale, 0.0D, 1.0D);
        double clampedWidth = Mth.clamp(chatWidth, 0.0D, 1.0D);
        boolean changed = clampedScale != this.lastAppliedScale || clampedWidth != this.lastAppliedWidth;
        this.mc.options.chatScale().set(clampedScale);
        this.mc.options.chatWidth().set(clampedWidth);
        if (changed) {
            this.lastAppliedScale = clampedScale;
            this.lastAppliedWidth = clampedWidth;
            this.mc.gui.getChat().rescaleChat();
        }
    }

    private void restoreOriginalPreviewOptions() {
        applyChatPreviewOptions(this.originalChatScale, this.originalChatWidth);
    }

    private void drawGroupTabs(int mouseX, int mouseY) {
        drawSectionFrame("功能分组", this.groupBarX, this.groupBarY, this.groupBarW, this.groupBarH);
        ConfigGroup[] groups = ConfigGroup.values();
        int availableWidth = this.groupBarW - 12;
        int tabWidth = availableWidth / groups.length;
        for (int i = 0; i < groups.length; i++) {
            ConfigGroup group = groups[i];
            int x = this.groupBarX + 6 + i * tabWidth;
            int width = i == groups.length - 1 ? this.groupBarW - 12 - i * tabWidth : tabWidth - 4;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= this.groupBarY + 4
                    && mouseY <= this.groupBarY + 24;
            boolean selected = group == this.selectedGroup;
            GuiTheme.UiState state = selected ? GuiTheme.UiState.SELECTED
                    : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            GuiTheme.drawButtonFrameSafe(x, this.groupBarY + 4, width, 20, state);
            drawCenteredString(this.fontRenderer, group.tabLabel, x + width / 2, this.groupBarY + 10,
                    selected ? 0xFFFFFFFF : GuiTheme.getStateTextColor(state));
        }
    }

    private void drawActiveGroupLabels() {
        int labelSpacing = 12;
        switch (this.selectedGroup) {
        case BASIC:
            drawString(this.fontRenderer, I18n.format("gui.chatopt.antispam_sec"), this.antiSpamThresholdField.x - 110,
                    this.antiSpamThresholdField.y + 6, 0xFFFFFF);
            break;
        case TIMED:
            drawString(this.fontRenderer, I18n.format("gui.chatopt.send_interval_sec"),
                    this.timedMessageIntervalField.x - 80, this.timedMessageIntervalField.y + 6, 0xFFFFFF);
            drawString(this.fontRenderer, I18n.format("gui.chatopt.timed_list"), this.timedListX, this.timedListY - 12,
                    0xFFFFFF);
            break;
        case FILTER:
            drawString(this.fontRenderer, I18n.format("gui.chatopt.blacklist_label"), this.blacklistField.x,
                    this.blacklistField.y - labelSpacing, 0xFFFFFF);
            drawString(this.fontRenderer, I18n.format("gui.chatopt.whitelist_label"), this.whitelistField.x,
                    this.whitelistField.y - labelSpacing, 0xFFFFFF);
            break;
        case STYLE:
            drawString(this.fontRenderer, I18n.format("gui.chatopt.bg_path"), this.backgroundImagePathField.x,
                    this.backgroundImagePathField.y - labelSpacing, 0xFFFFFF);
            drawString(this.fontRenderer, I18n.format("gui.chatopt.bg_transform"), this.backgroundScaleField.x,
                    this.backgroundScaleField.y - labelSpacing, 0xFFFFFF);
            drawString(this.fontRenderer, "scale", this.backgroundScaleField.x + 2, this.backgroundScaleField.y + 6,
                    0xFFB0B0B0);
            drawString(this.fontRenderer, "cropX", this.backgroundCropXField.x + 2, this.backgroundCropXField.y + 6,
                    0xFFB0B0B0);
            drawString(this.fontRenderer, "cropY", this.backgroundCropYField.x + 2, this.backgroundCropYField.y + 6,
                    0xFFB0B0B0);
            break;
        default:
            break;
        }
    }

    private void drawTimedMessageList(int mouseX, int mouseY) {
        drawRect(this.timedListX, this.timedListY, this.timedListX + this.timedListW, this.timedListY + this.timedListH,
                0x80000000);
        int itemHeight = 15;
        int visibleItems = Math.max(1, this.timedListH / itemHeight);
        this.maxTimedMessageScroll = Math.max(0, this.settings.timedMessages.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + this.timedMessageScrollOffset;
            if (index >= this.settings.timedMessages.size()) {
                break;
            }
            String message = this.settings.timedMessages.get(index);
            int itemY = this.timedListY + i * itemHeight;
            int bgColor = index == this.selectedTimedMessageIndex ? 0xFF0066AA : 0x00000000;
            drawRect(this.timedListX, itemY, this.timedListX + this.timedListW, itemY + itemHeight, bgColor);
            drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(message, this.timedListW - 10),
                    this.timedListX + 5, itemY + 4, 0xFFFFFF);
        }

        if (this.maxTimedMessageScroll > 0) {
            int scrollbarX = this.timedListX + this.timedListW + 2;
            int thumbHeight = Math.max(10,
                    (int) ((float) visibleItems / this.settings.timedMessages.size() * this.timedListH));
            int thumbY = this.timedListY + (int) ((float) this.timedMessageScrollOffset / this.maxTimedMessageScroll
                    * (this.timedListH - thumbHeight));
            GuiTheme.drawScrollbar(scrollbarX, this.timedListY, 5, this.timedListH, thumbY, thumbHeight);
        }
    }

    private void drawExampleChat() {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null) {
            return;
        }

        float chatScale = (float) this.mc.gui.getChat().getScale();
        int chatWidth = ChatComponent.getWidth(this.mc.options.chatWidth().get());
        int wrapWidth = Mth.floor((float) chatWidth / Math.max(chatScale, 0.001F));
        List<String> lines = new ArrayList<>();
        for (Component component : this.exampleChat) {
            lines.addAll(this.fontRenderer.listFormattedStringToWidth(component.getString(), Math.max(1, wrapWidth)));
        }
        Collections.reverse(lines);

        int baseY = this.mc.getWindow().getGuiScaledHeight() - 48;
        float chatOpacity = (float) (this.mc.options.chatOpacity().get() * 0.9F + 0.1F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(4.0F + this.settings.xOffset, baseY + this.settings.yOffset);
        graphics.pose().scale(chatScale, chatScale);

        int drawn = 0;
        int alpha = Math.round(255.0F * chatOpacity);
        this.chatLeft = 4 + this.settings.xOffset;
        this.chatRight = this.chatLeft + Math.round((chatWidth + 8) * chatScale);
        this.chatBottom = baseY + this.settings.yOffset;

        for (String line : lines) {
            int y = -drawn * 9;
            drawPreviewBackground(graphics, -4, y - 9, chatWidth + 8, 9, alpha);
            graphics.drawString(this.fontRenderer.unwrap(), line, 0, y - 8, 0xFFFFFF | (alpha << 24), true);
            drawn++;
        }

        this.chatTop = this.chatBottom + Math.round((-drawn * 9) * chatScale);
        graphics.pose().popMatrix();
    }

    private void drawPreviewBackground(GuiGraphics graphics, int x, int y, int width, int height, int alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }
        var background = TextureManagerHelper.getIdentifierForPath(this.settings.backgroundImagePath,
                this.settings.imageQuality);
        float transparencyRatio = Mth.clamp(this.settings.backgroundTransparencyPercent / 100.0F, 0.0F, 1.0F);
        float opacityRatio = 1.0F - transparencyRatio;
        if (background != null) {
            int[] textureSize = TextureManagerHelper.getTextureSizeForPath(this.settings.backgroundImagePath,
                    this.settings.imageQuality);
            int textureWidth = textureSize != null ? textureSize[0] : width;
            int textureHeight = textureSize != null ? textureSize[1] : height;
            int safeScale = Math.max(10, Math.min(300, this.settings.backgroundImageScale));
            float scaleFactor = 100.0F / safeScale;
            int sampleWidth = Math.max(1, Math.min(textureWidth, Math.round(width * scaleFactor)));
            int sampleHeight = Math.max(1, Math.min(textureHeight, Math.round(height * scaleFactor)));
            int u = Math.max(0, this.settings.backgroundCropX);
            int v = Math.max(0, this.settings.backgroundCropY);
            if (u + sampleWidth > textureWidth) {
                u = Math.max(0, textureWidth - sampleWidth);
            }
            if (v + sampleHeight > textureHeight) {
                v = Math.max(0, textureHeight - sampleHeight);
            }

            int color = (Math.round(alpha * opacityRatio) << 24) | 0x00FFFFFF;
            graphics.blit(RenderPipelines.GUI_TEXTURED, background, x, y, (float) u, (float) v, width, height,
                    sampleWidth, sampleHeight, textureWidth, textureHeight, color);
        } else if (this.settings.backgroundTransparencyPercent < 100) {
            int backgroundAlpha = (int) ((alpha / 2.0F) * opacityRatio);
            graphics.fill(x, y, x + width, y + height, backgroundAlpha << 24);
        }
    }

    private void drawHoverTooltips(int mouseX, int mouseY) {
        int localMouseX = mouseX - this.panelX;
        int localMouseY = mouseY - (this.contentViewportTop - this.scrollOffset);
        for (GuiButton button : this.scrollableButtons) {
            if (!button.visible) {
                continue;
            }
            boolean hovered = localMouseX >= button.x && localMouseX <= button.x + button.width
                    && localMouseY >= button.y && localMouseY <= button.y + button.height;
            if (hovered) {
                String tooltip = this.tooltips.get(button);
                if (tooltip != null) {
                    drawHoveringText(splitTooltipLines(tooltip), mouseX, mouseY);
                    return;
                }
            }
        }

        if (this.selectedGroup == ConfigGroup.STYLE) {
            if (isFieldHovered(mouseX, mouseY, this.backgroundScaleField)) {
                drawHoveringText(splitTooltipLines(I18n.format("gui.chatopt.tip.bg_scale")), mouseX, mouseY);
            } else if (isFieldHovered(mouseX, mouseY, this.backgroundCropXField)) {
                drawHoveringText(splitTooltipLines(I18n.format("gui.chatopt.tip.bg_crop_x")), mouseX, mouseY);
            } else if (isFieldHovered(mouseX, mouseY, this.backgroundCropYField)) {
                drawHoveringText(splitTooltipLines(I18n.format("gui.chatopt.tip.bg_crop_y")), mouseX, mouseY);
            }
        }
    }

    private boolean isFieldHovered(int mouseX, int mouseY, GuiTextField field) {
        if (field == null || !isTextFieldVisible(field)) {
            return false;
        }
        int fieldX = this.panelX + field.x;
        int fieldY = this.contentViewportTop - this.scrollOffset + field.y;
        return mouseX >= fieldX && mouseX <= fieldX + field.width && mouseY >= fieldY
                && mouseY <= fieldY + field.height;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && handleGroupClick(mouseX, mouseY)) {
            return;
        }

        int scrollbarX = this.panelX + this.panelWidth - 6;
        if (this.maxScroll > 0 && mouseButton == 0 && mouseX >= scrollbarX && mouseX <= scrollbarX + 5
                && mouseY >= this.contentViewportTop && mouseY <= this.contentViewportTop + this.viewportHeight) {
            this.draggingScrollbar = true;
            return;
        }

        int translatedX = mouseX - this.panelX;
        int translatedY = mouseY - (this.contentViewportTop - this.scrollOffset);

        if (this.selectedGroup == ConfigGroup.TIMED && this.maxTimedMessageScroll > 0 && mouseButton == 0) {
            int timedScrollbarX = this.timedListX + this.timedListW + 2;
            if (translatedX >= timedScrollbarX && translatedX <= timedScrollbarX + 5 && translatedY >= this.timedListY
                    && translatedY <= this.timedListY + this.timedListH) {
                this.draggingTimedMessageScrollbar = true;
                return;
            }
        }

        if (this.selectedGroup == ConfigGroup.TIMED && translatedX >= this.timedListX
                && translatedX <= this.timedListX + this.timedListW && translatedY >= this.timedListY
                && translatedY <= this.timedListY + this.timedListH) {
            int clickedIndex = (translatedY - this.timedListY) / 15 + this.timedMessageScrollOffset;
            if (clickedIndex >= 0 && clickedIndex < this.settings.timedMessages.size()) {
                this.selectedTimedMessageIndex = clickedIndex;
            }
            return;
        }

        for (GuiButton button : this.scrollableButtons) {
            if (button.visible && button.mousePressed(this.mc, translatedX, translatedY)) {
                button.playPressSound(this.mc.getSoundManager());
                this.actionPerformed(button);
                return;
            }
        }
        for (GuiSlider slider : this.sliders) {
            if (slider.visible && slider.mousePressed(this.mc, translatedX, translatedY)) {
                return;
            }
        }
        for (GuiTextField field : this.textFields) {
            if (isTextFieldVisible(field)) {
                field.mouseClicked(translatedX, translatedY, mouseButton);
            }
        }

        if (mouseButton == 0 && mouseX >= this.chatLeft && mouseX <= this.chatRight && mouseY >= this.chatTop
                && mouseY <= this.chatBottom) {
            this.draggingPreview = true;
            this.dragStartX = mouseX;
            this.dragStartY = mouseY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (this.draggingScrollbar && this.maxScroll > 0) {
            float percent = (float) (mouseY - this.contentViewportTop) / Math.max(1, this.viewportHeight);
            this.scrollOffset = Mth.clamp((int) (percent * this.maxScroll), 0, this.maxScroll);
        }
        if (this.draggingTimedMessageScrollbar && this.maxTimedMessageScroll > 0) {
            float percent = (float) (mouseY - (this.contentViewportTop - this.scrollOffset + this.timedListY))
                    / Math.max(1, this.timedListH);
            this.timedMessageScrollOffset = Mth.clamp((int) (percent * this.maxTimedMessageScroll), 0,
                    this.maxTimedMessageScroll);
        }
        int translatedX = mouseX - this.panelX;
        int translatedY = mouseY - (this.contentViewportTop - this.scrollOffset);
        for (GuiSlider slider : this.sliders) {
            if (slider.visible) {
                slider.mousePressed(this.mc, translatedX, translatedY);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        this.draggingPreview = false;
        this.draggingScrollbar = false;
        this.draggingTimedMessageScrollbar = false;
        for (GuiSlider slider : this.sliders) {
            slider.mouseReleased(mouseX, mouseY);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel == 0) {
            return;
        }

        int mouseX = Mouse.getX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getY() * this.height / this.mc.getWindow().getHeight() - 1;

        int translatedX = mouseX - this.panelX;
        int translatedY = mouseY - (this.contentViewportTop - this.scrollOffset);
        if (this.selectedGroup == ConfigGroup.TIMED && translatedX >= this.timedListX
                && translatedX <= this.timedListX + this.timedListW && translatedY >= this.timedListY
                && translatedY <= this.timedListY + this.timedListH) {
            this.timedMessageScrollOffset = Mth.clamp(this.timedMessageScrollOffset + (wheel > 0 ? -1 : 1), 0,
                    this.maxTimedMessageScroll);
            return;
        }

        if (this.maxScroll > 0) {
            this.scrollOffset = Mth.clamp(this.scrollOffset + (wheel > 0 ? -20 : 20), 0, this.maxScroll);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_LEFT) {
            selectGroupByOffset(-1);
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            selectGroupByOffset(1);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        for (GuiTextField field : this.textFields) {
            if (isTextFieldVisible(field)) {
                field.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (!this.committed) {
            restoreOriginalPreviewOptions();
            ChatOptimizationConfig.load();
        }
        this.mc.options.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean handleGroupClick(int mouseX, int mouseY) {
        ConfigGroup[] groups = ConfigGroup.values();
        int availableWidth = this.groupBarW - 12;
        int tabWidth = availableWidth / groups.length;
        for (int i = 0; i < groups.length; i++) {
            int x = this.groupBarX + 6 + i * tabWidth;
            int width = i == groups.length - 1 ? this.groupBarW - 12 - i * tabWidth : tabWidth - 4;
            if (mouseX >= x && mouseX <= x + width && mouseY >= this.groupBarY + 4 && mouseY <= this.groupBarY + 24) {
                setSelectedGroup(groups[i]);
                return true;
            }
        }
        return false;
    }

    private void selectGroupByOffset(int delta) {
        ConfigGroup[] groups = ConfigGroup.values();
        int nextIndex = Mth.clamp(this.selectedGroup.ordinal() + delta, 0, groups.length - 1);
        setSelectedGroup(groups[nextIndex]);
    }

    private void setSelectedGroup(ConfigGroup group) {
        if (group == null || group == this.selectedGroup) {
            return;
        }
        this.selectedGroup = group;
        this.scrollOffset = 0;
        this.selectedTimedMessageIndex = -1;
        for (GuiTextField field : this.textFields) {
            field.setFocused(false);
        }
        relayoutGroupedContent();
    }

    private void relayoutGroupedContent() {
        hideAllGroupControls();
        int controlX = 10;
        int controlWidth = this.panelWidth - 20;
        int halfWidth = (controlWidth - 10) / 2;
        int smallFieldWidth = 48;
        int currentY = 4;

        switch (this.selectedGroup) {
        case BASIC:
            layoutButton(BTN_ANTI_SPAM, controlX, currentY, halfWidth, 20);
            layoutButton(BTN_TIMESTAMP, controlX + halfWidth + 10, currentY, halfWidth, 20);
            currentY += 25;
            layoutButton(BTN_ANTI_SPAM_SCROLL, controlX, currentY, halfWidth, 20);
            layoutField(this.antiSpamThresholdField, controlX + controlWidth - smallFieldWidth, currentY,
                    smallFieldWidth);
            currentY += 25;
            layoutButton(BTN_SMOOTH, controlX, currentY, controlWidth, 20);
            currentY += 28;
            break;
        case TIMED:
            layoutButton(BTN_TIMED_ENABLE, controlX, currentY, halfWidth, 20);
            layoutButton(BTN_TIMED_MODE, controlX + halfWidth + 10, currentY, halfWidth, 20);
            currentY += 25;
            layoutField(this.timedMessageIntervalField, controlX + controlWidth - smallFieldWidth, currentY,
                    smallFieldWidth);
            currentY += 28;
            this.timedListX = controlX;
            this.timedListY = currentY;
            this.timedListW = controlWidth - 10;
            this.timedListH = 84;
            currentY += this.timedListH + 6;
            int listButtonWidth = (controlWidth - 20) / 3;
            layoutButton(BTN_TIMED_ADD, controlX, currentY, listButtonWidth, 20);
            layoutButton(BTN_TIMED_EDIT, controlX + listButtonWidth + 10, currentY, listButtonWidth, 20);
            layoutButton(BTN_TIMED_DELETE, controlX + 2 * (listButtonWidth + 10), currentY, listButtonWidth, 20);
            currentY += 28;
            break;
        case FILTER:
            layoutButton(BTN_BLACKLIST, controlX, currentY, halfWidth, 20);
            layoutButton(BTN_WHITELIST, controlX + halfWidth + 10, currentY, halfWidth, 20);
            currentY += 32;
            layoutField(this.blacklistField, controlX, currentY, controlWidth);
            currentY += 34;
            layoutField(this.whitelistField, controlX, currentY, controlWidth);
            currentY += 30;
            break;
        case STYLE:
            layoutSlider(this.backgroundTransparencySlider, controlX, currentY, controlWidth, 20);
            currentY += 32;
            int pathFieldWidth = controlWidth - 110;
            layoutField(this.backgroundImagePathField, controlX, currentY, pathFieldWidth);
            layoutButton(BTN_IMAGE_QUALITY, controlX + pathFieldWidth + 10, currentY, 100, 20);
            currentY += 32;
            int fieldWidth = (controlWidth - 8) / 3;
            layoutField(this.backgroundScaleField, controlX, currentY, fieldWidth);
            layoutField(this.backgroundCropXField, controlX + fieldWidth + 4, currentY, fieldWidth);
            layoutField(this.backgroundCropYField, controlX + 2 * (fieldWidth + 4), currentY, fieldWidth);
            currentY += 32;
            layoutSlider(this.scaleSlider, controlX, currentY, controlWidth, 20);
            currentY += 25;
            layoutSlider(this.widthSlider, controlX, currentY, controlWidth, 20);
            currentY += 28;
            break;
        default:
            break;
        }

        this.contentHeight = currentY + 8;
        this.maxScroll = Math.max(0, this.contentHeight - this.viewportHeight);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);
    }

    private void hideAllGroupControls() {
        this.timedListX = -2000;
        this.timedListY = -2000;
        this.timedListW = 0;
        this.timedListH = 0;
        for (GuiButton button : this.scrollableButtons) {
            button.visible = false;
            button.x = -2000;
            button.y = -2000;
        }
        for (GuiSlider slider : this.sliders) {
            slider.visible = false;
            slider.x = -2000;
            slider.y = -2000;
        }
        for (GuiTextField field : this.textFields) {
            field.x = -2000;
            field.y = -2000;
        }
    }

    private void layoutButton(int id, int x, int y, int width, int height) {
        GuiButton button = findScrollableButton(id);
        if (button != null) {
            button.visible = true;
            button.x = x;
            button.y = y;
            button.width = width;
            button.height = height;
        }
    }

    private void layoutSlider(GuiSlider slider, int x, int y, int width, int height) {
        if (slider != null) {
            slider.visible = true;
            slider.x = x;
            slider.y = y;
            slider.width = width;
            slider.height = height;
        }
    }

    private void layoutField(GuiTextField field, int x, int y, int width) {
        if (field != null) {
            field.x = x;
            field.y = y;
            field.width = width;
        }
    }

    private GuiButton findScrollableButton(int id) {
        for (GuiButton button : this.scrollableButtons) {
            if (button.id == id) {
                return button;
            }
        }
        return null;
    }

    private boolean isTextFieldVisible(GuiTextField field) {
        return field != null && field.x > -1000 && field.y > -1000;
    }

    private void drawSectionFrame(String title, int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, 0x44202A36);
        drawHorizontalLine(x, x + width, y, 0xFF4FA6D9);
        drawHorizontalLine(x, x + width, y + height, 0xFF35536C);
        drawVerticalLine(x, y, y + height, 0xFF35536C);
        drawVerticalLine(x + width, y, y + height, 0xFF35536C);
        this.drawString(this.fontRenderer, "§b" + title, x + 6, y + 5, 0xFFE8F6FF);
    }

    private int parseIntOr(String text, int fallback) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private List<String> splitTooltipLines(String text) {
        if (text == null || text.isEmpty()) {
            return Arrays.asList("");
        }
        return Arrays.asList(text.replace("\\n", "\n").split("\n"));
    }

    private enum ConfigGroup {
        BASIC("基础"), TIMED("定时消息"), FILTER("黑白名单"), STYLE("外观预览");

        private final String tabLabel;

        ConfigGroup(String tabLabel) {
            this.tabLabel = tabLabel;
        }
    }
}
