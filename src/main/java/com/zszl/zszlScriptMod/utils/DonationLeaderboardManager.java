package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.zszlScriptMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DonationLeaderboardManager {

    public static final class Entry {
        public final int rank;
        public final String name;
        public final String amount;

        public Entry(int rank, String name, String amount) {
            this.rank = rank;
            this.name = name;
            this.amount = amount;
        }
    }

    public static final String PAYMENT_QR_RESOURCE = "img/Sponsored.jpg";

    private static final String LEADERBOARD_URL_KEY = "donation_leaderboard.url";
    private static final String MOBILE_USER_AGENT = SharechainPageParser.MOBILE_USER_AGENT;
    private static final String CACHE_FILE = "donation_leaderboard.md";

    public static volatile List<Entry> leaderboard = new CopyOnWriteArrayList<>();
    public static volatile String rawMarkdown = "榜单加载中...";

    private static volatile boolean fetchInProgress = false;
    private static volatile boolean hasFetched = false;
    private static volatile long lastFetchTime = 0L;

    private DonationLeaderboardManager() {
    }

    public static void fetchContent() {
        if (hasFetched) {
            return;
        }

        String cached = CloudContentCache.readText(CACHE_FILE);
        if (!cached.isEmpty()) {
            rawMarkdown = cached;
            leaderboard = parseLeaderboardFromMarkdown(cached);
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
        if (fetchInProgress) {
            return;
        }
        fetchInProgress = true;
        lastFetchTime = System.currentTimeMillis();

        Thread thread = new Thread(() -> {
            try {
                String latest = SharechainPageParser.fetchBestMarkdown(
                        SharechainLinkConfig.getRequiredUrl(LEADERBOARD_URL_KEY),
                        MOBILE_USER_AGENT,
                        15000).trim();
                if (!latest.isEmpty()) {
                    rawMarkdown = latest;
                    leaderboard = parseLeaderboardFromMarkdown(latest);
                    CloudContentCache.writeText(CACHE_FILE, latest);
                    zszlScriptMod.LOGGER.info("[Donation] 榜单刷新完成，解析到 {} 行文本与 {} 条记录 cache={}",
                            latest.split("\\r?\\n").length, leaderboard.size(),
                            CloudContentCache.getCachePath(CACHE_FILE).toAbsolutePath());
                } else {
                    throw new Exception("sharechain markdown content is empty");
                }
            } catch (Throwable t) {
                if (leaderboard.isEmpty() && (rawMarkdown == null || rawMarkdown.trim().isEmpty())) {
                    rawMarkdown = "榜单加载失败：" + safeMessage(t);
                }
                zszlScriptMod.LOGGER.warn("[Donation] 榜单刷新失败", t);
            } finally {
                fetchInProgress = false;
            }
        }, "DonationLeaderboard-Refresh");
        thread.setDaemon(true);
        thread.start();
    }

    private static List<Entry> parseLeaderboardFromMarkdown(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return new CopyOnWriteArrayList<>();
        }

        List<Entry> parsed = new ArrayList<>();
        String[] lines = markdown.split("\\r?\\n");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            Entry tableEntry = tryParseMarkdownTableLine(line);
            if (tableEntry != null) {
                parsed.add(tableEntry);
                continue;
            }

            Entry looseEntry = tryParseLooseLine(line);
            if (looseEntry != null) {
                parsed.add(looseEntry);
            }
        }

        Collections.sort(parsed, Comparator.comparingInt(o -> o.rank));
        return new CopyOnWriteArrayList<>(parsed);
    }

    private static Entry tryParseMarkdownTableLine(String line) {
        if (!line.contains("|")) {
            return null;
        }

        String[] cells = line.split("\\|");
        List<String> trimmed = new ArrayList<>();
        for (String cell : cells) {
            String value = cell.trim();
            if (!value.isEmpty()) {
                trimmed.add(value);
            }
        }

        if (trimmed.size() < 3) {
            return null;
        }

        String rankCell = trimmed.get(0);
        String nameCell = trimmed.get(1);
        String amountCell = trimmed.get(2);
        if (isHeaderOrSeparator(rankCell, nameCell, amountCell)) {
            return null;
        }

        int rank = parseRank(rankCell);
        if (rank <= 0 || nameCell.isEmpty() || amountCell.isEmpty()) {
            return null;
        }
        return new Entry(rank, nameCell, normalizeAmount(amountCell));
    }

    private static Entry tryParseLooseLine(String line) {
        Pattern pattern = Pattern.compile("^(\\d{1,3})[、.．)]\\s*([^\\s]+)\\s+(.+)$");
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        int rank;
        try {
            rank = Integer.parseInt(matcher.group(1));
        } catch (Exception e) {
            return null;
        }
        if (rank <= 0) {
            return null;
        }

        String name = matcher.group(2).trim();
        String amount = normalizeAmount(matcher.group(3).trim());
        if (name.isEmpty() || amount.isEmpty()) {
            return null;
        }

        return new Entry(rank, name, amount);
    }

    private static boolean isHeaderOrSeparator(String rankCell, String nameCell, String amountCell) {
        String rank = rankCell.replace(" ", "");
        String name = nameCell.replace(" ", "");
        String amount = amountCell.replace(" ", "");

        if (rank.matches("[-:]+") || name.matches("[-:]+") || amount.matches("[-:]+")) {
            return true;
        }

        return containsAny(rank, "排名", "名次", "rank")
                || containsAny(name, "昵称", "名字", "玩家", "name")
                || containsAny(amount, "金额", "打赏", "amount");
    }

    private static boolean containsAny(String text, String... keys) {
        String lower = text.toLowerCase();
        for (String key : keys) {
            if (lower.contains(key.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static int parseRank(String rankCell) {
        Matcher matcher = Pattern.compile("(\\d{1,3})").matcher(rankCell);
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception e) {
            return -1;
        }
    }

    private static String normalizeAmount(String amount) {
        String normalized = amount.trim();
        if (normalized.endsWith("元") || normalized.endsWith("¥")) {
            return normalized;
        }
        return normalized + " 元";
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()) {
            return throwable == null ? "" : throwable.getClass().getName();
        }
        return throwable.getMessage().trim();
    }
}
