package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    public static volatile String latestVersion = "...";
    public static volatile String changelogContent = I18n.format("msg.update_checker.fetching_changelog");

    private static final String UPDATE_URL_KEY = "update_changelog.url";
    private static final String MOBILE_USER_AGENT = SharechainPageParser.MOBILE_USER_AGENT;
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\[(v[\\d.]+)\\]");
    private static final String CACHE_FILE = "update_changelog.md";
    private static final String META_CACHE_FILE = "update_checker_meta.txt";
    private static final long NOTICE_INTERVAL_MS = 60L * 60L * 1000L;

    private static volatile boolean hasFetched = false;
    private static volatile boolean fetchInProgress = false;
    private static volatile long lastFetchTime = 0L;
    private static volatile long lastCheckRunTime = 0L;
    private static volatile long lastNoticeTime = 0L;
    private static volatile boolean metaLoaded = false;

    private UpdateChecker() {
    }

    public static void fetchVersionAndChangelog() {
        ensureMetaLoaded();
        if (hasFetched) {
            return;
        }

        String cached = CloudContentCache.readText(CACHE_FILE);
        if (!cached.isEmpty()) {
            applyParsedContent(cached);
        }

        hasFetched = true;
        forceRefresh();
    }

    public static void requestRefreshIfDue(long intervalMs) {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime >= intervalMs) {
            forceRefresh();
        }
    }

    public static void forceRefresh() {
        ensureMetaLoaded();
        if (fetchInProgress) {
            return;
        }
        fetchInProgress = true;
        long now = System.currentTimeMillis();
        lastFetchTime = now;
        lastCheckRunTime = now;
        persistMeta();

        Thread thread = new Thread(() -> {
            zszlScriptMod.LOGGER.info("[UpdateChecker] Background refresh thread started");
            try {
                String updateUrl = SharechainLinkConfig.getRequiredUrl(UPDATE_URL_KEY);
                zszlScriptMod.LOGGER.info("[UpdateChecker] Connecting to URL: {}", updateUrl);

                String rawText = SharechainPageParser.fetchBestMarkdown(updateUrl, MOBILE_USER_AGENT, 15000).trim();

                if (rawText.isEmpty()) {
                    throw new Exception(I18n.format("msg.common.error.parsed_content_empty"));
                }

                zszlScriptMod.LOGGER.info("[UpdateChecker] Parsed markdown content length: {} cache={}",
                        rawText.length(), CloudContentCache.getCachePath(CACHE_FILE).toAbsolutePath());
                applyParsedContent(rawText);
                CloudContentCache.writeText(CACHE_FILE, rawText);
            } catch (Throwable t) {
                if (changelogContent == null || changelogContent.trim().isEmpty()
                        || changelogContent.equals(I18n.format("msg.update_checker.fetching_changelog"))) {
                    latestVersion = I18n.format("msg.common.fetch_failed");
                    changelogContent = I18n.format("msg.update_checker.fetch_failed",
                            t.getClass().getSimpleName(), safeMessage(t));
                }
                zszlScriptMod.LOGGER.error("Failed to fetch update information", t);
            } finally {
                fetchInProgress = false;
            }
        }, "UpdateChecker-Refresh");
        thread.setDaemon(true);
        thread.start();
    }

    public static void notifyIfNewVersion() {
        ensureMetaLoaded();
        requestRefreshIfDue(NOTICE_INTERVAL_MS);

        if (latestVersion == null
                || latestVersion.equals("...")
                || latestVersion.equals(I18n.format("msg.common.unknown"))
                || latestVersion.equals(I18n.format("msg.common.fetch_failed"))) {
            return;
        }
        if (!latestVersion.equals(zszlScriptMod.VERSION)) {
            long now = System.currentTimeMillis();
            if (lastNoticeTime > 0L && (now - lastNoticeTime) < NOTICE_INTERVAL_MS) {
                return;
            }

            lastNoticeTime = now;
            persistMeta();
            zszlScriptMod.LOGGER.info(
                    "New script version detected. Click update script to update, or click version to view changelog.");
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(new TextComponentString(
                        ChatFormatting.YELLOW + I18n.format("msg.update_checker.new_version_notice")));
            }
        }
    }

    private static void applyParsedContent(String rawText) {
        changelogContent = rawText;
        Matcher matcher = VERSION_PATTERN.matcher(rawText == null ? "" : rawText);
        if (matcher.find()) {
            latestVersion = matcher.group(1);
            zszlScriptMod.LOGGER.info("[UpdateChecker] Latest version parsed: {}", latestVersion);
        } else {
            latestVersion = I18n.format("msg.common.unknown");
            zszlScriptMod.LOGGER.warn("[UpdateChecker] Unable to parse version from content");
        }
    }

    private static void ensureMetaLoaded() {
        if (metaLoaded) {
            return;
        }
        synchronized (UpdateChecker.class) {
            if (metaLoaded) {
                return;
            }
            String meta = CloudContentCache.readText(META_CACHE_FILE);
            if (!meta.isEmpty()) {
                for (String line : meta.split("\\r?\\n")) {
                    String trimmed = line == null ? "" : line.trim();
                    if (trimmed.isEmpty() || !trimmed.contains("=")) {
                        continue;
                    }
                    String[] kv = trimmed.split("=", 2);
                    String key = kv[0].trim().toLowerCase(Locale.ROOT);
                    String value = kv[1].trim();
                    long parsed = parseLong(value);
                    if ("last_check_run_ms".equals(key)) {
                        lastCheckRunTime = Math.max(0L, parsed);
                    } else if ("last_notice_ms".equals(key)) {
                        lastNoticeTime = Math.max(0L, parsed);
                    }
                }
            }
            metaLoaded = true;
        }
    }

    private static void persistMeta() {
        String content = "last_check_run_ms=" + Math.max(0L, lastCheckRunTime) + "\n"
                + "last_notice_ms=" + Math.max(0L, lastNoticeTime);
        CloudContentCache.writeText(META_CACHE_FILE, content);
    }

    private static long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()) {
            return throwable == null ? "" : throwable.getClass().getName();
        }
        return throwable.getMessage().trim();
    }
}
