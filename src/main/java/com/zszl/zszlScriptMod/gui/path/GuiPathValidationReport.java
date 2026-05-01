package com.zszl.zszlScriptMod.gui.path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.validation.PathConfigValidator;
import com.zszl.zszlScriptMod.path.validation.PathConfigValidator.Issue;
import com.zszl.zszlScriptMod.system.ProfileManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiPathValidationReport extends ThemedGuiScreen {

    private static final int ROW_HEIGHT = 28;
    private static final String PREFERENCES_FILE_NAME = "path_validation_preferences.json";
    private static boolean ignoreWarningsEnabled = false;
    private static boolean preferencesLoaded = false;

    private final GuiScreen parentScreen;
    private final String title;
    private final String confirmLabel;
    private final Runnable onConfirm;
    private final List<Issue> issues;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public GuiPathValidationReport(GuiScreen parentScreen, String title, String confirmLabel, List<Issue> issues,
            Runnable onConfirm) {
        ensurePreferencesLoaded();
        this.parentScreen = parentScreen;
        this.title = title == null || title.trim().isEmpty() ? "保存前检查" : title;
        this.confirmLabel = confirmLabel == null || confirmLabel.trim().isEmpty() ? "忽略并继续" : confirmLabel;
        this.onConfirm = onConfirm;
        this.issues = issues == null ? Collections.emptyList() : new ArrayList<>(issues);
    }

    public static String buildIssueSummaryText(List<Issue> issues, boolean ignoreWarnings) {
        List<Issue> displayIssues = ignoreWarnings ? filterIssuesForPrompt(issues) : normalizeIssues(issues);
        int errorCount = 0;
        int warningCount = 0;
        for (Issue issue : displayIssues) {
            if (issue == null) {
                continue;
            }
            if (issue.getSeverity() == PathConfigValidator.Severity.ERROR) {
                errorCount++;
            } else {
                warningCount++;
            }
        }
        return ignoreWarnings
                ? (errorCount > 0
                        ? "§c错误 " + errorCount + "  §7已忽略警告，仅显示会阻止保存的错误项。"
                        : "§a已忽略警告  §7当前没有会阻止保存的错误项。")
                : "§c错误 " + errorCount + "  §e警告 " + warningCount + "  §7保存前建议先修掉错误项。";
    }

    public static String buildIssueHeaderText(Issue issue) {
        if (issue == null) {
            return "";
        }
        String level = issue.getSeverity() == PathConfigValidator.Severity.ERROR ? "错误" : "警告";
        String location = issue.getLocationText();
        return location == null || location.trim().isEmpty()
                ? "[" + level + "]"
                : "[" + level + "] " + location;
    }

    public static String buildIssueBodyText(Issue issue) {
        if (issue == null) {
            return "";
        }
        return issue.getSummary() + (issue.getDetail().isEmpty() ? "" : " - " + issue.getDetail());
    }

    public static String buildEmptyStateText(boolean ignoreWarnings) {
        return ignoreWarnings ? "当前没有需要处理的错误项" : "当前没有检测到问题";
    }

    public static List<Issue> normalizeIssues(List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(issues);
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        ensurePreferencesLoaded();
        int panelWidth = Math.min(760, this.width - 40);
        int panelHeight = Math.min(460, this.height - 40);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        listX = panelX + 14;
        listY = panelY + 54;
        listW = panelWidth - 28;
        listH = panelHeight - 108;
        int buttonY = panelY + panelHeight - 34;
        this.buttonList.add(new ThemedButton(0, panelX + 14, buttonY, 110, 20, "返回修改"));
        this.buttonList.add(new ThemedButton(2, panelX + panelWidth - 254, buttonY, 122, 20, buildIgnoreWarningsLabel()));
        this.buttonList.add(new ThemedButton(1, panelX + panelWidth - 124, buttonY, 110, 20, confirmLabel));
        recalcScrollBounds();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.setScreen(parentScreen);
            return;
        }
        if (button.id == 1) {
            Object before = this.mc.screen;
            if (onConfirm != null) {
                onConfirm.run();
            }
            if (this.mc.screen == this || this.mc.screen == before) {
                this.mc.setScreen(parentScreen);
            }
            return;
        }
        if (button.id == 2) {
            ignoreWarningsEnabled = !ignoreWarningsEnabled;
            savePreferences();
            button.displayString = buildIgnoreWarningsLabel();
            recalcScrollBounds();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0 || maxScroll <= 0) {
            return;
        }
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - Integer.signum(dWheel)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = Math.min(760, this.width - 40);
        int panelHeight = Math.min(460, this.height - 40);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, title, this.fontRenderer);

        List<Issue> displayIssues = getDisplayIssues();
        String summary = buildIssueSummaryText(displayIssues, ignoreWarningsEnabled);
        this.drawString(this.fontRenderer, summary, panelX + 14, panelY + 34, 0xFFFFFFFF);

        drawRect(listX, listY, listX + listW, listY + listH, 0x55203038);
        GuiTheme.drawInputFrameSafe(listX, listY, listW, listH, false, true);

        int visibleRows = Math.max(1, listH / ROW_HEIGHT);
        int end = Math.min(displayIssues.size(), scrollOffset + visibleRows);
        int rowY = listY + 6;
        for (int i = scrollOffset; i < end; i++) {
            Issue issue = displayIssues.get(i);
            int color = issue.getSeverity() == PathConfigValidator.Severity.ERROR ? 0xFFFF8A8A : 0xFFFFD26A;
            String header = trim(buildIssueHeaderText(issue), listW - 24);
            this.drawString(this.fontRenderer, header, listX + 8, rowY, color);
            String issueSummary = trim(buildIssueBodyText(issue), listW - 24);
            this.drawString(this.fontRenderer, issueSummary, listX + 8, rowY + 12, 0xFFD9E3F0);
            rowY += ROW_HEIGHT;
        }

        if (displayIssues.isEmpty()) {
            GuiTheme.drawEmptyState(listX + listW / 2, listY + listH / 2 - 6,
                    buildEmptyStateText(ignoreWarningsEnabled),
                    this.fontRenderer);
        }

        if (maxScroll > 0) {
            int scrollX = listX + listW - 8;
            int thumbHeight = Math.max(18,
                    (int) ((float) visibleRows / Math.max(visibleRows, displayIssues.size()) * listH));
            int thumbY = listY + (int) ((float) scrollOffset / maxScroll * Math.max(1, listH - thumbHeight));
            GuiTheme.drawScrollbar(scrollX, listY, 6, listH, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String trim(String text, int width) {
        String safe = text == null ? "" : text;
        return this.fontRenderer.trimStringToWidth(safe, Math.max(20, width));
    }

    private List<Issue> getDisplayIssues() {
        return filterIssuesForPrompt(this.issues);
    }

    private void recalcScrollBounds() {
        int visibleRows = Math.max(1, listH / ROW_HEIGHT);
        List<Issue> displayIssues = getDisplayIssues();
        maxScroll = Math.max(0, displayIssues.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private String buildIgnoreWarningsLabel() {
        return "忽略警告: " + (ignoreWarningsEnabled ? "§a开" : "§c关");
    }

    public static List<Issue> filterIssuesForPrompt(List<Issue> issues) {
        ensurePreferencesLoaded();
        if (issues == null || issues.isEmpty()) {
            return Collections.emptyList();
        }
        if (!ignoreWarningsEnabled) {
            return normalizeIssues(issues);
        }
        List<Issue> filtered = new ArrayList<>();
        for (Issue issue : issues) {
            if (issue != null && issue.getSeverity() == PathConfigValidator.Severity.ERROR) {
                filtered.add(issue);
            }
        }
        return filtered;
    }

    private static synchronized void ensurePreferencesLoaded() {
        if (preferencesLoaded) {
            return;
        }
        preferencesLoaded = true;
        ignoreWarningsEnabled = false;
        Path path = getPreferencesPath();
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root != null && root.has("ignoreWarnings")) {
                ignoreWarningsEnabled = root.get("ignoreWarnings").getAsBoolean();
            }
        } catch (Exception ignored) {
            ignoreWarningsEnabled = false;
        }
    }

    private static synchronized void savePreferences() {
        Path path = getPreferencesPath();
        if (path == null) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            JsonObject root = new JsonObject();
            root.addProperty("ignoreWarnings", ignoreWarningsEnabled);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private static Path getPreferencesPath() {
        return ProfileManager.getCurrentProfileDir().resolve(PREFERENCES_FILE_NAME);
    }
}
