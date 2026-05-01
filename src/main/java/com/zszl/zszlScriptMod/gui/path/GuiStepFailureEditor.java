package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiStepFailureEditor extends ThemedGuiScreen {

    private static final int BTN_SAVE = 1;
    private static final int BTN_BACK = 2;
    private static final int BTN_SELECT_SEQUENCE = 11;
    private static final int BTN_RESTORE_DEFAULTS = 12;
    private static final String[] EXHAUSTED_POLICIES = new String[] {
            "END_SEQUENCE",
            "RESTART_SEQUENCE",
            "RUN_SEQUENCE"
    };
    private static final String[] EXHAUSTED_POLICY_LABELS = new String[] {
            "直接结束序列",
            "重试开始序列",
            "执行指定序列"
    };

    private final GuiScreen parent;
    private final List<PathStep> targetSteps = new ArrayList<>();
    private final PathStep displayStep;

    private GuiTextField retryCountField;
    private GuiTextField retryTimeoutField;
    private GuiTextField arrivalToleranceField;
    private GuiButton selectSequenceButton;
    private SimpleDropdown exhaustedPolicyDropdown;
    private String selectedSequenceName = "";
    private String draftRetryCount = null;
    private String draftRetryTimeout = null;
    private String draftArrivalTolerance = null;
    private String draftPolicy = null;
    private String draftSequenceName = null;

    public GuiStepFailureEditor(GuiScreen parent, PathStep step) {
        this(parent,
                step == null ? Collections.emptyList() : Collections.singletonList(step),
                step);
    }

    public GuiStepFailureEditor(GuiScreen parent, List<PathStep> steps, PathStep displayStep) {
        this.parent = parent;
        if (steps != null) {
            for (PathStep step : steps) {
                if (step != null) {
                    this.targetSteps.add(step);
                }
            }
        }
        this.displayStep = displayStep != null
                ? displayStep
                : (this.targetSteps.isEmpty() ? null : this.targetSteps.get(0));
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int panelW = Math.min(420, this.width - 24);
        int panelH = 338;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        int fieldX = panelX + 14;
        int fieldW = panelW - 28;
        int y = panelY + 38;

        PathStep referenceStep = getReferenceStep();
        int defaultRetryCount = referenceStep == null
                ? PathSequenceManager.getDefaultStepRetryCount()
                : referenceStep.getRetryCount();
        int defaultTimeout = referenceStep == null
                ? PathSequenceManager.getDefaultStepPathRetryTimeoutSeconds()
                : referenceStep.getPathRetryTimeoutSeconds();
        int defaultArrivalTolerance = referenceStep == null
                ? PathSequenceManager.getDefaultStepArrivalToleranceBlocks()
                : referenceStep.getArrivalToleranceBlocks();

        retryCountField = new GuiTextField(4001, fontRenderer, fieldX, y, fieldW, 18);
        retryCountField.setText(draftRetryCount != null ? draftRetryCount : String.valueOf(defaultRetryCount));
        y += 34;

        retryTimeoutField = new GuiTextField(4002, fontRenderer, fieldX, y, fieldW, 18);
        retryTimeoutField.setText(draftRetryTimeout != null ? draftRetryTimeout : String.valueOf(defaultTimeout));
        y += 34;

        arrivalToleranceField = new GuiTextField(4003, fontRenderer, fieldX, y, fieldW, 18);
        arrivalToleranceField.setText(draftArrivalTolerance != null
                ? draftArrivalTolerance
                : String.valueOf(defaultArrivalTolerance));
        y += 34;

        exhaustedPolicyDropdown = new SimpleDropdown(fieldX, y, fieldW, 20, EXHAUSTED_POLICY_LABELS);
        exhaustedPolicyDropdown.setValue(policyToDisplay(draftPolicy != null
                ? draftPolicy
                : (referenceStep == null ? "END_SEQUENCE" : referenceStep.getRetryExhaustedPolicy())));
        y += 40;

        selectedSequenceName = draftSequenceName != null
                ? draftSequenceName
                : (referenceStep == null ? "" : referenceStep.getRetryExhaustedSequenceName());
        selectSequenceButton = new ThemedButton(BTN_SELECT_SEQUENCE, fieldX, y, fieldW, 20, "");
        buttonList.add(selectSequenceButton);

        int btnY = panelY + panelH - 28;
        buttonList.add(new ThemedButton(BTN_RESTORE_DEFAULTS, panelX + 14, btnY, 92, 20, "§e恢复默认值"));
        buttonList.add(new ThemedButton(BTN_SAVE, panelX + panelW - 184, btnY, 80, 20, "保存"));
        buttonList.add(new ThemedButton(BTN_BACK, panelX + panelW - 94, btnY, 80, 20, "返回"));
        refreshControls();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_SAVE:
                applySavedSettings();
                mc.setScreen(parent);
                break;
            case BTN_BACK:
                mc.setScreen(parent);
                break;
            case BTN_SELECT_SEQUENCE:
                if (!isRunSequenceSelected()) {
                    return;
                }
                captureDraftState();
                mc.setScreen(new GuiSequenceSelector(this, seq -> {
                    selectedSequenceName = seq == null ? "" : seq.trim();
                    draftSequenceName = selectedSequenceName;
                    mc.setScreen(this);
                    refreshControls();
                }));
                break;
            case BTN_RESTORE_DEFAULTS:
                populateBuiltInDefaults();
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelW = Math.min(420, this.width - 24);
        int panelH = 338;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "寻路中断自动重试", fontRenderer);

        drawString(fontRenderer, "重试次数", retryCountField.x, retryCountField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "停留尝试限时(秒)", retryTimeoutField.x, retryTimeoutField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "到达目的地范围判定", arrivalToleranceField.x, arrivalToleranceField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "重试耗尽后", exhaustedPolicyDropdown.x, exhaustedPolicyDropdown.y - 10, 0xFFFFFF);
        if (isRunSequenceSelected()) {
            drawString(fontRenderer, "目标序列", selectSequenceButton.x, selectSequenceButton.y - 10, 0xFFFFFF);
        }

        int noteY = isRunSequenceSelected() ? selectSequenceButton.y + 30 : exhaustedPolicyDropdown.y + 30;
        fontRenderer.drawSplitString(buildHelpText(), panelX + 14, noteY, panelW - 28, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawThemedTextField(retryCountField);
        drawThemedTextField(retryTimeoutField);
        drawThemedTextField(arrivalToleranceField);
        exhaustedPolicyDropdown.draw(mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (exhaustedPolicyDropdown != null && exhaustedPolicyDropdown.expanded) {
                exhaustedPolicyDropdown.collapse();
                return;
            }
            mc.setScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        retryCountField.textboxKeyTyped(typedChar, keyCode);
        retryTimeoutField.textboxKeyTyped(typedChar, keyCode);
        arrivalToleranceField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (exhaustedPolicyDropdown != null && exhaustedPolicyDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            refreshControls();
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        retryCountField.mouseClicked(mouseX, mouseY, mouseButton);
        retryTimeoutField.mouseClicked(mouseX, mouseY, mouseButton);
        arrivalToleranceField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void applySavedSettings() {
        PathStep referenceStep = getReferenceStep();
        String policy = getSelectedPolicy();
        int retryCount = parseInt(retryCountField,
                referenceStep == null ? PathSequenceManager.getDefaultStepRetryCount() : referenceStep.getRetryCount());
        int timeoutSeconds = parseInt(retryTimeoutField, referenceStep == null
                ? PathSequenceManager.getDefaultStepPathRetryTimeoutSeconds()
                : referenceStep.getPathRetryTimeoutSeconds());
        int arrivalToleranceBlocks = parseInt(arrivalToleranceField, referenceStep == null
                ? PathSequenceManager.getDefaultStepArrivalToleranceBlocks()
                : referenceStep.getArrivalToleranceBlocks());
        String targetSequenceName = "RUN_SEQUENCE".equals(policy) ? selectedSequenceName : "";

        PathSequenceManager.updateDefaultStepRetrySettings(retryCount, timeoutSeconds, arrivalToleranceBlocks);
        if (parent instanceof GuiPathManager) {
            ((GuiPathManager) parent).applyStepFailureSettings(targetSteps, retryCount, timeoutSeconds,
                    arrivalToleranceBlocks, policy, targetSequenceName);
            return;
        }
        for (PathStep targetStep : getTargetSteps()) {
            targetStep.setRetryCount(retryCount);
            targetStep.setPathRetryTimeoutSeconds(timeoutSeconds);
            targetStep.setArrivalToleranceBlocks(arrivalToleranceBlocks);
            targetStep.setRetryExhaustedPolicy(policy);
            targetStep.setRetryExhaustedSequenceName(targetSequenceName);
        }
    }

    private void captureDraftState() {
        draftRetryCount = retryCountField == null ? draftRetryCount : retryCountField.getText();
        draftRetryTimeout = retryTimeoutField == null ? draftRetryTimeout : retryTimeoutField.getText();
        draftArrivalTolerance = arrivalToleranceField == null ? draftArrivalTolerance : arrivalToleranceField.getText();
        draftPolicy = getSelectedPolicy();
        draftSequenceName = selectedSequenceName == null ? "" : selectedSequenceName.trim();
    }

    private int parseInt(GuiTextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void populateBuiltInDefaults() {
        if (retryCountField != null) {
            retryCountField.setText(String.valueOf(PathSequenceManager.getBuiltinStepRetryCount()));
        }
        if (retryTimeoutField != null) {
            retryTimeoutField.setText(String.valueOf(PathSequenceManager.getBuiltinStepPathRetryTimeoutSeconds()));
        }
        if (arrivalToleranceField != null) {
            arrivalToleranceField.setText(String.valueOf(PathSequenceManager.getBuiltinStepArrivalToleranceBlocks()));
        }
        selectedSequenceName = "";
        draftSequenceName = "";
        if (exhaustedPolicyDropdown != null) {
            exhaustedPolicyDropdown.setValue(policyToDisplay("END_SEQUENCE"));
        }
        refreshControls();
    }

    private void refreshControls() {
        if (selectSequenceButton == null) {
            return;
        }
        boolean showSequenceSelector = isRunSequenceSelected();
        selectSequenceButton.visible = showSequenceSelector;
        selectSequenceButton.enabled = showSequenceSelector;
        if (!showSequenceSelector) {
            selectSequenceButton.displayString = "";
            return;
        }
        if (selectedSequenceName == null || selectedSequenceName.trim().isEmpty()) {
            selectSequenceButton.displayString = "选择失败后执行序列";
            return;
        }
        String trimmed = selectedSequenceName.trim();
        selectSequenceButton.displayString = trimmed.length() > 20
                ? "目标序列: " + trimmed.substring(0, 20) + "..."
                : "目标序列: " + trimmed;
    }

    private boolean isRunSequenceSelected() {
        return "RUN_SEQUENCE".equals(getSelectedPolicy());
    }

    private String buildHelpText() {
        StringBuilder text = new StringBuilder();
        if (getTargetStepCount() > 1) {
            text.append("当前会批量修改选中的 ")
                    .append(getTargetStepCount())
                    .append(" 个步骤。");
        }
        text.append("说明：当前步骤有坐标寻路时，会监测人物是否长时间原地不动。超时后会重新发送寻路命令并扣减重试次数。")
                .append("新建步骤会沿用最近一次保存的重试次数和停留限时；点击“恢复默认值”可回到 ")
                .append(PathSequenceManager.getBuiltinStepRetryCount())
                .append(" / ")
                .append(PathSequenceManager.getBuiltinStepPathRetryTimeoutSeconds())
                .append("，到达范围默认 ")
                .append(PathSequenceManager.getBuiltinStepArrivalToleranceBlocks())
                .append("。")
                .append("到达范围表示目标方块向外扩几圈；进入范围后会等待寻路自然结束，再执行动作。")
                .append("填写 0 表示该步骤不进行自动重试。")
                .append("选择“执行指定序列”后，失败时会按目标序列自身配置决定前台或后台执行。");
        return text.toString();
    }

    private PathStep getReferenceStep() {
        return displayStep;
    }

    private List<PathStep> getTargetSteps() {
        if (!targetSteps.isEmpty()) {
            return targetSteps;
        }
        if (displayStep == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(displayStep);
    }

    private int getTargetStepCount() {
        return getTargetSteps().size();
    }

    private String getSelectedPolicy() {
        return displayToPolicy(exhaustedPolicyDropdown == null ? "" : exhaustedPolicyDropdown.getValue());
    }

    private String policyToDisplay(String policy) {
        String normalized = policy == null ? "" : policy.trim().toUpperCase();
        for (int i = 0; i < EXHAUSTED_POLICIES.length; i++) {
            if (EXHAUSTED_POLICIES[i].equals(normalized)) {
                return EXHAUSTED_POLICY_LABELS[i];
            }
        }
        return EXHAUSTED_POLICY_LABELS[0];
    }

    private String displayToPolicy(String display) {
        String normalized = display == null ? "" : display.trim();
        for (int i = 0; i < EXHAUSTED_POLICY_LABELS.length; i++) {
            if (EXHAUSTED_POLICY_LABELS[i].equalsIgnoreCase(normalized)) {
                return EXHAUSTED_POLICIES[i];
            }
        }
        return "END_SEQUENCE";
    }

    private final class SimpleDropdown extends Gui {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String[] options;
        private int selectedIndex = 0;
        private boolean expanded = false;

        private SimpleDropdown(int x, int y, int width, int height, String[] options) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.options = options == null ? new String[0] : options;
        }

        private void draw(int mouseX, int mouseY) {
            boolean hoverMain = isInside(mouseX, mouseY, x, y, width, height);
            int border = expanded ? 0xFF76D1FF : (hoverMain ? 0xFF4FA6D9 : 0xFF3F6A8C);
            int bg = hoverMain ? 0xCC203146 : 0xCC152433;
            drawRect(x, y, x + width, y + height, bg);
            drawRect(x, y, x + width + 1, y + 1, border);
            drawRect(x, y + height, x + width + 1, y + height + 1, border);
            drawRect(x, y, x + 1, y + height + 1, border);
            drawRect(x + width, y, x + width + 1, y + height + 1, border);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(getValue(), Math.max(16, width - 18)),
                    x + 5, y + 6, 0xFFEAF7FF);
            fontRenderer.drawString(expanded ? "▲" : "▼", x + width - 10, y + 6, 0xFF9FDFFF);

            if (!expanded) {
                return;
            }

            for (int i = 0; i < options.length; i++) {
                int optionY = y + height + i * height;
                boolean hoverOption = isInside(mouseX, mouseY, x, optionY, width, height);
                boolean selected = i == selectedIndex;
                int optionBg = selected ? 0xEE2B5A7C : (hoverOption ? 0xCC29455E : 0xCC1B2D3D);
                int optionBorder = hoverOption ? 0xFF7ED0FF : 0xFF3B6B8A;
                drawRect(x, optionY, x + width, optionY + height, optionBg);
                drawRect(x, optionY, x + width + 1, optionY + 1, optionBorder);
                drawRect(x, optionY + height, x + width + 1, optionY + height + 1, optionBorder);
                drawRect(x, optionY, x + 1, optionY + height + 1, optionBorder);
                drawRect(x + width, optionY, x + width + 1, optionY + height + 1, optionBorder);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(options[i], Math.max(16, width - 8)),
                        x + 5, optionY + 6, 0xFFFFFFFF);
            }
        }

        private boolean handleClick(int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0) {
                return false;
            }
            if (isInside(mouseX, mouseY, x, y, width, height)) {
                expanded = !expanded;
                return true;
            }
            if (!expanded) {
                return false;
            }
            for (int i = 0; i < options.length; i++) {
                int optionY = y + height + i * height;
                if (isInside(mouseX, mouseY, x, optionY, width, height)) {
                    selectedIndex = i;
                    expanded = false;
                    return true;
                }
            }
            expanded = false;
            return false;
        }

        private String getValue() {
            if (options.length == 0) {
                return "";
            }
            if (selectedIndex < 0 || selectedIndex >= options.length) {
                selectedIndex = 0;
            }
            return options[selectedIndex] == null ? "" : options[selectedIndex];
        }

        private void setValue(String value) {
            String normalized = value == null ? "" : value.trim();
            for (int i = 0; i < options.length; i++) {
                if (options[i] != null && options[i].equalsIgnoreCase(normalized)) {
                    selectedIndex = i;
                    return;
                }
            }
            selectedIndex = 0;
        }

        private void collapse() {
            expanded = false;
        }

        private boolean isInside(int mouseX, int mouseY, int rx, int ry, int rw, int rh) {
            return mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + rh;
        }
    }
}

