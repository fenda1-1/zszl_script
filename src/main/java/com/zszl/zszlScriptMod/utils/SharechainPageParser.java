package com.zszl.zszlScriptMod.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.zszlScriptMod;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SharechainPageParser {

    public static final String MOBILE_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1.1 Mobile/15E148 Safari/604.1";

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[\\w./?%&=:+#~_-]+)");

    private SharechainPageParser() {
    }

    public static PageData fetch(String url) throws IOException {
        return fetch(url, MOBILE_USER_AGENT, 15000);
    }

    public static PageData fetch(String url, String userAgent, int timeoutMs) throws IOException {
        return parse(url, HttpsCompat.readText(url, userAgent, timeoutMs));
    }

    public static String fetchMarkdownViaJsoup(String url, String userAgent, int timeoutMs) throws IOException {
        Document document = HttpsCompat.connect(url)
                .userAgent(userAgent == null || userAgent.trim().isEmpty() ? MOBILE_USER_AGENT : userAgent)
                .timeout(timeoutMs)
                .get();

        Element noteContentDiv = document.selectFirst("div.note-content");
        if (noteContentDiv == null) {
            throw new IOException("div.note-content not found");
        }

        StringBuilder markdownBuilder = new StringBuilder();
        for (Element p : noteContentDiv.select("p")) {
            String lineText = p.text().replace('\u00A0', ' ').trim();
            if (!lineText.isEmpty()) {
                markdownBuilder.append(lineText).append('\n');
            }
        }

        String markdown = markdownBuilder.toString().trim();
        if (!markdown.isEmpty()) {
            return markdown;
        }

        String fallbackText = noteContentDiv.text() == null ? "" : noteContentDiv.text().replace('\u00A0', ' ').trim();
        if (!fallbackText.isEmpty()) {
            return fallbackText;
        }
        throw new IOException("div.note-content parsed empty");
    }

    public static String fetchBodyTextViaJsoup(String url, String userAgent, int timeoutMs) throws IOException {
        Document document = HttpsCompat.connect(url)
                .userAgent(userAgent == null || userAgent.trim().isEmpty() ? MOBILE_USER_AGENT : userAgent)
                .timeout(timeoutMs)
                .get();
        if (document.body() == null) {
            throw new IOException("document.body is null");
        }
        String text = document.body().text();
        if (text == null || text.trim().isEmpty()) {
            throw new IOException("document.body text is empty");
        }
        return text.trim();
    }

    public static String fetchBestMarkdown(String url, String userAgent, int timeoutMs) throws IOException {
        Throwable jsoupError = null;
        try {
            String markdown = fetchMarkdownViaJsoup(url, userAgent, timeoutMs);
            zszlScriptMod.LOGGER.info("[Sharechain] route=jsoup-note-content url={} foundNoteContent=true length={}",
                    url, markdown.length());
            return markdown;
        } catch (Throwable t) {
            jsoupError = t;
            zszlScriptMod.LOGGER.warn("[Sharechain] route=jsoup-note-content url={} failed={}: {}",
                    url, t.getClass().getSimpleName(), safeMessage(t));
        }

        try {
            PageData page = fetch(url, userAgent, timeoutMs);
            String markdown = page.markdownLikeText == null ? "" : page.markdownLikeText.trim();
            if (!markdown.isEmpty()) {
                zszlScriptMod.LOGGER.info("[Sharechain] route=syncData url={} foundSyncData=true length={}",
                        url, markdown.length());
                return markdown;
            }
            throw new IOException("sharechain markdown content is empty");
        } catch (Throwable t) {
            zszlScriptMod.LOGGER.warn("[Sharechain] route=syncData url={} failed={}: {}",
                    url, t.getClass().getSimpleName(), safeMessage(t));
            IOException e = asIoException("sharechain markdown fetch failed", t);
            if (jsoupError != null) {
                e.addSuppressed(jsoupError);
            }
            throw e;
        }
    }

    public static String fetchBestFirstUrl(String url, String userAgent, int timeoutMs) throws IOException {
        Throwable jsoupError = null;
        try {
            String bodyText = fetchBodyTextViaJsoup(url, userAgent, timeoutMs);
            String extracted = findFirstUrl(bodyText);
            if (extracted != null && !extracted.trim().isEmpty()) {
                zszlScriptMod.LOGGER.info("[Sharechain] route=jsoup-body-text url={} extractedUrl={}", url, extracted);
                return extracted;
            }
            throw new IOException("no valid URL in jsoup body text");
        } catch (Throwable t) {
            jsoupError = t;
            zszlScriptMod.LOGGER.warn("[Sharechain] route=jsoup-body-text url={} failed={}: {}",
                    url, t.getClass().getSimpleName(), safeMessage(t));
        }

        try {
            PageData page = fetch(url, userAgent, timeoutMs);
            String extracted = findFirstUrl(page);
            if (extracted != null && !extracted.trim().isEmpty()) {
                zszlScriptMod.LOGGER.info("[Sharechain] route=syncData-url url={} extractedUrl={}", url, extracted);
                return extracted;
            }
            throw new IOException("no valid URL in sharechain data");
        } catch (Throwable t) {
            zszlScriptMod.LOGGER.warn("[Sharechain] route=syncData-url url={} failed={}: {}",
                    url, t.getClass().getSimpleName(), safeMessage(t));
            IOException e = asIoException("sharechain url fetch failed", t);
            if (jsoupError != null) {
                e.addSuppressed(jsoupError);
            }
            throw e;
        }
    }

    public static PageData parse(String url, String rawHtml) throws IOException {
        String syncDataJson = extractSyncDataJson(rawHtml);
        JsonElement rootElement = JsonParser.parseString(syncDataJson);
        if (!rootElement.isJsonObject()) {
            throw new IOException("sharechain syncData is not a JSON object");
        }

        JsonObject root = rootElement.getAsJsonObject();
        String htmlContent = getNestedString(root, "shareData", "html_content");
        String brief = getNestedString(root, "shareData", "collection", "summary", "rich_media_summary", "brief");
        String markdownLikeText = toMarkdownLikeText(!htmlContent.isEmpty() ? htmlContent : brief);

        Set<String> urls = new LinkedHashSet<>();
        urls.addAll(extractUrls(htmlContent));
        urls.addAll(extractUrls(brief));
        urls.addAll(extractUrls(markdownLikeText));
        urls.addAll(extractUrls(rawHtml));

        return new PageData(url, rawHtml, root, htmlContent, brief, markdownLikeText, new ArrayList<>(urls));
    }

    private static String extractSyncDataJson(String rawHtml) throws IOException {
        if (rawHtml == null || rawHtml.trim().isEmpty()) {
            throw new IOException("sharechain page is empty");
        }

        int markerIndex = rawHtml.indexOf("window.syncData");
        if (markerIndex < 0) {
            throw new IOException("window.syncData not found");
        }

        int firstBrace = rawHtml.indexOf('{', markerIndex);
        if (firstBrace < 0) {
            throw new IOException("window.syncData JSON start not found");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = firstBrace; i < rawHtml.length(); i++) {
            char c = rawHtml.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
                continue;
            }
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return rawHtml.substring(firstBrace, i + 1);
                }
            }
        }

        throw new IOException("window.syncData JSON end not found");
    }

    private static String getNestedString(JsonObject root, String... path) {
        JsonElement current = root;
        for (String key : path) {
            if (current == null || !current.isJsonObject()) {
                return "";
            }
            JsonObject object = current.getAsJsonObject();
            if (!object.has(key)) {
                return "";
            }
            current = object.get(key);
        }

        if (current == null || current.isJsonNull()) {
            return "";
        }
        try {
            return current.getAsString();
        } catch (Exception ignored) {
            return current.toString();
        }
    }

    public static String toMarkdownLikeText(String htmlOrText) {
        if (htmlOrText == null || htmlOrText.trim().isEmpty()) {
            return "";
        }

        String text = htmlOrText.replace("\r", "");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</p\\s*>", "\n");
        text = text.replaceAll("(?i)<p[^>]*>", "");
        text = text.replaceAll("(?i)</div\\s*>", "\n");
        text = text.replaceAll("(?i)<div[^>]*>", "");
        text = text.replaceAll("(?i)</h([1-6])\\s*>", "\n");
        text = text.replaceAll("(?i)<h1[^>]*>", "# ");
        text = text.replaceAll("(?i)<h2[^>]*>", "## ");
        text = text.replaceAll("(?i)<h3[^>]*>", "### ");
        text = text.replaceAll("(?i)<h4[^>]*>", "#### ");
        text = text.replaceAll("(?i)<h5[^>]*>", "##### ");
        text = text.replaceAll("(?i)<h6[^>]*>", "###### ");
        text = text.replaceAll("(?i)</li\\s*>", "\n");
        text = text.replaceAll("(?i)<li[^>]*>", "- ");
        text = text.replaceAll("(?i)<[^>]+>", "");
        text = decodeHtmlEntities(text);
        text = text.replace('\u00A0', ' ');
        text = text.replace('\u200B', ' ');

        StringBuilder normalized = new StringBuilder(text.length());
        boolean previousBlank = false;
        for (String rawLine : text.split("\\n", -1)) {
            String line = rtrim(rawLine);
            boolean blank = line.trim().isEmpty();
            if (blank) {
                if (!previousBlank && normalized.length() > 0) {
                    normalized.append('\n');
                }
                previousBlank = true;
                continue;
            }
            if (normalized.length() > 0 && normalized.charAt(normalized.length() - 1) != '\n') {
                normalized.append('\n');
            }
            normalized.append(line.trim());
            previousBlank = false;
        }

        return normalized.toString().trim();
    }

    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return urls;
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group(1);
            if (url != null && !url.trim().isEmpty()) {
                urls.add(url.trim());
            }
        }
        return urls;
    }

    public static String findFirstUrl(PageData pageData) {
        if (pageData == null || pageData.extractedUrls.isEmpty()) {
            return null;
        }
        return pageData.extractedUrls.get(0);
    }

    public static String findFirstUrl(String text) {
        List<String> urls = extractUrls(text);
        if (urls.isEmpty()) {
            return null;
        }
        return urls.get(0);
    }

    private static IOException asIoException(String message, Throwable throwable) {
        if (throwable instanceof IOException ioException) {
            return ioException;
        }
        return new IOException(message + ": " + safeMessage(throwable), throwable);
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return throwable.getClass().getName();
        }
        return message.trim();
    }

    private static String decodeHtmlEntities(String text) {
        StringBuilder output = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '&') {
                output.append(c);
                continue;
            }

            int end = text.indexOf(';', i + 1);
            if (end < 0 || end - i > 12) {
                output.append(c);
                continue;
            }

            String entity = text.substring(i + 1, end);
            String decoded = decodeEntity(entity);
            if (decoded == null) {
                output.append(c);
                continue;
            }

            output.append(decoded);
            i = end;
        }
        return output.toString();
    }

    private static String decodeEntity(String entity) {
        if (entity == null || entity.isEmpty()) {
            return null;
        }

        String normalized = entity.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "nbsp":
                return " ";
            case "amp":
                return "&";
            case "lt":
                return "<";
            case "gt":
                return ">";
            case "quot":
                return "\"";
            case "apos":
            case "#39":
                return "'";
            default:
                break;
        }

        try {
            if (normalized.startsWith("#x")) {
                int codePoint = Integer.parseInt(normalized.substring(2), 16);
                return new String(Character.toChars(codePoint));
            }
            if (normalized.startsWith("#")) {
                int codePoint = Integer.parseInt(normalized.substring(1));
                return new String(Character.toChars(codePoint));
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String rtrim(String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    public static final class PageData {
        public final String url;
        public final String rawHtml;
        public final JsonObject syncData;
        public final String htmlContent;
        public final String brief;
        public final String markdownLikeText;
        public final List<String> extractedUrls;

        private PageData(String url, String rawHtml, JsonObject syncData, String htmlContent, String brief,
                String markdownLikeText, List<String> extractedUrls) {
            this.url = url;
            this.rawHtml = rawHtml;
            this.syncData = syncData;
            this.htmlContent = htmlContent;
            this.brief = brief;
            this.markdownLikeText = markdownLikeText;
            this.extractedUrls = extractedUrls;
        }
    }
}
