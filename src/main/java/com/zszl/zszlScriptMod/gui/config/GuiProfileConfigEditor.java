package com.zszl.zszlScriptMod.gui.config;

import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.system.ProfileShareCodeManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.stream.JsonReader;

public class GuiProfileConfigEditor extends ThemedGuiScreen {

    private static final Pattern JSON_LOCATION_PATTERN = Pattern.compile("line\\s+(\\d+)\\s+column\\s+(\\d+)");

    private final GuiScreen parentScreen;
    private final String profileName;
    private final String relativePath;

    private final List<String> lines = new ArrayList<>();
    private String originalContent = "";

    private int cursorLine = 0;
    private int cursorColumn = 0;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private String statusMessage = "§7Ctrl+S 保存，保存前会自动校验；失败会定位错误行，可一键回滚";
    private int statusColor = 0xFFB8C7D9;
    private boolean dirty = false;
    private int highlightedErrorLine = -1;

    public GuiProfileConfigEditor(GuiScreen parentScreen, String profileName, String relativePath, String content) {
        this.parentScreen = parentScreen;
        this.profileName = profileName == null ? "" : profileName;
        this.relativePath = relativePath == null ? "" : relativePath;
        this.originalContent = content == null ? "" : content;
        setContent(this.originalContent);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int bottomY = panelY + getPanelHeight() - 28;

        this.buttonList.add(new ThemedButton(0, panelX + 10, bottomY, 90, 20, "§a保存当前文件"));
        this.buttonList.add(new ThemedButton(1, panelX + 110, bottomY, 90, 20, "§e重新载入"));
        this.buttonList.add(new ThemedButton(2, panelX + 210, bottomY, 90, 20, "§b复制全文"));
        this.buttonList.add(new ThemedButton(3, panelX + 310, bottomY, 100, 20, "§6回滚到已保存"));
        this.buttonList.add(new ThemedButton(4, panelX + panelWidth - 110, bottomY, 100, 20, "§c返回"));
        recalcScrollBounds();
        clampCursor();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                saveCurrentFile();
                break;
            case 1:
                reloadFromDisk();
                break;
            case 2:
                setClipboardString(getContent());
                setStatus("§a已复制当前配置全文到剪贴板", 0xFF8CFF9E);
                break;
            case 3:
                rollbackToLastSaved();
                break;
            case 4:
                this.mc.displayGuiScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (isInsideEditor(mouseX, mouseY)) {
                if (wheel > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 3);
                } else {
                    scrollOffset = Math.min(maxScroll, scrollOffset + 3);
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && isInsideEditor(mouseX, mouseY)) {
            moveCursorFromMouse(mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (GuiScreen.isCtrlKeyDown()) {
            if (keyCode == Keyboard.KEY_S) {
                saveCurrentFile();
                return;
            }
            if (keyCode == Keyboard.KEY_C) {
                setClipboardString(getContent());
                setStatus("§a已复制当前配置全文到剪贴板", 0xFF8CFF9E);
                return;
            }
            if (keyCode == Keyboard.KEY_V) {
                String text = GuiScreen.getClipboardString();
                if (text != null && !text.isEmpty()) {
                    insertText(text);
                }
                return;
            }
            if (keyCode == Keyboard.KEY_A) {
                cursorLine = lines.size() - 1;
                cursorColumn = lines.isEmpty() ? 0 : lines.get(cursorLine).length();
                ensureCursorVisible();
                return;
            }
        }

        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                this.mc.displayGuiScreen(parentScreen);
                return;
            case Keyboard.KEY_BACK:
                backspace();
                return;
            case Keyboard.KEY_DELETE:
                deleteForward();
                return;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                newline();
                return;
            case Keyboard.KEY_TAB:
                insertText("    ");
                return;
            case Keyboard.KEY_LEFT:
                moveLeft();
                return;
            case Keyboard.KEY_RIGHT:
                moveRight();
                return;
            case Keyboard.KEY_UP:
                moveUp();
                return;
            case Keyboard.KEY_DOWN:
                moveDown();
                return;
            case Keyboard.KEY_HOME:
                cursorColumn = 0;
                ensureCursorVisible();
                return;
            case Keyboard.KEY_END:
                cursorColumn = currentLine().length();
                ensureCursorVisible();
                return;
            case Keyboard.KEY_PRIOR:
                scrollOffset = Math.max(0, scrollOffset - getVisibleLineCount());
                cursorLine = Math.max(0, cursorLine - getVisibleLineCount());
                clampCursor();
                ensureCursorVisible();
                return;
            case Keyboard.KEY_NEXT:
                scrollOffset = Math.min(maxScroll, scrollOffset + getVisibleLineCount());
                cursorLine = Math.min(lines.size() - 1, cursorLine + getVisibleLineCount());
                clampCursor();
                ensureCursorVisible();
                return;
            default:
                break;
        }

        if (typedChar >= 32 && typedChar != 127) {
            insertText(String.valueOf(typedChar));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth,
                "配置编辑器 - " + profileName + " / " + relativePath, this.fontRenderer);

        int pathY = panelY + 28;
        this.drawString(this.fontRenderer, "§7文件: " + relativePath, panelX + 10, pathY, 0xFFC7D3E0);
        this.drawString(this.fontRenderer,
                dirty ? "§e状态: 未保存修改" : "§a状态: 已保存",
                panelX + panelWidth - 120, pathY, dirty ? 0xFFFFD36B : 0xFF90F0A0);

        int statusY = panelY + 42;
        GuiTheme.drawInputFrameSafe(panelX + 10, statusY, panelWidth - 20, 18, false, true);
        this.drawString(this.fontRenderer, statusMessage, panelX + 14, statusY + 5, statusColor);

        int editorX = getEditorX();
        int editorY = getEditorY();
        int editorW = getEditorWidth();
        int editorH = getEditorHeight();

        GuiTheme.drawInputFrameSafe(editorX, editorY, editorW, editorH, true, true);

        int lineNumberWidth = 40;
        drawRect(editorX + lineNumberWidth, editorY + 1, editorX + lineNumberWidth + 1, editorY + editorH - 1,
                0x884A5F75);

        int visibleCount = getVisibleLineCount();
        for (int local = 0; local < visibleCount; local++) {
            int lineIndex = scrollOffset + local;
            if (lineIndex >= lines.size()) {
                break;
            }

            int drawY = editorY + 4 + local * getLineHeight();
            String lineNo = String.valueOf(lineIndex + 1);
            String lineText = lines.get(lineIndex);
            boolean errorLine = lineIndex == highlightedErrorLine;

            if (errorLine) {
                drawRect(editorX + 1, drawY - 1, editorX + editorW - 9, drawY + getLineHeight() - 1, 0x44CC4C4C);
            }

            this.drawString(this.fontRenderer, "§7" + lineNo,
                    editorX + lineNumberWidth - 4 - this.fontRenderer.getStringWidth(lineNo),
                    drawY, 0xFF9EB1C5);

            int maxTextWidth = Math.max(10, editorW - lineNumberWidth - 10);
            String clipped = this.fontRenderer.trimStringToWidth(lineText, maxTextWidth);
            this.drawString(this.fontRenderer, clipped, editorX + lineNumberWidth + 6, drawY, 0xFFE8F1FA);

            if (lineIndex == cursorLine && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cursorX = editorX + lineNumberWidth + 6
                        + this.fontRenderer.getStringWidth(clampTextToCursor(lineText, cursorColumn, maxTextWidth));
                drawRect(cursorX, drawY - 1, cursorX + 1, drawY + 9, 0xFFFFFFFF);
            }
        }

        if (maxScroll > 0) {
            int scrollbarX = editorX + editorW - 8;
            int thumbHeight = Math.max(18, (int) ((visibleCount / (float) Math.max(visibleCount, lines.size())) * editorH));
            int track = Math.max(1, editorH - thumbHeight);
            int thumbY = editorY + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
            GuiTheme.drawScrollbar(scrollbarX, editorY, 6, editorH, thumbY, thumbHeight);
        }

        this.drawString(this.fontRenderer,
                "§7行: " + (cursorLine + 1) + "  列: " + (cursorColumn + 1) + "  总行数: " + lines.size(),
                panelX + 10, panelY + panelHeight - 40, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void saveCurrentFile() {
        ValidationResult validation = validateCurrentContent();
        if (!validation.valid) {
            applyValidationFailure(validation);
            return;
        }
        try {
            ProfileShareCodeManager.saveProfileFileContent(profileName, relativePath, getContent());
            originalContent = getContent();
            dirty = false;
            highlightedErrorLine = -1;
            setStatus("§a已保存当前配置文件", 0xFF8CFF9E);
        } catch (Exception e) {
            setStatus("§c保存失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void reloadFromDisk() {
        try {
            originalContent = ProfileShareCodeManager.loadProfileFileContent(profileName, relativePath);
            setContent(originalContent);
            dirty = false;
            highlightedErrorLine = -1;
            setStatus("§a已重新从磁盘载入", 0xFF8CFF9E);
        } catch (Exception e) {
            setStatus("§c重新载入失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void rollbackToLastSaved() {
        setContent(originalContent);
        dirty = false;
        highlightedErrorLine = -1;
        setStatus("§a已回滚到上次保存内容", 0xFF8CFF9E);
    }

    private void setContent(String content) {
        lines.clear();
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
        String[] split = normalized.split("\n", -1);
        for (String line : split) {
            lines.add(line);
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        cursorLine = 0;
        cursorColumn = 0;
        scrollOffset = 0;
        recalcScrollBounds();
        ensureCursorVisible();
        highlightedErrorLine = -1;
    }

    private String getContent() {
        return String.join("\n", lines);
    }

    private void insertText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String current = currentLine();
        String before = current.substring(0, Math.min(cursorColumn, current.length()));
        String after = current.substring(Math.min(cursorColumn, current.length()));

        String[] parts = normalized.split("\n", -1);
        if (parts.length == 1) {
            lines.set(cursorLine, before + parts[0] + after);
            cursorColumn = before.length() + parts[0].length();
        } else {
            lines.set(cursorLine, before + parts[0]);
            int insertLine = cursorLine + 1;
            for (int i = 1; i < parts.length; i++) {
                String segment = parts[i];
                if (i == parts.length - 1) {
                    lines.add(insertLine, segment + after);
                } else {
                    lines.add(insertLine, segment);
                }
                insertLine++;
            }
            cursorLine += parts.length - 1;
            cursorColumn = parts[parts.length - 1].length();
        }
        markDirty();
    }

    private void newline() {
        String current = currentLine();
        String before = current.substring(0, Math.min(cursorColumn, current.length()));
        String after = current.substring(Math.min(cursorColumn, current.length()));
        lines.set(cursorLine, before);
        lines.add(cursorLine + 1, after);
        cursorLine++;
        cursorColumn = 0;
        markDirty();
    }

    private void backspace() {
        if (cursorColumn > 0) {
            String current = currentLine();
            lines.set(cursorLine, current.substring(0, cursorColumn - 1) + current.substring(cursorColumn));
            cursorColumn--;
            markDirty();
            return;
        }
        if (cursorLine > 0) {
            String current = currentLine();
            int previousLength = lines.get(cursorLine - 1).length();
            lines.set(cursorLine - 1, lines.get(cursorLine - 1) + current);
            lines.remove(cursorLine);
            cursorLine--;
            cursorColumn = previousLength;
            markDirty();
        }
    }

    private void deleteForward() {
        String current = currentLine();
        if (cursorColumn < current.length()) {
            lines.set(cursorLine, current.substring(0, cursorColumn) + current.substring(cursorColumn + 1));
            markDirty();
            return;
        }
        if (cursorLine < lines.size() - 1) {
            lines.set(cursorLine, current + lines.get(cursorLine + 1));
            lines.remove(cursorLine + 1);
            markDirty();
        }
    }

    private void moveLeft() {
        if (cursorColumn > 0) {
            cursorColumn--;
        } else if (cursorLine > 0) {
            cursorLine--;
            cursorColumn = lines.get(cursorLine).length();
        }
        ensureCursorVisible();
    }

    private void moveRight() {
        if (cursorColumn < currentLine().length()) {
            cursorColumn++;
        } else if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorColumn = 0;
        }
        ensureCursorVisible();
    }

    private void moveUp() {
        if (cursorLine > 0) {
            cursorLine--;
            cursorColumn = Math.min(cursorColumn, currentLine().length());
            ensureCursorVisible();
        }
    }

    private void moveDown() {
        if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorColumn = Math.min(cursorColumn, currentLine().length());
            ensureCursorVisible();
        }
    }

    private void moveCursorFromMouse(int mouseX, int mouseY) {
        int localY = mouseY - getEditorY() - 4;
        int lineIndex = scrollOffset + Math.max(0, localY / getLineHeight());
        lineIndex = Math.max(0, Math.min(lines.size() - 1, lineIndex));
        cursorLine = lineIndex;

        String line = lines.get(cursorLine);
        int textX = getEditorX() + 46;
        int relativeX = Math.max(0, mouseX - textX);
        int bestColumn = 0;
        for (int i = 1; i <= line.length(); i++) {
            String sub = line.substring(0, i);
            if (this.fontRenderer.getStringWidth(sub) > relativeX) {
                break;
            }
            bestColumn = i;
        }
        cursorColumn = bestColumn;
        ensureCursorVisible();
    }

    private boolean isInsideEditor(int mouseX, int mouseY) {
        return mouseX >= getEditorX() && mouseX <= getEditorX() + getEditorWidth()
                && mouseY >= getEditorY() && mouseY <= getEditorY() + getEditorHeight();
    }

    private void markDirty() {
        dirty = true;
        recalcScrollBounds();
        clampCursor();
        ensureCursorVisible();
        highlightedErrorLine = -1;
    }

    private void recalcScrollBounds() {
        maxScroll = Math.max(0, lines.size() - getVisibleLineCount());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void ensureCursorVisible() {
        clampCursor();
        if (cursorLine < scrollOffset) {
            scrollOffset = cursorLine;
        }
        int visible = getVisibleLineCount();
        if (cursorLine >= scrollOffset + visible) {
            scrollOffset = cursorLine - visible + 1;
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void clampCursor() {
        if (lines.isEmpty()) {
            lines.add("");
        }
        cursorLine = Math.max(0, Math.min(cursorLine, lines.size() - 1));
        cursorColumn = Math.max(0, Math.min(cursorColumn, currentLine().length()));
    }

    private String currentLine() {
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines.get(Math.max(0, Math.min(cursorLine, lines.size() - 1)));
    }

    private String clampTextToCursor(String line, int cursor, int maxTextWidth) {
        int safeCursor = Math.max(0, Math.min(cursor, line.length()));
        return this.fontRenderer.trimStringToWidth(line.substring(0, safeCursor), maxTextWidth);
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    private ValidationResult validateCurrentContent() {
        String normalizedPath = this.relativePath == null ? "" : this.relativePath.trim().toLowerCase(Locale.ROOT);
        String content = getContent();
        if (normalizedPath.endsWith(".json")) {
            return validateJsonContent(content);
        }
        if (normalizedPath.endsWith(".lang")) {
            return validateLangContent(content);
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateJsonContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return ValidationResult.error("JSON 文件不能为空", 1, 1);
        }
        try {
            JsonReader reader = new JsonReader(new StringReader(content));
            reader.setLenient(false);
            new JsonParser().parse(reader);
            return ValidationResult.ok();
        } catch (Exception e) {
            int line = 1;
            int column = 1;
            Matcher matcher = JSON_LOCATION_PATTERN.matcher(String.valueOf(e.getMessage()));
            if (matcher.find()) {
                try {
                    line = Math.max(1, Integer.parseInt(matcher.group(1)));
                    column = Math.max(1, Integer.parseInt(matcher.group(2)));
                } catch (Exception ignored) {
                }
            }
            return ValidationResult.error("JSON 语法错误: " + safeMessage(e.getMessage()), line, column);
        }
    }

    private ValidationResult validateLangContent(String content) {
        String[] split = (content == null ? "" : content).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        Set<String> keys = new LinkedHashSet<>();
        for (int i = 0; i < split.length; i++) {
            String rawLine = split[i];
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = rawLine.indexOf('=');
            if (separator < 0) {
                return ValidationResult.error("lang 文件必须使用 key=value 格式", i + 1, 1);
            }
            String key = rawLine.substring(0, separator).trim();
            if (key.isEmpty()) {
                return ValidationResult.error("lang 键名不能为空", i + 1, 1);
            }
            if (!keys.add(key)) {
                return ValidationResult.error("lang 键重复: " + key, i + 1, 1);
            }
        }
        return ValidationResult.ok();
    }

    private void applyValidationFailure(ValidationResult validation) {
        highlightedErrorLine = validation.line - 1;
        cursorLine = Math.max(0, Math.min(lines.size() - 1, highlightedErrorLine));
        cursorColumn = Math.max(0, validation.column - 1);
        ensureCursorVisible();
        setStatus("§c保存已拦截: 第 " + validation.line + " 行，第 " + validation.column + " 列 - " + validation.message,
                0xFFFF8E8E);
    }

    private String safeMessage(String message) {
        return message == null ? "未知错误" : message.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static final class ValidationResult {
        private final boolean valid;
        private final String message;
        private final int line;
        private final int column;

        private ValidationResult(boolean valid, String message, int line, int column) {
            this.valid = valid;
            this.message = message == null ? "" : message;
            this.line = Math.max(1, line);
            this.column = Math.max(1, column);
        }

        private static ValidationResult ok() {
            return new ValidationResult(true, "", 1, 1);
        }

        private static ValidationResult error(String message, int line, int column) {
            return new ValidationResult(false, message, line, column);
        }
    }

    private int getPanelWidth() {
        return Math.min(920, this.width - 20);
    }

    private int getPanelHeight() {
        return Math.min(560, this.height - 20);
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

    private int getEditorX() {
        return getPanelX() + 10;
    }

    private int getEditorY() {
        return getPanelY() + 64;
    }

    private int getEditorWidth() {
        return getPanelWidth() - 20;
    }

    private int getEditorHeight() {
        return getPanelHeight() - 104;
    }

    private int getVisibleLineCount() {
        return Math.max(1, (getEditorHeight() - 8) / getLineHeight());
    }

    private int getLineHeight() {
        return 10;
    }
}
