package com.zszl.zszlScriptMod.gui.changelog;

import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.HtmlEntity;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.ExternalLinkOpener;
import com.zszl.zszlScriptMod.utils.UpdateChecker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GuiChangelog extends ThemedGuiScreen {

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_H1 = 1;
    private static final int TYPE_H2 = 2;
    private static final int TYPE_H3 = 3;
    private static final int TYPE_QUOTE = 4;
    private static final int TYPE_LIST = 5;
    private static final int TYPE_SEPARATOR = 6;
    private static final int TYPE_SPACER = 7;

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern RAW_URL = Pattern.compile("(https?://[^\\s)]+)");

    private static final class RenderEntry {
        private final FormattedCharSequence sequence;
        private final int type;
        private final String url;

        private RenderEntry(FormattedCharSequence sequence, int type, String url) {
            this.sequence = sequence;
            this.type = type;
            this.url = url;
        }
    }

    private static final class TocEntry {
        private final String text;
        private final int yPosition;
        private final int level;

        private TocEntry(String text, int yPosition, int level) {
            this.text = text;
            this.yPosition = yPosition;
            this.level = level;
        }
    }

    private static final class InlineParseResult {
        private final MutableComponent component;
        private final String url;

        private InlineParseResult(MutableComponent component, String url) {
            this.component = component;
            this.url = url;
        }
    }

    private final GuiScreen parentScreen;
    private String markdownContent;
    private final List<RenderEntry> renderedLines = new ArrayList<>();
    private final List<TocEntry> tocEntries = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int tocScrollOffset = 0;
    private int maxTocScroll = 0;
    private int totalContentHeight = 0;
    private int tocWidth = 140;
    private int contentX;
    private int contentWidth;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int tocX;
    private int tocTop;
    private int tocBottom;
    private boolean draggingContentScrollbar = false;
    private int dragContentStartMouseY = 0;
    private int dragContentStartScrollOffset = 0;
    private boolean draggingTocScrollbar = false;
    private int dragTocStartMouseY = 0;
    private int dragTocStartScrollOffset = 0;

    public GuiChangelog(GuiScreen parent, String content) {
        this.parentScreen = parent;
        this.markdownContent = content == null ? I18n.format("gui.changelog.empty_content") : content;
    }

    private void returnToMainMenuOverlay() {
        if (parentScreen != null) {
            mc.setScreen(parentScreen);
            return;
        }
        GuiInventory.openOverlayScreen();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        this.panelWidth = Math.min(760, this.width - 40);
        this.panelHeight = Math.min(360, this.height - 30);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.tocX = this.panelX + 12;
        this.tocTop = this.panelY + 36;
        this.tocBottom = this.panelY + this.panelHeight - 36;

        this.contentX = this.tocX + this.tocWidth + 12;
        this.contentWidth = (this.panelX + this.panelWidth - 12) - this.contentX;

        this.buttonList.add(new ThemedButton(0, this.width / 2 - 50, this.panelY + this.panelHeight - 26, 100, 18,
                I18n.format("gui.common.back")));

        parseContent();
    }

    private void parseContent() {
        this.renderedLines.clear();
        this.tocEntries.clear();

        Node document = MARKDOWN_PARSER.parse(this.markdownContent == null ? "" : this.markdownContent);
        int currentY = 0;
        for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
            currentY = appendBlock(node, currentY, false, 0);
        }

        totalContentHeight = 0;
        for (RenderEntry entry : renderedLines) {
            totalContentHeight += getEntryHeight(entry);
        }

        int viewHeight = Math.max(1, this.tocBottom - this.tocTop - 8);
        maxScroll = Math.max(0, totalContentHeight - viewHeight);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        int tocVisibleRows = Math.max(1, (this.tocBottom - (this.tocTop + 20) - 4) / 12);
        maxTocScroll = Math.max(0, tocEntries.size() - tocVisibleRows);
        tocScrollOffset = MathHelper.clamp(tocScrollOffset, 0, maxTocScroll);
    }

    private int appendBlock(Node node, int currentY, boolean quoteContext, int listDepth) {
        if (node == null) {
            return currentY;
        }

        if (node instanceof Heading heading) {
            int type = headingTypeForLevel(heading.getLevel());
            String tocTitle = collectPlainText(heading).trim();
            if (!tocTitle.isEmpty()) {
                tocEntries.add(new TocEntry(tocTitle, currentY, Math.min(3, Math.max(1, heading.getLevel()))));
            }
            InlineParseResult rendered = renderInlineChildren(heading);
            currentY = appendWrappedComponent(applyPrefix(rendered.component, "", type), type, rendered.url, currentY);
            return appendSpacer(currentY);
        }

        if (node instanceof Paragraph paragraph) {
            InlineParseResult rendered = renderInlineChildren(paragraph);
            int type = quoteContext ? TYPE_QUOTE : TYPE_NORMAL;
            String prefix = quoteContext ? "| " : "";
            return appendWrappedComponent(applyPrefix(rendered.component, prefix, type), type, rendered.url, currentY);
        }

        if (node instanceof BlockQuote blockQuote) {
            for (Node child = blockQuote.getFirstChild(); child != null; child = child.getNext()) {
                currentY = appendBlock(child, currentY, true, listDepth);
            }
            return appendSpacer(currentY);
        }

        if (node instanceof BulletList || node instanceof OrderedList) {
            int itemIndex = 1;
            for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof ListItem listItem) {
                    currentY = appendListItem(listItem, currentY, quoteContext, listDepth, itemIndex++);
                }
            }
            return currentY;
        }

        if (node instanceof ThematicBreak) {
            return appendWrappedComponent(Component.literal("----------------------------------------------------------------")
                    .withStyle(styleForType(TYPE_SEPARATOR)), TYPE_SEPARATOR, null, currentY);
        }

        if (node instanceof FencedCodeBlock fencedCodeBlock) {
            return appendCodeBlock(fencedCodeBlock.getContentChars().toString(), currentY, quoteContext);
        }

        if (node instanceof IndentedCodeBlock indentedCodeBlock) {
            return appendCodeBlock(indentedCodeBlock.getContentChars().toString(), currentY, quoteContext);
        }

        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            currentY = appendBlock(child, currentY, quoteContext, listDepth);
        }
        return currentY;
    }

    private int appendListItem(ListItem item, int currentY, boolean quoteContext, int listDepth, int itemIndex) {
        InlineParseResult rendered = renderListItemContent(item);
        if (!rendered.component.getString().trim().isEmpty()) {
            String indent = listDepth <= 0 ? "" : "  ".repeat(listDepth);
            String marker = item.getParent() instanceof OrderedList ? (itemIndex + ". ") : "• ";
            int type = quoteContext ? TYPE_QUOTE : TYPE_LIST;
            String prefix = (quoteContext ? "| " : "") + indent + marker;
            currentY = appendWrappedComponent(applyPrefix(rendered.component, prefix, type), type, rendered.url,
                    currentY);
        }
        for (Node child = item.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof BulletList || child instanceof OrderedList) {
                currentY = appendBlock(child, currentY, quoteContext, listDepth + 1);
            }
        }
        return currentY;
    }

    private int appendCodeBlock(String text, int currentY, boolean quoteContext) {
        String[] lines = (text == null ? "" : text.replace("\r", "")).split("\n", -1);
        int type = quoteContext ? TYPE_QUOTE : TYPE_NORMAL;
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                currentY = appendSpacer(currentY);
                continue;
            }
            MutableComponent component = Component.literal(line).withStyle(style -> style.withColor(ChatFormatting.YELLOW));
            String prefix = quoteContext ? "| " : "";
            currentY = appendWrappedComponent(applyPrefix(component, prefix, type), type, null, currentY);
        }
        return appendSpacer(currentY);
    }

    private int appendWrappedComponent(Component component, int type, String url, int currentY) {
        if (component == null || component.getString().trim().isEmpty()) {
            return appendSpacer(currentY);
        }

        List<FormattedCharSequence> wrapped = ComponentRenderUtils.wrapComponents(component,
                Math.max(40, this.contentWidth - 8), this.fontRenderer.unwrap());
        if (wrapped.isEmpty()) {
            wrapped = java.util.Collections.singletonList(component.getVisualOrderText());
        }

        for (FormattedCharSequence sequence : wrapped) {
            RenderEntry entry = new RenderEntry(sequence, type, url);
            renderedLines.add(entry);
            currentY += getEntryHeight(entry);
        }
        return currentY;
    }

    private int appendSpacer(int currentY) {
        RenderEntry entry = new RenderEntry(null, TYPE_SPACER, null);
        renderedLines.add(entry);
        return currentY + getEntryHeight(entry);
    }

    private int headingTypeForLevel(int level) {
        if (level <= 1) {
            return TYPE_H1;
        }
        if (level == 2) {
            return TYPE_H2;
        }
        return TYPE_H3;
    }

    private InlineParseResult renderListItemContent(ListItem item) {
        MutableComponent component = Component.empty();
        String url = null;
        boolean appended = false;
        for (Node child = item.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof BulletList || child instanceof OrderedList) {
                continue;
            }

            InlineParseResult rendered;
            if (child instanceof Paragraph || child instanceof Heading) {
                rendered = renderInlineChildren(child);
            } else {
                rendered = renderInlineNode(child);
            }

            if (rendered.component.getString().trim().isEmpty()) {
                continue;
            }

            if (appended) {
                component.append(" ");
            }
            component.append(rendered.component);
            if (url == null) {
                url = rendered.url;
            }
            appended = true;
        }
        return new InlineParseResult(component, url);
    }

    private InlineParseResult renderInlineChildren(Node parent) {
        MutableComponent component = Component.empty();
        String firstUrl = null;
        for (Node child = parent.getFirstChild(); child != null; child = child.getNext()) {
            InlineParseResult rendered = renderInlineNode(child);
            if (!rendered.component.getString().isEmpty()) {
                component.append(rendered.component);
            }
            if (firstUrl == null && rendered.url != null && !rendered.url.trim().isEmpty()) {
                firstUrl = rendered.url;
            }
        }
        return new InlineParseResult(component, firstUrl);
    }

    private InlineParseResult renderInlineNode(Node node) {
        if (node == null) {
            return new InlineParseResult(Component.empty(), null);
        }

        if (node instanceof Text text) {
            return renderRawText(text.getChars().toString());
        }
        if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            return new InlineParseResult(Component.literal(" "), null);
        }
        if (node instanceof StrongEmphasis) {
            InlineParseResult inner = renderInlineChildren(node);
            return new InlineParseResult(inner.component.copy().withStyle(style -> style.withBold(true)), inner.url);
        }
        if (node instanceof Emphasis) {
            InlineParseResult inner = renderInlineChildren(node);
            return new InlineParseResult(inner.component.copy().withStyle(style -> style.withItalic(true)), inner.url);
        }
        if (node instanceof Code code) {
            return new InlineParseResult(Component.literal(code.getText().toString())
                    .withStyle(style -> style.withColor(ChatFormatting.YELLOW)), null);
        }
        if (node instanceof Link link) {
            InlineParseResult inner = renderInlineChildren(node);
            String url = link.getUrl().toString();
            MutableComponent label = inner.component.getString().trim().isEmpty()
                    ? Component.literal(url)
                    : inner.component.copy();
            return new InlineParseResult(label.withStyle(style -> style.withColor(ChatFormatting.BLUE)
                    .withUnderlined(true)), url);
        }
        if (node instanceof AutoLink autoLink) {
            String url = autoLink.getText().toString();
            return new InlineParseResult(Component.literal(url).withStyle(style -> style.withColor(ChatFormatting.BLUE)
                    .withUnderlined(true)), url);
        }
        if (node instanceof HtmlEntity htmlEntity) {
            return renderRawText(htmlEntity.getChars().toString()
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">"));
        }
        if (node.getFirstChild() != null) {
            return renderInlineChildren(node);
        }
        return renderRawText(node.getChars().toString());
    }

    private InlineParseResult renderRawText(String text) {
        if (text == null || text.isEmpty()) {
            return new InlineParseResult(Component.empty(), null);
        }

        MutableComponent component = Component.empty();
        java.util.regex.Matcher matcher = RAW_URL.matcher(text);
        int index = 0;
        String firstUrl = null;
        while (matcher.find()) {
            if (matcher.start() > index) {
                component.append(Component.literal(text.substring(index, matcher.start())));
            }
            String url = matcher.group(1);
            component.append(Component.literal(url).withStyle(style -> style.withColor(ChatFormatting.BLUE)
                    .withUnderlined(true)));
            if (firstUrl == null) {
                firstUrl = url;
            }
            index = matcher.end();
        }
        if (index < text.length()) {
            component.append(Component.literal(text.substring(index)));
        }

        return new InlineParseResult(component, firstUrl);
    }

    private String collectPlainText(Node node) {
        if (node == null) {
            return "";
        }
        if (node instanceof Text text) {
            return text.getChars().toString();
        }
        if (node instanceof Code code) {
            return code.getText().toString();
        }
        if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            return " ";
        }
        if (node instanceof Link link && node.getFirstChild() == null) {
            return link.getUrl().toString();
        }
        if (node instanceof AutoLink autoLink) {
            return autoLink.getText().toString();
        }
        if (node instanceof HtmlEntity htmlEntity) {
            return htmlEntity.getChars().toString().replace("&nbsp;", " ");
        }

        StringBuilder builder = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            builder.append(collectPlainText(child));
        }
        return builder.toString();
    }

    private MutableComponent applyPrefix(MutableComponent body, String prefix, int type) {
        MutableComponent output = Component.empty();
        if (prefix != null && !prefix.isEmpty()) {
            output.append(Component.literal(prefix).withStyle(styleForType(type)));
        }
        output.append(body.copy().withStyle(styleForType(type)));
        return output;
    }

    private Style styleForType(int type) {
        return switch (type) {
            case TYPE_H1 -> Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true);
            case TYPE_H2 -> Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true);
            case TYPE_H3 -> Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(true);
            case TYPE_QUOTE -> Style.EMPTY.withColor(ChatFormatting.GRAY);
            case TYPE_LIST -> Style.EMPTY.withColor(ChatFormatting.WHITE);
            case TYPE_SEPARATOR -> Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
            default -> Style.EMPTY.withColor(ChatFormatting.WHITE);
        };
    }

    private int getEntryHeight(RenderEntry entry) {
        switch (entry.type) {
            case TYPE_H1:
                return this.fontRenderer.FONT_HEIGHT + 8;
            case TYPE_H2:
                return this.fontRenderer.FONT_HEIGHT + 6;
            case TYPE_H3:
                return this.fontRenderer.FONT_HEIGHT + 4;
            case TYPE_QUOTE:
                return this.fontRenderer.FONT_HEIGHT + 3;
            case TYPE_LIST:
                return this.fontRenderer.FONT_HEIGHT + 2;
            case TYPE_SEPARATOR:
                return this.fontRenderer.FONT_HEIGHT + 4;
            case TYPE_SPACER:
                return 4;
            default:
                return this.fontRenderer.FONT_HEIGHT + 1;
        }
    }

    private void drawTableOfContents(int mouseX, int mouseY) {
        int tocY = tocTop;
        drawRect(tocX, tocY, tocX + tocWidth, tocBottom, 0x800B0F14);
        drawCenteredString(fontRenderer, I18n.format("gui.changelog.toc"), tocX + tocWidth / 2, tocY, 0xFFFFFF);

        int currentY = tocY + 20;
        int currentTocIndex = -1;
        for (int i = tocEntries.size() - 1; i >= 0; i--) {
            if (scrollOffset >= tocEntries.get(i).yPosition) {
                currentTocIndex = i;
                break;
            }
        }

        int visibleRows = Math.max(1, (tocBottom - currentY - 4) / 12);
        for (int i = 0; i < visibleRows; i++) {
            int actualIndex = i + tocScrollOffset;
            if (actualIndex < 0 || actualIndex >= tocEntries.size()) {
                break;
            }
            TocEntry entry = tocEntries.get(actualIndex);
            int entryX = tocX + (entry.level <= 1 ? 5 : (entry.level == 2 ? 12 : 19));
            int entryWidth = tocWidth - (entryX - tocX) - 5;

            if (currentY + 12 > tocBottom) {
                break;
            }

            boolean isHovered = mouseX >= tocX && mouseX <= tocX + tocWidth && mouseY >= currentY && mouseY < currentY + 12;
            String text = fontRenderer.trimStringToWidth(entry.text, entryWidth);
            int color = actualIndex == currentTocIndex ? 0xFFF4D35E : (isHovered ? 0xFFFFFFFF : 0xFFD5E4F5);
            this.fontRenderer.drawStringWithShadow(text, entryX, currentY, color);
            currentY += 12;
        }

        if (maxTocScroll > 0) {
            int scrollbarX = tocX + tocWidth - 6;
            int scrollbarTop = tocY + 20;
            int scrollbarHeight = tocBottom - scrollbarTop - 4;
            drawRect(scrollbarX, scrollbarTop, scrollbarX + 4, scrollbarTop + scrollbarHeight, 0xFF202732);
            int thumbHeight = Math.max(14, (int) ((float) visibleRows / Math.max(1, tocEntries.size()) * scrollbarHeight));
            int thumbY = scrollbarTop
                    + (int) ((float) tocScrollOffset / Math.max(1, maxTocScroll) * (scrollbarHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
        }
    }

    private void drawFancyTitle(String title, int centerX, int y) {
        int totalWidth = fontRenderer.getStringWidth(title);
        int startX = centerX - totalWidth / 2;
        int x = startX;
        long ticker = System.currentTimeMillis() / 30L;

        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            int mix = (int) ((i * 18 + ticker) % 180);
            float t = mix / 180.0F;

            int r = (int) (255 * (1.0F - t) + 80 * t);
            int g = (int) (210 * (1.0F - t) + 220 * t);
            int b = (int) (40 * (1.0F - t) + 255 * t);
            int color = (r << 16) | (g << 8) | b;

            this.fontRenderer.drawStringWithShadow(ch, x, y, color);
            x += this.fontRenderer.getStringWidth(ch);
        }
    }

    private RenderEntry getHoveredEntry(int mouseX, int mouseY) {
        int contentTop = this.tocTop;
        int contentBottom = this.tocBottom;
        int contentRight = this.panelX + this.panelWidth - 12;
        if (mouseX < this.contentX || mouseX > contentRight || mouseY < contentTop || mouseY > contentBottom) {
            return null;
        }

        int yCursor = contentTop + 3 - scrollOffset;
        for (RenderEntry entry : this.renderedLines) {
            int height = getEntryHeight(entry);
            if (mouseY >= yCursor && mouseY < yCursor + height) {
                return entry;
            }
            yCursor += height;
        }
        return null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        String latest = UpdateChecker.changelogContent;
        if (latest != null && !latest.equals(this.markdownContent)) {
            this.markdownContent = latest;
            parseContent();
        }

        drawGradientRect(0, 0, this.width, this.height, 0xA0000000, 0xD0000000);

        drawRect(this.panelX - 4, this.panelY - 2, this.panelX + this.panelWidth + 6, this.panelY + this.panelHeight + 6,
                0x40000000);
        drawRect(this.panelX - 2, this.panelY - 1, this.panelX + this.panelWidth + 3, this.panelY + this.panelHeight + 3,
                0x25000000);

        drawRect(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xC0181E28);
        drawHorizontalLine(this.panelX, this.panelX + this.panelWidth - 1, this.panelY, 0xFF5A6A80);
        drawHorizontalLine(this.panelX, this.panelX + this.panelWidth - 1, this.panelY + this.panelHeight - 1, 0xFF5A6A80);
        drawVerticalLine(this.panelX, this.panelY, this.panelY + this.panelHeight - 1, 0xFF5A6A80);
        drawVerticalLine(this.panelX + this.panelWidth - 1, this.panelY, this.panelY + this.panelHeight - 1, 0xFF5A6A80);

        drawFancyTitle(I18n.format("gui.changelog.main_title"), this.panelX + this.panelWidth / 2, this.panelY + 10);
        drawTableOfContents(mouseX, mouseY);

        int contentTop = this.tocTop;
        int contentBottom = this.tocBottom;
        int contentRight = this.panelX + this.panelWidth - 12;
        drawRect(this.contentX, contentTop, contentRight, contentBottom, 0x800B0F14);

        int yCursor = contentTop + 3 - scrollOffset;
        RenderEntry hoveredEntry = getHoveredEntry(mouseX, mouseY);

        for (RenderEntry entry : this.renderedLines) {
            int entryHeight = getEntryHeight(entry);
            if (yCursor + entryHeight >= contentTop && yCursor <= contentBottom - this.fontRenderer.FONT_HEIGHT) {
                if (hoveredEntry == entry && entry.type != TYPE_SPACER) {
                    drawRect(contentX + 3, yCursor - 1, contentRight - 3, yCursor + entryHeight - 1, 0x332E8BFF);
                }
                if (entry.type == TYPE_QUOTE) {
                    drawRect(contentX + 4, yCursor - 1, contentX + 6, yCursor + entryHeight - 2, 0xAA6C7A8C);
                }
                GuiGraphics graphics = com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext.current();
                if (graphics != null && entry.sequence != null) {
                    graphics.drawString(this.fontRenderer.unwrap(), entry.sequence, contentX + 8, yCursor, 0xFFFFFF);
                }
            }
            yCursor += entryHeight;
        }

        if (maxScroll > 0) {
            int scrollbarX = this.panelX + this.panelWidth - 10;
            int scrollbarHeight = contentBottom - contentTop;
            drawRect(scrollbarX, contentTop, scrollbarX + 4, contentBottom, 0xFF202732);
            int contentHeight = Math.max(1, totalContentHeight);
            int thumbHeight = Math.max(14, (int) ((float) scrollbarHeight / contentHeight * scrollbarHeight));
            int thumbY = contentTop
                    + (int) ((float) scrollOffset / Math.max(1, maxScroll) * (scrollbarHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            returnToMainMenuOverlay();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            returnToMainMenuOverlay();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            RenderEntry entry = getHoveredEntry(mouseX, mouseY);
            if (entry != null && entry.url != null && !entry.url.trim().isEmpty()) {
                ExternalLinkOpener.open(entry.url.trim(), "changelog-link");
                return;
            }
        }

        int contentScrollbarX = this.panelX + this.panelWidth - 10;
        if (maxScroll > 0 && mouseX >= contentScrollbarX && mouseX <= contentScrollbarX + 8
                && mouseY >= this.tocTop && mouseY <= this.tocBottom) {
            int scrollbarHeight = this.tocBottom - this.tocTop;
            int contentHeight = Math.max(1, totalContentHeight);
            int thumbHeight = Math.max(14, (int) ((float) scrollbarHeight / contentHeight * scrollbarHeight));
            int trackHeight = Math.max(1, scrollbarHeight - thumbHeight);
            int thumbY = this.tocTop
                    + (int) ((float) scrollOffset / Math.max(1, maxScroll) * trackHeight);

            if (mouseY < thumbY || mouseY > thumbY + thumbHeight) {
                float progress = (float) (mouseY - this.tocTop - thumbHeight / 2) / (float) trackHeight;
                this.scrollOffset = MathHelper.clamp(Math.round(progress * maxScroll), 0, maxScroll);
                thumbY = this.tocTop + (int) ((float) scrollOffset / Math.max(1, maxScroll) * trackHeight);
            }

            draggingContentScrollbar = true;
            dragContentStartMouseY = mouseY;
            dragContentStartScrollOffset = scrollOffset;
            return;
        }

        int tocScrollbarX = tocX + tocWidth - 6;
        int tocY = this.tocTop + 20;
        int tocVisibleRows = Math.max(1, (tocBottom - tocY - 4) / 12);
        if (maxTocScroll > 0 && mouseX >= tocScrollbarX && mouseX <= tocScrollbarX + 8
                && mouseY >= tocY && mouseY <= tocBottom) {
            int scrollbarHeight = tocBottom - tocY - 4;
            int thumbHeight = Math.max(14, (int) ((float) tocVisibleRows / Math.max(1, tocEntries.size()) * scrollbarHeight));
            int trackHeight = Math.max(1, scrollbarHeight - thumbHeight);
            int thumbY = tocY + (int) ((float) tocScrollOffset / Math.max(1, maxTocScroll) * trackHeight);

            if (mouseY < thumbY || mouseY > thumbY + thumbHeight) {
                float progress = (float) (mouseY - tocY - thumbHeight / 2) / (float) trackHeight;
                this.tocScrollOffset = MathHelper.clamp(Math.round(progress * maxTocScroll), 0, maxTocScroll);
            }

            draggingTocScrollbar = true;
            dragTocStartMouseY = mouseY;
            dragTocStartScrollOffset = tocScrollOffset;
            return;
        }

        if (mouseX >= tocX && mouseX <= tocX + tocWidth - 8 && mouseY >= tocY && mouseY <= tocBottom) {
            int clickedIndex = (mouseY - tocY) / 12 + tocScrollOffset;
            if (clickedIndex >= 0 && clickedIndex < tocEntries.size()) {
                this.scrollOffset = MathHelper.clamp(tocEntries.get(clickedIndex).yPosition, 0, maxScroll);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }

        int mouseX = 0;
        int mouseY = 0;
        if (this.mc != null && this.width > 0 && this.height > 0) {
            mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getWidth());
            mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getHeight()) - 1;
        }

        int contentRight = this.panelX + this.panelWidth - 12;
        if (mouseX >= this.tocX && mouseX <= this.tocX + this.tocWidth && mouseY >= this.tocTop && mouseY <= this.tocBottom) {
            tocScrollOffset -= dWheel / 120;
            tocScrollOffset = MathHelper.clamp(tocScrollOffset, 0, maxTocScroll);
            return;
        }
        if (mouseX >= this.contentX && mouseX <= contentRight && mouseY >= this.tocTop && mouseY <= this.tocBottom) {
            scrollOffset -= dWheel / 120 * (this.fontRenderer.FONT_HEIGHT + 1);
            scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton == 0) {
            updateContentDrag(mouseY);
            updateTocDrag(mouseY);
        }
    }

    private void updateContentDrag(int mouseY) {
        if (!draggingContentScrollbar || maxScroll <= 0) {
            return;
        }

        int scrollbarHeight = this.tocBottom - this.tocTop;
        int contentHeight = Math.max(1, totalContentHeight);
        int thumbHeight = Math.max(14, (int) ((float) scrollbarHeight / contentHeight * scrollbarHeight));
        int track = Math.max(1, scrollbarHeight - thumbHeight);
        int dy = mouseY - dragContentStartMouseY;
        int deltaScroll = (int) ((float) dy / (float) track * maxScroll);
        scrollOffset = MathHelper.clamp(dragContentStartScrollOffset + deltaScroll, 0, maxScroll);
    }

    private void updateTocDrag(int mouseY) {
        if (!draggingTocScrollbar || maxTocScroll <= 0) {
            return;
        }

        int scrollbarTop = this.tocTop + 20;
        int visibleRows = Math.max(1, (tocBottom - scrollbarTop - 4) / 12);
        int scrollbarHeight = tocBottom - scrollbarTop - 4;
        int thumbHeight = Math.max(14, (int) ((float) visibleRows / Math.max(1, tocEntries.size()) * scrollbarHeight));
        int track = Math.max(1, scrollbarHeight - thumbHeight);
        int dy = mouseY - dragTocStartMouseY;
        int deltaScroll = (int) ((float) dy / (float) track * maxTocScroll);
        tocScrollOffset = MathHelper.clamp(dragTocStartScrollOffset + deltaScroll, 0, maxTocScroll);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            draggingContentScrollbar = false;
            draggingTocScrollbar = false;
        }
    }
}
