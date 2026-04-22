package com.zszl.zszlScriptMod.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.packet.PacketFilterConfig;
import com.zszl.zszlScriptMod.gui.packet.PacketInterceptConfig;
import com.zszl.zszlScriptMod.gui.theme.ThemeConfigManager;
import com.zszl.zszlScriptMod.utils.PacketFieldRuleManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class ProfileShareCodeManager {

    public static final String SHARE_CODE_PREFIX = "ZS2.";
    public static final String LEGACY_SHARE_CODE_PREFIX = "ZSP1.";
    private static final int BINARY_FORMAT_VERSION = 2;
    private static final int MAX_IMPORT_BYTES = 4 * 1024 * 1024;
    private static final Gson COMPACT_GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> EXCLUDED_DIRECTORY_NAMES = new LinkedHashSet<>(Arrays.asList(
            "theme_image_cache",
            "terrain",
            "cloud_cache",
            "debuff_reports",
            "execution_logs"));
    private static final Map<String, ProfileFileSpec> PROFILE_FILE_SPECS = buildProfileFileSpecs();
    private static final List<String> KNOWN_FILE_PATHS = Collections.unmodifiableList(Arrays.asList(
            "arena_config.json",
            "auto_equip_sets_v5.json",
            "auto_pickup_rules.json",
            "auto_use_item_rules.json",
            "autofollow_rules.json",
            "baritone_settings.json",
            "block_replacement_rules.json",
            "chat_optimization.json",
            "conditional_rules.json",
            "custom_paths.json",
            "debug_keybinds.json",
            "dungeon_warehouse_config.json",
            "filter_config.json",
            "gui_themes.json",
            "keybind_actions.json",
            "keybinds_v2.json",
            "keycommand_autoeat.json",            "keycommand_leaveconfig.json",
            "kill_timer_config.json",
            "loop_execution.json",
            "mail_settings.json",
            "packet_filter_config.json",
            "packet_intercept_rules.json",
            "packet_field_rules.json",
            "path_categories.json",
            "scanner_settings.json",
            "server_feature_visibility.json",
            "warehouses.json"));
    private static final Map<String, Integer> KNOWN_FILE_INDEX = buildKnownFileIndex();

    private ProfileShareCodeManager() {
    }

    public enum ImportStrategy {
        REPLACE("整体替换"),
        MERGE("智能合并");

        private final String displayName;

        ImportStrategy(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static List<String> listShareableFiles(String profileName) {
        Path profileDir = ProfileManager.getProfileDir(profileName);
        if (profileDir == null || !Files.exists(profileDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.walk(profileDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> isShareablePath(profileDir, path))
                    .map(path -> normalizeRelativePath(profileDir.relativize(path).toString()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            zszlScriptMod.LOGGER.error("Failed to list shareable files for profile '{}'", profileName, e);
            return Collections.emptyList();
        }
    }

    public static String getDisplayNameForPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }

        String normalized = normalizeRelativePath(path);
        String lower = normalized.toLowerCase(Locale.ROOT);
        ProfileFileSpec spec = PROFILE_FILE_SPECS.get(lower);
        if (spec != null && spec.displayName != null && !spec.displayName.trim().isEmpty()) {
            return spec.displayName;
        }

        if ("shadowbaritone/setting.json".equals(lower)
                || "shadowbaritone/settings.json".equals(lower)
                || "shadowbatitone/setting.json".equals(lower)
                || "shadowbatitone/settings.json".equals(lower)) {
            return "ShadowBaritone 设置";
        }
        if (lower.startsWith("shadowbaritone/")) {
            return "ShadowBaritone / " + normalized.substring("shadowbaritone/".length());
        }
        if (lower.startsWith("shadowbatitone/")) {
            return "ShadowBaritone / " + normalized.substring("shadowbatitone/".length());
        }
        if (lower.startsWith("execution_logs/")) {
            return "执行日志 / " + normalized.substring("execution_logs/".length());
        }
        return normalized;
    }

    public static String loadProfileFileContent(String profileName, String relativePath) throws IOException {
        Path file = resolveProfileFile(profileName, relativePath);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return "";
        }
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    public static void saveProfileFileContent(String profileName, String relativePath, String content) throws IOException {
        Path file = resolveProfileFile(profileName, relativePath);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, (content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
        reloadRuntimeConfigIfNeeded(profileName, Collections.singletonList(normalizeRelativePath(relativePath)));
    }

    public static String generateShareCode(String profileName, Collection<String> selectedFiles) throws IOException {
        LinkedHashSet<String> normalizedFiles = new LinkedHashSet<>();
        if (selectedFiles != null) {
            for (String selectedFile : selectedFiles) {
                if (selectedFile == null || selectedFile.trim().isEmpty()) {
                    continue;
                }
                normalizedFiles.add(normalizeRelativePath(selectedFile));
            }
        }

        if (normalizedFiles.isEmpty()) {
            throw new IllegalArgumentException("未选择任何可导出的配置文件");
        }

        List<String> orderedFiles = normalizedFiles.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(BINARY_FORMAT_VERSION);
        writeVarInt(payload, orderedFiles.size());

        for (String relativePath : orderedFiles) {
            int knownFileId = getKnownFileId(relativePath);
            writeVarInt(payload, knownFileId);
            if (knownFileId == 0) {
                writeUtf8(payload, relativePath);
            }

            String content = loadProfileFileContent(profileName, relativePath);
            byte[] contentBytes = normalizeContentForExport(relativePath, content).getBytes(StandardCharsets.UTF_8);
            writeVarInt(payload, contentBytes.length);
            payload.write(contentBytes);
        }

        byte[] compressed = deflate(payload.toByteArray());
        return SHARE_CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed);
    }

    public static ImportPreview previewImport(String shareCode, String targetProfileName) throws IOException {
        DecodedPayload payload = decodeShareCodePayload(shareCode);
        List<ImportPreviewEntry> entries = new ArrayList<>();

        for (ImportedFileEntry entry : payload.files) {
            entries.add(buildPreviewEntry(targetProfileName, entry.relativePath, entry.content));
        }

        return new ImportPreview(targetProfileName, entries);
    }

    public static ImportResult importShareCode(String shareCode, String targetProfileName) throws IOException {
        ImportPreview preview = previewImport(shareCode, targetProfileName);
        return applyImportPreview(preview, null);
    }

    public static ImportResult applyImportPreview(ImportPreview preview, Collection<String> selectedFiles) throws IOException {
        if (preview == null) {
            throw new IllegalArgumentException("导入预览不能为空");
        }

        Set<String> selected = null;
        if (selectedFiles != null) {
            selected = new LinkedHashSet<>();
            for (String selectedFile : selectedFiles) {
                if (selectedFile == null || selectedFile.trim().isEmpty()) {
                    continue;
                }
                selected.add(normalizeRelativePath(selectedFile));
            }
        }

        List<String> writtenFiles = new ArrayList<>();
        int replacedFileCount = 0;
        int mergedFileCount = 0;
        int unchangedFileCount = 0;
        int consideredCount = 0;
        String importedCustomPathsFinalContent = null;

        for (ImportPreviewEntry entry : preview.entries) {
            String normalizedEntryPath = normalizeRelativePath(entry.relativePath);
            if (selected != null && !selected.contains(normalizedEntryPath)) {
                continue;
            }

            consideredCount++;
            if (!entry.hasChanges) {
                unchangedFileCount++;
                continue;
            }

            writeImportedFile(preview.targetProfileName, normalizedEntryPath, entry.finalContent);
            writtenFiles.add(normalizedEntryPath);

            if ("custom_paths.json".equalsIgnoreCase(normalizedEntryPath)) {
                importedCustomPathsFinalContent = entry.finalContent;
            }

            if (entry.strategy == ImportStrategy.MERGE) {
                mergedFileCount++;
            } else {
                replacedFileCount++;
            }
        }

        if (consideredCount <= 0) {
            throw new IllegalArgumentException("未选择任何需要导入的配置文件");
        }

        if (importedCustomPathsFinalContent != null) {
            if (ensurePathCategoriesForImportedPaths(preview.targetProfileName, importedCustomPathsFinalContent,
                    writtenFiles)) {
                mergedFileCount++;
            }
        }

        reloadRuntimeConfigIfNeeded(preview.targetProfileName, writtenFiles);

        return new ImportResult(writtenFiles, replacedFileCount, mergedFileCount, unchangedFileCount);
    }

    private static ImportPreviewEntry buildPreviewEntry(String targetProfileName, String relativePath, String importedContent)
            throws IOException {
        ExistingFileState existing = loadExistingFileState(targetProfileName, relativePath);
        String normalizedPath = normalizeRelativePath(relativePath);
        String lowerName = normalizedPath.toLowerCase(Locale.ROOT);

        if ("gui_themes.json".equals(lowerName)) {
            return analyzeThemeImport(normalizedPath, existing, importedContent);
        }
        if ("custom_paths.json".equals(lowerName)) {
            return analyzeCustomPathsImport(normalizedPath, existing, importedContent);
        }
        if ("path_categories.json".equals(lowerName)) {
            return analyzePathCategoriesImport(normalizedPath, existing, importedContent);
        }

        return analyzeReplaceImport(normalizedPath, existing, importedContent, null);
    }

    private static ImportPreviewEntry analyzeReplaceImport(String relativePath, ExistingFileState existing,
            String importedContent, String reason) {
        String normalizedImportedContent = restoreContentForImport(relativePath, importedContent);
        String finalContent = normalizedImportedContent;
        boolean hasChanges = !contentEquals(existing.content, finalContent) || !existing.exists;

        List<String> details = new ArrayList<>();
        details.add("策略: 整体替换");
        details.add(existing.exists ? "目标文件: 已存在，将覆盖原内容" : "目标文件: 不存在，将新建该文件");
        if (reason != null && !reason.trim().isEmpty()) {
            details.add(reason);
        }

        String summary = existing.exists ? "替换当前文件内容" : "写入新的配置文件";
        return new ImportPreviewEntry(relativePath, ImportStrategy.REPLACE, summary, details,
                existing.content, normalizedImportedContent, finalContent, hasChanges);
    }

    private static ImportPreviewEntry analyzeThemeImport(String relativePath, ExistingFileState existing,
            String importedContent) {
        JsonObject importedRoot = tryParseJsonObject(importedContent);
        if (importedRoot == null || !importedRoot.has("profiles") || !importedRoot.get("profiles").isJsonArray()) {
            return analyzeReplaceImport(relativePath, existing, importedContent, "导入主题格式异常，已回退为整体替换");
        }

        JsonObject baseRoot = tryParseJsonObject(existing.content);
        if (existing.exists && baseRoot == null) {
            return analyzeReplaceImport(relativePath, existing, importedContent, "本地主题文件格式异常，无法安全合并，已改为整体替换");
        }
        if (baseRoot == null) {
            baseRoot = new JsonObject();
        }

        JsonArray currentProfiles = deepCopyArray(getJsonArray(baseRoot, "profiles"));
        JsonArray importedProfiles = getJsonArray(importedRoot, "profiles");

        Map<String, Integer> existingNameIndex = new HashMap<>();
        for (int i = 0; i < currentProfiles.size(); i++) {
            JsonObject profile = currentProfiles.get(i).isJsonObject() ? currentProfiles.get(i).getAsJsonObject() : null;
            String key = normalizeNameKey(getString(profile, "name"));
            if (!key.isEmpty() && !existingNameIndex.containsKey(key)) {
                existingNameIndex.put(key, i);
            }
        }

        int importedCount = 0;
        int addedCount = 0;
        int skippedCount = 0;

        for (JsonElement element : importedProfiles) {
            if (!element.isJsonObject()) {
                continue;
            }
            importedCount++;
            JsonObject profile = element.getAsJsonObject();
            String key = normalizeNameKey(getString(profile, "name"));
            if (!key.isEmpty() && existingNameIndex.containsKey(key)) {
                skippedCount++;
                continue;
            }
            currentProfiles.add(cloneJsonObject(profile));
            if (!key.isEmpty()) {
                existingNameIndex.put(key, currentProfiles.size() - 1);
            }
            addedCount++;
        }

        baseRoot.add("profiles", currentProfiles);
        if (!baseRoot.has("activeIndex") || !baseRoot.get("activeIndex").isJsonPrimitive()) {
            baseRoot.addProperty("activeIndex", 0);
        }

        String finalContent = PRETTY_GSON.toJson(baseRoot);
        String normalizedImportedContent = restoreContentForImport(relativePath, importedContent);
        boolean hasChanges = !contentEquals(existing.content, finalContent) || !existing.exists;

        List<String> details = new ArrayList<>();
        details.add("策略: 智能合并（同名主题跳过，不覆盖当前已有主题）");
        details.add("导入主题数: " + importedCount);
        details.add("新增主题数: " + addedCount);
        details.add("同名跳过数: " + skippedCount);
        details.add("当前激活主题索引将保持本地设置");

        String summary = addedCount > 0
                ? "合并主题配置，新增 " + addedCount + " 个主题"
                : "主题配置无新增内容";
        return new ImportPreviewEntry(relativePath, ImportStrategy.MERGE, summary, details,
                existing.content, normalizedImportedContent, finalContent, hasChanges);
    }

    private static ImportPreviewEntry analyzeCustomPathsImport(String relativePath, ExistingFileState existing,
            String importedContent) {
        JsonObject importedRoot = tryParseJsonObject(importedContent);
        if (importedRoot == null || !importedRoot.has("sequences") || !importedRoot.get("sequences").isJsonArray()) {
            return analyzeReplaceImport(relativePath, existing, importedContent, "导入路径格式异常，已回退为整体替换");
        }

        JsonObject baseRoot = tryParseJsonObject(existing.content);
        if (existing.exists && baseRoot == null) {
            return analyzeReplaceImport(relativePath, existing, importedContent, "本地路径文件格式异常，无法安全合并，已改为整体替换");
        }
        if (baseRoot == null) {
            baseRoot = new JsonObject();
        }

        JsonArray currentSequences = deepCopyArray(getJsonArray(baseRoot, "sequences"));
        JsonArray importedSequences = getJsonArray(importedRoot, "sequences");

        Map<String, Integer> existingSequenceIndex = new HashMap<>();
        for (int i = 0; i < currentSequences.size(); i++) {
            JsonObject sequence = currentSequences.get(i).isJsonObject() ? currentSequences.get(i).getAsJsonObject() : null;
            String key = normalizeNameKey(getString(sequence, "name"));
            if (!key.isEmpty() && !existingSequenceIndex.containsKey(key)) {
                existingSequenceIndex.put(key, i);
            }
        }

        int importedCount = 0;
        int addedCount = 0;
        int replacedCount = 0;

        for (JsonElement element : importedSequences) {
            if (!element.isJsonObject()) {
                continue;
            }
            importedCount++;
            JsonObject sequence = element.getAsJsonObject();
            String key = normalizeNameKey(getString(sequence, "name"));
            if (!key.isEmpty() && existingSequenceIndex.containsKey(key)) {
                int index = existingSequenceIndex.get(key);
                currentSequences.set(index, cloneJsonObject(sequence));
                replacedCount++;
            } else {
                currentSequences.add(cloneJsonObject(sequence));
                if (!key.isEmpty()) {
                    existingSequenceIndex.put(key, currentSequences.size() - 1);
                }
                addedCount++;
            }
        }

        baseRoot.add("sequences", currentSequences);

        String finalContent = PRETTY_GSON.toJson(baseRoot);
        String normalizedImportedContent = restoreContentForImport(relativePath, importedContent);
        boolean hasChanges = !contentEquals(existing.content, finalContent) || !existing.exists;

        List<String> details = new ArrayList<>();
        details.add("策略: 智能合并（同名路径替换，不同名路径追加）");
        details.add("导入路径数: " + importedCount);
        details.add("新增路径数: " + addedCount);
        details.add("同名替换数: " + replacedCount);

        String summary;
        if (addedCount > 0 || replacedCount > 0) {
            summary = "合并自定义路径：新增 " + addedCount + "，替换 " + replacedCount;
        } else {
            summary = "自定义路径无可更新内容";
        }
        return new ImportPreviewEntry(relativePath, ImportStrategy.MERGE, summary, details,
                existing.content, normalizedImportedContent, finalContent, hasChanges);
    }

    private static ImportPreviewEntry analyzePathCategoriesImport(String relativePath, ExistingFileState existing,
            String importedContent) {
        CategoriesState importedState = parseCategoriesState(importedContent);
        if (importedState == null) {
            return analyzeReplaceImport(relativePath, existing, importedContent, "导入分类格式异常，已回退为整体替换");
        }

        CategoriesState baseState = parseCategoriesState(existing.content);
        if (existing.exists && baseState == null) {
            return analyzeReplaceImport(relativePath, existing, importedContent, "本地分类文件格式异常，无法安全合并，已改为整体替换");
        }
        if (baseState == null) {
            baseState = new CategoriesState();
        }

        int addedCategories = mergeStringList(baseState.categories, importedState.categories);
        int addedHiddenCategories = mergeStringList(baseState.hiddenCategories, importedState.hiddenCategories);
        int skippedCategories = Math.max(0, importedState.categories.size() - addedCategories);
        int skippedHiddenCategories = Math.max(0, importedState.hiddenCategories.size() - addedHiddenCategories);

        JsonObject resultRoot = new JsonObject();
        resultRoot.add("categories", COMPACT_GSON.toJsonTree(baseState.categories));
        resultRoot.add("hiddenCategories", COMPACT_GSON.toJsonTree(baseState.hiddenCategories));

        String finalContent = PRETTY_GSON.toJson(resultRoot);
        String normalizedImportedContent = restoreContentForImport(relativePath, importedContent);
        boolean hasChanges = !contentEquals(existing.content, finalContent) || !existing.exists;

        List<String> details = new ArrayList<>();
        details.add("策略: 智能合并（已存在分类跳过，缺失分类追加）");
        details.add("导入分类数: " + importedState.categories.size());
        details.add("新增分类数: " + addedCategories);
        details.add("已存在跳过数: " + skippedCategories);
        details.add("新增隐藏分类数: " + addedHiddenCategories);
        details.add("已存在隐藏分类数: " + skippedHiddenCategories);

        String summary = addedCategories > 0 || addedHiddenCategories > 0
                ? "合并路径分类：新增分类 " + addedCategories + "，新增隐藏项 " + addedHiddenCategories
                : "路径分类无新增内容";
        return new ImportPreviewEntry(relativePath, ImportStrategy.MERGE, summary, details,
                existing.content, normalizedImportedContent, finalContent, hasChanges);
    }

    private static int mergeStringList(List<String> target, List<String> source) {
        if (target == null || source == null) {
            return 0;
        }
        Set<String> existing = new LinkedHashSet<>();
        for (String value : target) {
            String key = normalizeNameKey(value);
            if (!key.isEmpty()) {
                existing.add(key);
            }
        }

        int added = 0;
        for (String value : source) {
            String trimmed = value == null ? "" : value.trim();
            String key = normalizeNameKey(trimmed);
            if (key.isEmpty() || existing.contains(key)) {
                continue;
            }
            target.add(trimmed);
            existing.add(key);
            added++;
        }
        return added;
    }

    private static boolean ensurePathCategoriesForImportedPaths(String profileName, String customPathsContent,
            List<String> writtenFiles) throws IOException {
        List<String> referencedCategories = extractCategoriesFromCustomPaths(customPathsContent);
        if (referencedCategories.isEmpty()) {
            return false;
        }

        ExistingFileState existingCategoriesFile = loadExistingFileState(profileName, "path_categories.json");
        CategoriesState categoriesState = parseCategoriesState(existingCategoriesFile.content);
        if (categoriesState == null) {
            categoriesState = new CategoriesState();
        }

        int addedCategories = mergeStringList(categoriesState.categories, referencedCategories);
        if (addedCategories <= 0) {
            return false;
        }

        JsonObject resultRoot = new JsonObject();
        resultRoot.add("categories", COMPACT_GSON.toJsonTree(categoriesState.categories));
        resultRoot.add("hiddenCategories", COMPACT_GSON.toJsonTree(categoriesState.hiddenCategories));

        writeImportedFile(profileName, "path_categories.json", PRETTY_GSON.toJson(resultRoot));

        String normalizedPath = normalizeRelativePath("path_categories.json");
        if (writtenFiles != null && !writtenFiles.contains(normalizedPath)) {
            writtenFiles.add(normalizedPath);
        }
        return true;
    }

    private static List<String> extractCategoriesFromCustomPaths(String customPathsContent) {
        JsonObject root = tryParseJsonObject(customPathsContent);
        if (root == null || !root.has("sequences") || !root.get("sequences").isJsonArray()) {
            return Collections.emptyList();
        }

        List<String> categories = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("sequences")) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            JsonObject sequence = element.getAsJsonObject();
            String category = getString(sequence, "category");
            String trimmed = category == null ? "" : category.trim();
            if (!trimmed.isEmpty()) {
                categories.add(trimmed);
            }
        }
        return categories;
    }

    private static CategoriesState parseCategoriesState(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new CategoriesState();
        }

        try {
            JsonElement root = new JsonParser().parse(content);
            CategoriesState state = new CategoriesState();

            if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                if (object.has("categories") && object.get("categories").isJsonArray()) {
                    state.categories.addAll(readStringArray(object.getAsJsonArray("categories")));
                }
                if (object.has("hiddenCategories") && object.get("hiddenCategories").isJsonArray()) {
                    state.hiddenCategories.addAll(readStringArray(object.getAsJsonArray("hiddenCategories")));
                }
                return state;
            }

            if (root.isJsonArray()) {
                state.categories.addAll(readStringArray(root.getAsJsonArray()));
                return state;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> readStringArray(JsonArray array) {
        List<String> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (JsonElement element : array) {
            if (element == null || !element.isJsonPrimitive()) {
                continue;
            }
            String value = element.getAsString();
            String trimmed = value == null ? "" : value.trim();
            String key = normalizeNameKey(trimmed);
            if (key.isEmpty() || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            result.add(trimmed);
        }
        return result;
    }

    private static JsonObject tryParseJsonObject(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        try {
            JsonElement element = new JsonParser().parse(content);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static JsonArray getJsonArray(JsonObject root, String key) {
        if (root != null && root.has(key) && root.get(key).isJsonArray()) {
            return root.getAsJsonArray(key);
        }
        return new JsonArray();
    }

    private static JsonArray deepCopyArray(JsonArray source) {
        JsonArray copy = new JsonArray();
        if (source == null) {
            return copy;
        }
        for (JsonElement element : source) {
            copy.add(element == null ? null : cloneJsonElement(element));
        }
        return copy;
    }

    private static JsonObject cloneJsonObject(JsonObject source) {
        if (source == null) {
            return new JsonObject();
        }
        return new JsonParser().parse(COMPACT_GSON.toJson(source)).getAsJsonObject();
    }

    private static JsonElement cloneJsonElement(JsonElement source) {
        if (source == null) {
            return null;
        }
        return new JsonParser().parse(COMPACT_GSON.toJson(source));
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static ExistingFileState loadExistingFileState(String profileName, String relativePath) throws IOException {
        Path file = resolveProfileFile(profileName, relativePath);
        boolean exists = Files.exists(file) && Files.isRegularFile(file);
        if (!exists) {
            return new ExistingFileState(false, "");
        }
        return new ExistingFileState(true, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    private static String normalizeContentForExport(String relativePath, String content) {
        if (content == null) {
            return "";
        }
        String lowerName = relativePath == null ? "" : relativePath.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".json")) {
            try {
                JsonElement parsed = new JsonParser().parse(content);
                return COMPACT_GSON.toJson(parsed);
            } catch (Exception ignored) {
            }
        }
        return content;
    }

    private static String restoreContentForImport(String relativePath, String content) {
        if (content == null) {
            return "";
        }
        String lowerName = relativePath == null ? "" : relativePath.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".json")) {
            try {
                JsonElement parsed = new JsonParser().parse(content);
                return PRETTY_GSON.toJson(parsed);
            } catch (Exception ignored) {
            }
        }
        return content;
    }

    private static void writeImportedFile(String profileName, String relativePath, String content) throws IOException {
        Path file = resolveProfileFile(profileName, relativePath);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, (content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
    }

    private static void reloadRuntimeConfigIfNeeded(String profileName, Collection<String> writtenFiles) {
        if (!ProfileManager.getActiveProfileName().equals(profileName)) {
            return;
        }

        ModConfig.loadAllConfigs();

        boolean reloadThemes = false;
        boolean reloadPacketFilter = false;
        boolean reloadPacketIntercept = false;
        boolean reloadPacketFieldRules = false;

        if (writtenFiles != null) {
            for (String file : writtenFiles) {
                String normalized = normalizeRelativePath(file);
                if ("gui_themes.json".equalsIgnoreCase(normalized)) {
                    reloadThemes = true;
                } else if ("packet_filter_config.json".equalsIgnoreCase(normalized)) {
                    reloadPacketFilter = true;
                } else if ("packet_intercept_rules.json".equalsIgnoreCase(normalized)) {
                    reloadPacketIntercept = true;
                } else if ("packet_field_rules.json".equalsIgnoreCase(normalized)) {
                    reloadPacketFieldRules = true;
                }
            }
        }

        if (reloadThemes) {
            try {
                ThemeConfigManager.load();
            } catch (Exception e) {
                zszlScriptMod.LOGGER.warn("Failed to reload theme config after import", e);
            }
        }
        if (reloadPacketFilter) {
            try {
                PacketFilterConfig.load();
            } catch (Exception e) {
                zszlScriptMod.LOGGER.warn("Failed to reload packet filter config after import", e);
            }
        }
        if (reloadPacketIntercept) {
            try {
                PacketInterceptConfig.load();
            } catch (Exception e) {
                zszlScriptMod.LOGGER.warn("Failed to reload packet intercept config after import", e);
            }
        }
        if (reloadPacketFieldRules) {
            try {
                PacketFieldRuleManager.reloadRules();
            } catch (Exception e) {
                zszlScriptMod.LOGGER.warn("Failed to reload packet field rules after import", e);
            }
        }
    }

    private static DecodedPayload decodeShareCodePayload(String shareCode) throws IOException {
        if (shareCode == null || shareCode.trim().isEmpty()) {
            throw new IllegalArgumentException("分享码不能为空");
        }

        String normalized = stripKnownPrefix(shareCode.replaceAll("\\s+", ""));
        byte[] encodedBytes = decodeBase64Lenient(normalized);
        byte[] decodedBytes = tryDecompressLenient(encodedBytes);

        if (decodedBytes.length == 0) {
            throw new IllegalArgumentException("分享码内容为空");
        }

        if (looksLikeLegacyJson(decodedBytes)) {
            return decodeLegacyJsonPayload(decodedBytes);
        }

        if ((decodedBytes[0] & 0xFF) == BINARY_FORMAT_VERSION) {
            return decodeBinaryPayload(decodedBytes);
        }

        try {
            return decodeLegacyJsonPayload(decodedBytes);
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("无法识别分享码版本");
    }

    private static DecodedPayload decodeLegacyJsonPayload(byte[] jsonBytes) {
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        JsonElement root = new JsonParser().parse(json);
        if (root == null || !root.isJsonObject()) {
            throw new IllegalArgumentException("分享码解码后不是有效对象");
        }

        JsonObject payload = root.getAsJsonObject();
        if (!payload.has("files") || !payload.get("files").isJsonObject()) {
            throw new IllegalArgumentException("分享码内容无效：缺少 files 节点");
        }

        JsonObject filesObject = payload.getAsJsonObject("files");
        List<ImportedFileEntry> files = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : filesObject.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isJsonNull()) {
                continue;
            }
            String relativePath = normalizeRelativePath(entry.getKey());
            String content = entry.getValue().isJsonPrimitive()
                    ? entry.getValue().getAsString()
                    : COMPACT_GSON.toJson(entry.getValue());
            files.add(new ImportedFileEntry(relativePath, content));
        }

        return new DecodedPayload(files);
    }

    private static DecodedPayload decodeBinaryPayload(byte[] bytes) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        int version = input.read();
        if (version != BINARY_FORMAT_VERSION) {
            throw new IllegalArgumentException("不支持的分享码版本: " + version);
        }

        int fileCount = readVarInt(input);
        if (fileCount < 0 || fileCount > 4096) {
            throw new IllegalArgumentException("分享码文件数量异常: " + fileCount);
        }

        List<ImportedFileEntry> files = new ArrayList<>(fileCount);
        Set<String> seenPaths = new LinkedHashSet<>();

        for (int i = 0; i < fileCount; i++) {
            int knownFileId = readVarInt(input);
            String relativePath;
            if (knownFileId == 0) {
                relativePath = normalizeRelativePath(readUtf8(input));
            } else {
                if (knownFileId < 1 || knownFileId > KNOWN_FILE_PATHS.size()) {
                    throw new IllegalArgumentException("分享码包含未知文件索引: " + knownFileId);
                }
                relativePath = KNOWN_FILE_PATHS.get(knownFileId - 1);
            }

            int contentLength = readVarInt(input);
            if (contentLength < 0 || contentLength > MAX_IMPORT_BYTES) {
                throw new IllegalArgumentException("分享码文件内容长度异常: " + contentLength);
            }

            byte[] contentBytes = readExactBytes(input, contentLength);
            String content = new String(contentBytes, StandardCharsets.UTF_8);

            if (!seenPaths.add(relativePath)) {
                throw new IllegalArgumentException("分享码中存在重复配置文件: " + relativePath);
            }

            files.add(new ImportedFileEntry(relativePath, content));
        }

        if (input.available() > 0) {
            throw new IllegalArgumentException("分享码包含无法识别的尾部数据");
        }

        return new DecodedPayload(files);
    }

    private static String stripKnownPrefix(String value) {
        String normalized = value == null ? "" : value.trim();

        if (normalized.startsWith(SHARE_CODE_PREFIX)) {
            return normalized.substring(SHARE_CODE_PREFIX.length());
        }
        if (normalized.startsWith("ZS2")) {
            normalized = normalized.substring(3);
            if (normalized.startsWith(".") || normalized.startsWith(":") || normalized.startsWith("-")) {
                normalized = normalized.substring(1);
            }
            return normalized;
        }
        if (normalized.startsWith("ZSP2")) {
            normalized = normalized.substring(4);
            if (normalized.startsWith(".") || normalized.startsWith(":") || normalized.startsWith("-")) {
                normalized = normalized.substring(1);
            }
            return normalized;
        }
        if (normalized.startsWith(LEGACY_SHARE_CODE_PREFIX)) {
            return normalized.substring(LEGACY_SHARE_CODE_PREFIX.length());
        }
        if (normalized.startsWith("ZSP1")) {
            normalized = normalized.substring(4);
            if (normalized.startsWith(".") || normalized.startsWith(":") || normalized.startsWith("-")) {
                normalized = normalized.substring(1);
            }
            return normalized;
        }

        return normalized;
    }

    private static boolean looksLikeLegacyJson(byte[] bytes) {
        for (byte current : bytes) {
            int c = current & 0xFF;
            if (!Character.isWhitespace((char) c)) {
                return c == '{' || c == '[';
            }
        }
        return false;
    }

    private static byte[] decodeBase64Lenient(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("分享码不是有效的 Base64 数据", e);
        }
    }

    private static byte[] tryDecompressLenient(byte[] encodedBytes) throws IOException {
        IOException last = null;
        try {
            return inflateBytes(encodedBytes, true);
        } catch (IOException e) {
            last = e;
        }
        try {
            return inflateBytes(encodedBytes, false);
        } catch (IOException e) {
            last = e;
        }
        try {
            return gunzipBytes(encodedBytes);
        } catch (IOException e) {
            last = e;
        }
        throw last == null ? new IOException("无法解压分享码数据") : last;
    }

    private static byte[] deflate(byte[] rawBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        try (DeflaterOutputStream stream = new DeflaterOutputStream(output, deflater)) {
            stream.write(rawBytes);
        } finally {
            deflater.end();
        }
        return output.toByteArray();
    }

    private static byte[] inflateBytes(byte[] compressed, boolean nowrap) throws IOException {
        Inflater inflater = new Inflater(nowrap);
        try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(compressed), inflater)) {
            return readAllBytesLimited(input);
        } finally {
            inflater.end();
        }
    }

    private static byte[] gunzipBytes(byte[] compressed) throws IOException {
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return readAllBytesLimited(input);
        }
    }

    private static byte[] readAllBytesLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_IMPORT_BYTES) {
                throw new IOException("分享码解压后内容过大");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static Path resolveProfileFile(String profileName, String relativePath) {
        Path profileDir = ProfileManager.getProfileDir(profileName).toAbsolutePath().normalize();
        String normalizedRelative = normalizeRelativePath(relativePath);
        Path resolved = profileDir.resolve(normalizedRelative).normalize();
        if (!resolved.startsWith(profileDir)) {
            throw new IllegalArgumentException("非法配置路径: " + relativePath);
        }
        return resolved;
    }

    private static boolean isShareablePath(Path profileDir, Path file) {
        Path relative = profileDir.relativize(file);
        for (Path part : relative) {
            String name = part.toString().toLowerCase(Locale.ROOT);
            if (EXCLUDED_DIRECTORY_NAMES.contains(name)) {
                return false;
            }
        }
        return isShareableRelativePath(normalizeRelativePath(relative.toString()));
    }

    private static String normalizeRelativePath(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("非法相对路径: " + path);
        }
        return normalized;
    }

    private static String normalizeNameKey(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized;
    }

    private static boolean contentEquals(String first, String second) {
        return normalizeForCompare(first).equals(normalizeForCompare(second));
    }

    private static String normalizeForCompare(String content) {
        return content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static int getKnownFileId(String relativePath) {
        Integer index = KNOWN_FILE_INDEX.get(normalizeRelativePath(relativePath).toLowerCase(Locale.ROOT));
        return index == null ? 0 : index;
    }

    private static boolean isShareableRelativePath(String relativePath) {
        String lower = normalizeRelativePath(relativePath).toLowerCase(Locale.ROOT);
        ProfileFileSpec spec = PROFILE_FILE_SPECS.get(lower);
        return spec != null && spec.shareable;
    }

    private static Map<String, ProfileFileSpec> buildProfileFileSpecs() {
        LinkedHashMap<String, ProfileFileSpec> specs = new LinkedHashMap<>();

        registerHiddenFile(specs, "arena_config.json", "竞技场设置");
        registerShareableFile(specs, "auto_equip_sets_v5.json", "自动穿戴配置集");
        registerShareableFile(specs, "auto_escape_rules.json", "自动逃跑规则");
        registerShareableFile(specs, "auto_pickup_rules.json", "自动拾取规则");
        registerShareableFile(specs, "auto_use_item_rules.json", "静默使用物品规则");
        registerShareableFile(specs, "autofollow_rules.json", "自动追怪规则");
        registerHiddenFile(specs, "baritone_command_table_state.json", "Baritone 指令表状态");
        registerHiddenFile(specs, "baritone_parkour_runtime.json", "Baritone 跑酷运行时状态");
        registerHiddenFile(specs, "baritone_settings.json", "旧版 Baritone 设置");
        registerShareableFile(specs, "block_replacement_rules.json", "区域方块替换规则");
        registerShareableFile(specs, "chat_optimization.json", "聊天框优化设置");
        registerShareableFile(specs, "conditional_rules.json", "条件执行规则");
        registerShareableFile(specs, "custom_paths.json", "自定义路径数据");
        registerShareableFile(specs, "debug_keybinds.json", "调试快捷键");
        registerHiddenFile(specs, "dungeon_warehouse_config.json", "副本仓库设置");
        registerShareableFile(specs, "filter_config.json", "旧版过滤配置");
        registerHiddenFile(specs, "gui_action_editor_preferences.json", "动作编辑器界面偏好");
        registerHiddenFile(specs, "gui_action_variable_manager_preferences.json", "动作全局变量界面偏好");
        registerHiddenFile(specs, "gui_path_manager_layout.json", "路径序列编辑器布局");
        registerHiddenFile(specs, "gui_profile_manager_layout.json", "配置管理界面布局");
        registerShareableFile(specs, "gui_themes.json", "主题配置");
        registerShareableFile(specs, "human_like_movement.json", "拟人移动设置");
        registerShareableFile(specs, "human_like_route_templates.json", "拟人路线模板");
        registerHiddenFile(specs, "human_route_templates.json", "拟人路线模板（旧版）");
        registerHiddenFile(specs, "inventory_ui_layout.json", "主界面布局");
        registerShareableFile(specs, "keybind_actions.json", "快捷键动作配置");
        registerShareableFile(specs, "keybinds_v2.json", "快捷键绑定");
        registerShareableFile(specs, "keycommand_auto_fishing.json", "自动钓鱼设置");
        registerShareableFile(specs, "keycommand_autoeat.json", "自动进食设置");
        registerShareableFile(specs, "keycommand_fly.json", "飞行设置");
        registerShareableFile(specs, "keycommand_killaura.json", "杀戮光环设置");
        registerShareableFile(specs, "keycommand_leaveconfig.json", "撤离/重进设置");
        registerShareableFile(specs, "keycommand_speed.json", "加速设置");
        registerShareableFile(specs, "kill_timer_config.json", "杀怪数据面板设置");
        registerShareableFile(specs, "legacy_action_templates.json", "动作模板");
        registerShareableFile(specs, "legacy_sequence_trigger_rules.json", "路径序列触发器规则");
        registerShareableFile(specs, "loop_execution.json", "循环执行设置");
        registerShareableFile(specs, "mail_settings.json", "邮件助手设置");
        registerHiddenFile(specs, "node_audit_log.json", "节点审计日志");
        registerHiddenFile(specs, "node_editor_drafts.json", "节点编辑器草稿");
        registerHiddenFile(specs, "node_editor_hotkeys.json", "节点编辑器快捷键");
        registerHiddenFile(specs, "node_library_panel_state.json", "节点库面板状态");
        registerHiddenFile(specs, "node_sequences.json", "节点序列");
        registerHiddenFile(specs, "node_templates.json", "节点模板库");
        registerShareableFile(specs, "other_features_block.json", "其他功能-方块");
        registerShareableFile(specs, "other_features_item.json", "其他功能-物品");
        registerShareableFile(specs, "other_features_misc.json", "其他功能-杂项");
        registerShareableFile(specs, "other_features_movement.json", "其他功能-移动");
        registerShareableFile(specs, "other_features_render.json", "其他功能-渲染");
        registerShareableFile(specs, "other_features_speed.json", "其他功能-加速");
        registerShareableFile(specs, "other_features_world.json", "其他功能-世界");
        registerShareableFile(specs, "packet_field_rules.json", "数据包字段规则");
        registerShareableFile(specs, "packet_filter_config.json", "数据包过滤设置");
        registerShareableFile(specs, "packet_intercept_rules.json", "数据包拦截规则");
        registerHiddenFile(specs, "password_manager.json", "密码管理数据");
        registerShareableFile(specs, "path_categories.json", "路径分类");
        registerShareableFile(specs, "path_safety_config.json", "路径安全设置");
        registerShareableFile(specs, "performance_monitor.json", "性能监控");
        registerShareableFile(specs, "scanner_settings.json", "地形扫描设置");
        registerShareableFile(specs, "server_feature_visibility.json", "服务器功能隐藏");
        registerHiddenFile(specs, "shadowbaritone/setting.json", "ShadowBaritone 设置");
        registerHiddenFile(specs, "shadowbaritone/settings.json", "ShadowBaritone 设置");
        registerHiddenFile(specs, "shadowbatitone/setting.json", "ShadowBaritone 设置");
        registerHiddenFile(specs, "shadowbatitone/settings.json", "ShadowBaritone 设置");
        registerShareableFile(specs, "warehouses.json", "智能仓库数据");

        return Collections.unmodifiableMap(specs);
    }

    private static void registerShareableFile(Map<String, ProfileFileSpec> specs, String relativePath,
            String displayName) {
        registerFile(specs, relativePath, displayName, true);
    }

    private static void registerHiddenFile(Map<String, ProfileFileSpec> specs, String relativePath,
            String displayName) {
        registerFile(specs, relativePath, displayName, false);
    }

    private static void registerFile(Map<String, ProfileFileSpec> specs, String relativePath,
            String displayName, boolean shareable) {
        String normalizedPath = normalizeRelativePath(relativePath);
        specs.put(normalizedPath.toLowerCase(Locale.ROOT),
                new ProfileFileSpec(normalizedPath, displayName, shareable));
    }

    private static Map<String, Integer> buildKnownFileIndex() {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < KNOWN_FILE_PATHS.size(); i++) {
            index.put(KNOWN_FILE_PATHS.get(i).toLowerCase(Locale.ROOT), i + 1);
        }
        return Collections.unmodifiableMap(index);
    }

    private static void writeUtf8(ByteArrayOutputStream output, String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        writeVarInt(output, bytes.length);
        output.write(bytes, 0, bytes.length);
    }

    private static String readUtf8(InputStream input) throws IOException {
        int length = readVarInt(input);
        if (length < 0 || length > MAX_IMPORT_BYTES) {
            throw new IllegalArgumentException("字符串长度异常: " + length);
        }
        return new String(readExactBytes(input, length), StandardCharsets.UTF_8);
    }

    private static void writeVarInt(ByteArrayOutputStream output, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("VarInt 不支持负数: " + value);
        }
        while ((value & ~0x7F) != 0) {
            output.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.write(value);
    }

    private static int readVarInt(InputStream input) throws IOException {
        int value = 0;
        int position = 0;
        int currentByte;
        do {
            currentByte = input.read();
            if (currentByte == -1) {
                throw new IOException("读取分享码时遇到意外结尾");
            }

            value |= (currentByte & 0x7F) << position;
            position += 7;

            if (position > 35) {
                throw new IOException("VarInt 过长");
            }
        } while ((currentByte & 0x80) != 0);

        return value;
    }

    private static byte[] readExactBytes(InputStream input, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(bytes, offset, length - offset);
            if (read == -1) {
                throw new IOException("读取分享码内容时遇到意外结尾");
            }
            offset += read;
        }
        return bytes;
    }

    private static final class CategoriesState {
        private final List<String> categories = new ArrayList<>();
        private final List<String> hiddenCategories = new ArrayList<>();
    }

    private static final class ExistingFileState {
        private final boolean exists;
        private final String content;

        private ExistingFileState(boolean exists, String content) {
            this.exists = exists;
            this.content = content == null ? "" : content;
        }
    }

    private static final class ProfileFileSpec {
        private final String relativePath;
        private final String displayName;
        private final boolean shareable;

        private ProfileFileSpec(String relativePath, String displayName, boolean shareable) {
            this.relativePath = relativePath == null ? "" : relativePath;
            this.displayName = displayName == null ? "" : displayName;
            this.shareable = shareable;
        }
    }

    private static final class DecodedPayload {
        private final List<ImportedFileEntry> files;

        private DecodedPayload(List<ImportedFileEntry> files) {
            this.files = files == null ? Collections.<ImportedFileEntry>emptyList() : files;
        }
    }

    private static final class ImportedFileEntry {
        private final String relativePath;
        private final String content;

        private ImportedFileEntry(String relativePath, String content) {
            this.relativePath = relativePath;
            this.content = content == null ? "" : content;
        }
    }

    public static final class ImportPreview {
        private final String targetProfileName;
        private final List<ImportPreviewEntry> entries;

        private ImportPreview(String targetProfileName, List<ImportPreviewEntry> entries) {
            this.targetProfileName = targetProfileName == null ? "" : targetProfileName;
            this.entries = entries == null
                    ? Collections.<ImportPreviewEntry>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(entries));
        }

        public String getTargetProfileName() {
            return targetProfileName;
        }

        public List<ImportPreviewEntry> getEntries() {
            return entries;
        }

        public int getTotalCount() {
            return entries.size();
        }

        public int getChangedCount() {
            int count = 0;
            for (ImportPreviewEntry entry : entries) {
                if (entry.hasChanges) {
                    count++;
                }
            }
            return count;
        }
    }

    public static final class ImportPreviewEntry {
        private final String relativePath;
        private final ImportStrategy strategy;
        private final String summary;
        private final List<String> detailLines;
        private final String existingContent;
        private final String importedContent;
        private final String finalContent;
        private final boolean hasChanges;

        private ImportPreviewEntry(String relativePath, ImportStrategy strategy, String summary,
                List<String> detailLines, String existingContent, String importedContent,
                String finalContent, boolean hasChanges) {
            this.relativePath = relativePath == null ? "" : relativePath;
            this.strategy = strategy == null ? ImportStrategy.REPLACE : strategy;
            this.summary = summary == null ? "" : summary;
            this.detailLines = detailLines == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(detailLines));
            this.existingContent = existingContent == null ? "" : existingContent;
            this.importedContent = importedContent == null ? "" : importedContent;
            this.finalContent = finalContent == null ? "" : finalContent;
            this.hasChanges = hasChanges;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public ImportStrategy getStrategy() {
            return strategy;
        }

        public String getSummary() {
            return summary;
        }

        public List<String> getDetailLines() {
            return detailLines;
        }

        public String getExistingContent() {
            return existingContent;
        }

        public String getImportedContent() {
            return importedContent;
        }

        public String getFinalContent() {
            return finalContent;
        }

        public boolean hasChanges() {
            return hasChanges;
        }
    }

    public static final class ImportResult {
        private final List<String> importedFiles;
        private final int replacedFileCount;
        private final int mergedFileCount;
        private final int unchangedFileCount;

        private ImportResult(List<String> importedFiles, int replacedFileCount, int mergedFileCount,
                int unchangedFileCount) {
            this.importedFiles = importedFiles == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(importedFiles));
            this.replacedFileCount = Math.max(0, replacedFileCount);
            this.mergedFileCount = Math.max(0, mergedFileCount);
            this.unchangedFileCount = Math.max(0, unchangedFileCount);
        }

        public List<String> getImportedFiles() {
            return importedFiles;
        }

        public int getImportedCount() {
            return importedFiles.size();
        }

        public int getReplacedFileCount() {
            return replacedFileCount;
        }

        public int getMergedFileCount() {
            return mergedFileCount;
        }

        public int getUnchangedFileCount() {
            return unchangedFileCount;
        }
    }
}

