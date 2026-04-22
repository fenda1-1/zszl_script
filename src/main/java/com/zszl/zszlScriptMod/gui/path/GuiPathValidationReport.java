package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.validation.PathConfigValidator.Issue;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiPathValidationReport extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final String title;
    private final Runnable onConfirm;
    private final List<Issue> issues;
    private final String confirmLabel;

    public GuiPathValidationReport(GuiScreen parentScreen, String title, String confirmLabel, List<Issue> issues,
            Runnable onConfirm) {
        this.parentScreen = parentScreen;
        this.title = title == null ? "路径检查" : title;
        this.confirmLabel = confirmLabel == null ? "继续" : confirmLabel;
        this.issues = issues == null ? new ArrayList<Issue>() : new ArrayList<Issue>(issues);
        this.onConfirm = onConfirm;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 360;
        int panelHeight = 180;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        this.buttonList.add(new ThemedButton(0, panelX + 12, panelY + panelHeight - 28, 90, 20, "返回"));
        this.buttonList.add(new ThemedButton(1, panelX + panelWidth - 102, panelY + panelHeight - 28, 90, 20, confirmLabel));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.setScreen(parentScreen);
        } else if (button.id == 1) {
            if (onConfirm != null) {
                onConfirm.run();
            }
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 360;
        int panelHeight = 180;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, title, this.fontRenderer);
        List<String> lines = issues.isEmpty()
                ? java.util.Arrays.asList("无校验问题。")
                : java.util.Arrays.asList(buildIssueSummaryText(issues, false),
                        buildIssueBodyText(issues.get(0)));
        int drawY = panelY + 44;
        for (String line : lines) {
            drawString(this.fontRenderer, line, panelX + 12, drawY, GuiTheme.SUB_TEXT);
            drawY += 14;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static String buildEmptyStateText(boolean forPrompt) {
        return forPrompt ? "未发现阻塞问题，可继续。" : "未发现校验问题。";
    }

    public static String buildIssueSummaryText(List<Issue> issues, boolean forPrompt) {
        int count = issues == null ? 0 : issues.size();
        if (count <= 0) {
            return buildEmptyStateText(forPrompt);
        }
        return (forPrompt ? "仍存在 " : "发现 ") + count + " 个问题";
    }

    public static String buildIssueBodyText(Issue issue) {
        if (issue == null) {
            return "";
        }
        String detail = issue.getDetail() == null ? "" : issue.getDetail().trim();
        if (!detail.isEmpty()) {
            return detail;
        }
        String summary = issue.getSummary() == null ? "" : issue.getSummary().trim();
        return summary.isEmpty() ? issue.toCompactText() : summary;
    }

    public static List<Issue> filterIssuesForPrompt(List<Issue> issues) {
        return issues == null ? new ArrayList<Issue>() : new ArrayList<Issue>(issues);
    }
}


