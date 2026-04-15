package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiStepFailureEditor extends ThemedGuiScreen {

    private static final int BTN_SAVE = 1;
    private static final int BTN_BACK = 2;
    private static final int BTN_SELECT_SEQUENCE = 11;
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
    private final PathStep step;

    private GuiTextField retryCountField;
    private GuiTextField retryTimeoutField;
    private GuiButton selectSequenceButton;
    private SimpleDropdown exhaustedPolicyDropdown;
    private String selectedSequenceName = "";
    private String draftRetryCount = null;
    private String draftRetryTimeout = null;
    private String draftPolicy = null;
    private String draftSequenceName = null;

    public GuiStepFailureEditor(GuiScreen parent, PathStep step) {
        this.parent = parent;
        this.step = step;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int panelW = Math.min(420, this.width - 24);
        int panelH = 274;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        int fieldX = panelX + 14;
        int fieldW = panelW - 28;
        int y = panelY + 38;

        retryCountField = new GuiTextField(4001, fontRenderer, fieldX, y, fieldW, 18);
        retryCountField.setText(draftRetryCount != null ? draftRetryCount : String.valueOf(step == null ? 3 : step.getRetryCount()));
        y += 34;

        retryTimeoutField = new GuiTextField(4002, fontRenderer, fieldX, y, fieldW, 18);
        retryTimeoutField
                .setText(draftRetryTimeout != null ? draftRetryTimeout : String.valueOf(step == null ? 5 : step.getPathRetryTimeoutSeconds()));
        y += 34;

        exhaustedPolicyDropdown = new SimpleDropdown(fieldX, y, fieldW, 20, EXHAUSTED_POLICY_LABELS);
        exhaustedPolicyDropdown.setValue(policyToDisplay(draftPolicy != null
                ? draftPolicy
                : (step == null ? "END_SEQUENCE" : step.getRetryExhaustedPolicy())));
        y += 40;

        selectedSequenceName = draftSequenceName != null
                ? draftSequenceName
                : (step == null ? "" : step.getRetryExhaustedSequenceName());
        selectSequenceButton = new ThemedButton(BTN_SELECT_SEQUENCE, fieldX, y, fieldW, 20, "");
        buttonList.add(selectSequenceButton);

        int btnY = panelY + panelH - 28;
        buttonList.add(new ThemedButton(BTN_SAVE, panelX + panelW - 184, btnY, 80, 20, "保存"));
        buttonList.add(new ThemedButton(BTN_BACK, panelX + panelW - 94, btnY, 80, 20, "返回"));
        refreshControls();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_SAVE:
                saveToStep();
                mc.displayGuiScreen(parent);
                break;
            case BTN_BACK:
                mc.displayGuiScreen(parent);
                break;
            case BTN_SELECT_SEQUENCE:
                if (!isRunSequenceSelected()) {
                    return;
                }
                captureDraftState();
                mc.displayGuiScreen(new GuiSequenceSelector(this, seq -> {
                    selectedSequenceName = seq == null ? "" : seq.trim();
                    draftSequenceName = selectedSequenceName;
                    mc.displayGuiScreen(this);
                    refreshControls();
                }));
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelW = Math.min(420, this.width - 24);
        int panelH = 274;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "寻路中断自动重试", fontRenderer);

        drawString(fontRenderer, "重试次数", retryCountField.x, retryCountField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "停留尝试限时(秒)", retryTimeoutField.x, retryTimeoutField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "重试耗尽后", exhaustedPolicyDropdown.x, exhaustedPolicyDropdown.y - 10, 0xFFFFFF);
        if (isRunSequenceSelected()) {
            drawString(fontRenderer, "目标序列", selectSequenceButton.x, selectSequenceButton.y - 10, 0xFFFFFF);
        }

        int noteY = isRunSequenceSelected() ? selectSequenceButton.y + 30 : exhaustedPolicyDropdown.y + 30;
        fontRenderer.drawSplitString(
                "说明：当前步骤有坐标寻路时，会监测人物是否长时间原地不动。超时后会重新发送寻路命令并扣减重试次数。"
                        + "重试次数默认 3，停留尝试限时默认 5 秒；填写 0 表示该步骤不进行自动重试。"
                        + "选择“执行指定序列”后，失败时会按目标序列自身配置决定前台或后台执行。",
                panelX + 14, noteY, panelW - 28, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawThemedTextField(retryCountField);
        drawThemedTextField(retryTimeoutField);
        exhaustedPolicyDropdown.draw(mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (exhaustedPolicyDropdown != null && exhaustedPolicyDropdown.expanded) {
                exhaustedPolicyDropdown.collapse();
                return;
            }
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        retryCountField.textboxKeyTyped(typedChar, keyCode);
        retryTimeoutField.textboxKeyTyped(typedChar, keyCode);
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
    }

    private void saveToStep() {
        if (step == null) {
            return;
        }
        String policy = getSelectedPolicy();
        step.setRetryCount(parseInt(retryCountField, step.getRetryCount()));
        step.setPathRetryTimeoutSeconds(parseInt(retryTimeoutField, step.getPathRetryTimeoutSeconds()));
        step.setRetryExhaustedPolicy(policy);
        step.setRetryExhaustedSequenceName("RUN_SEQUENCE".equals(policy) ? selectedSequenceName : "");
    }

    private void captureDraftState() {
        draftRetryCount = retryCountField == null ? draftRetryCount : retryCountField.getText();
        draftRetryTimeout = retryTimeoutField == null ? draftRetryTimeout : retryTimeoutField.getText();
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
