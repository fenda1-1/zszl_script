package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.prefs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.system.ProfileManager;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class GuiActionEditorPreferences {
    public final int actionListPreferredWidth;
    public final boolean actionLibraryCollapsed;
    public final boolean advancedEditorMode;
    public final LinkedHashSet<String> favoriteActionTypes;
    public final List<String> recentActionTypes;
    public final boolean showSummaryInfoCard;
    public final boolean showValidationInfoCard;
    public final boolean showHelpInfoCard;
    public final int infoPopupX;
    public final int infoPopupY;
    public final int infoPopupPreferredWidth;
    public final int infoPopupPreferredHeight;
    public final String infoPanelDockMode;
    public final int infoDockPreferredWidth;
    public final int infoDockPreferredHeight;

    public GuiActionEditorPreferences(int actionListPreferredWidth, boolean actionLibraryCollapsed,
            boolean advancedEditorMode, LinkedHashSet<String> favoriteActionTypes, List<String> recentActionTypes,
            boolean showSummaryInfoCard, boolean showValidationInfoCard, boolean showHelpInfoCard,
            int infoPopupX, int infoPopupY, int infoPopupPreferredWidth, int infoPopupPreferredHeight,
            String infoPanelDockMode, int infoDockPreferredWidth, int infoDockPreferredHeight) {
        this.actionListPreferredWidth = actionListPreferredWidth;
        this.actionLibraryCollapsed = actionLibraryCollapsed;
        this.advancedEditorMode = advancedEditorMode;
        this.favoriteActionTypes = favoriteActionTypes == null ? new LinkedHashSet<String>() : favoriteActionTypes;
        this.recentActionTypes = recentActionTypes == null ? new ArrayList<String>() : recentActionTypes;
        this.showSummaryInfoCard = showSummaryInfoCard;
        this.showValidationInfoCard = showValidationInfoCard;
        this.showHelpInfoCard = showHelpInfoCard;
        this.infoPopupX = infoPopupX;
        this.infoPopupY = infoPopupY;
        this.infoPopupPreferredWidth = infoPopupPreferredWidth;
        this.infoPopupPreferredHeight = infoPopupPreferredHeight;
        this.infoPanelDockMode = infoPanelDockMode == null ? "float" : infoPanelDockMode;
        this.infoDockPreferredWidth = infoDockPreferredWidth;
        this.infoDockPreferredHeight = infoDockPreferredHeight;
    }

    public static GuiActionEditorPreferences load(String fileName, int actionListMinWidth, int defaultWidth,
            int maxRecentActions) {
        int preferredWidth = defaultWidth;
        boolean collapsed = false;
        boolean advanced = false;
        LinkedHashSet<String> favorites = new LinkedHashSet<String>();
        List<String> recent = new ArrayList<String>();
        boolean showSummary = false;
        boolean showValidation = false;
        boolean showHelp = false;
        int popupX = Integer.MIN_VALUE;
        int popupY = Integer.MIN_VALUE;
        int popupW = 280;
        int popupH = 232;
        String dockMode = "float";
        int dockW = 240;
        int dockH = 180;

        Path path = getPreferencesPath(fileName);
        if (path == null || !Files.exists(path)) {
            return new GuiActionEditorPreferences(preferredWidth, collapsed, advanced, favorites, recent,
                    showSummary, showValidation, showHelp, popupX, popupY, popupW, popupH, dockMode, dockW, dockH);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root != null) {
                if (root.has("actionListPreferredWidth")) {
                    preferredWidth = Math.max(actionListMinWidth, root.get("actionListPreferredWidth").getAsInt());
                }
                if (root.has("actionLibraryCollapsed")) {
                    collapsed = root.get("actionLibraryCollapsed").getAsBoolean();
                }
                if (root.has("advancedEditorMode")) {
                    advanced = root.get("advancedEditorMode").getAsBoolean();
                }
                if (root.has("showSummaryInfoCard")) {
                    showSummary = root.get("showSummaryInfoCard").getAsBoolean();
                }
                if (root.has("showValidationInfoCard")) {
                    showValidation = root.get("showValidationInfoCard").getAsBoolean();
                }
                if (root.has("showHelpInfoCard")) {
                    showHelp = root.get("showHelpInfoCard").getAsBoolean();
                }
                if (root.has("infoPopupX")) {
                    popupX = root.get("infoPopupX").getAsInt();
                }
                if (root.has("infoPopupY")) {
                    popupY = root.get("infoPopupY").getAsInt();
                }
                if (root.has("infoPopupPreferredWidth")) {
                    popupW = root.get("infoPopupPreferredWidth").getAsInt();
                }
                if (root.has("infoPopupPreferredHeight")) {
                    popupH = root.get("infoPopupPreferredHeight").getAsInt();
                }
                if (root.has("infoPanelDockMode")) {
                    dockMode = normalizeDockMode(root.get("infoPanelDockMode").getAsString());
                }
                if (root.has("infoDockPreferredWidth")) {
                    dockW = root.get("infoDockPreferredWidth").getAsInt();
                }
                if (root.has("infoDockPreferredHeight")) {
                    dockH = root.get("infoDockPreferredHeight").getAsInt();
                }
                if (root.has("favoriteActionTypes") && root.get("favoriteActionTypes").isJsonArray()) {
                    for (JsonElement element : root.getAsJsonArray("favoriteActionTypes")) {
                        if (element != null && element.isJsonPrimitive()) {
                            String actionType = normalizeActionType(element.getAsString());
                            if (!actionType.isEmpty()) {
                                favorites.add(actionType);
                            }
                        }
                    }
                }
                if (root.has("recentActionTypes") && root.get("recentActionTypes").isJsonArray()) {
                    for (JsonElement element : root.getAsJsonArray("recentActionTypes")) {
                        if (element != null && element.isJsonPrimitive()) {
                            String actionType = normalizeActionType(element.getAsString());
                            if (!actionType.isEmpty() && !recent.contains(actionType)) {
                                recent.add(actionType);
                                if (recent.size() >= maxRecentActions) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return new GuiActionEditorPreferences(defaultWidth, false, false,
                    new LinkedHashSet<String>(), new ArrayList<String>(),
                    false, false, false, Integer.MIN_VALUE, Integer.MIN_VALUE, 280, 232, "float", 240, 180);
        }

        return new GuiActionEditorPreferences(preferredWidth, collapsed, advanced, favorites, recent,
                showSummary, showValidation, showHelp, popupX, popupY, popupW, popupH, dockMode, dockW, dockH);
    }

    public static void save(String fileName, int actionListPreferredWidth, boolean actionLibraryCollapsed,
            boolean advancedEditorMode, Iterable<String> favoriteActionTypes, Iterable<String> recentActionTypes,
            boolean showSummaryInfoCard, boolean showValidationInfoCard, boolean showHelpInfoCard,
            int infoPopupX, int infoPopupY, int infoPopupPreferredWidth, int infoPopupPreferredHeight,
            String infoPanelDockMode, int infoDockPreferredWidth, int infoDockPreferredHeight) {
        Path path = getPreferencesPath(fileName);
        if (path == null) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            JsonObject root = new JsonObject();
            root.addProperty("actionListPreferredWidth", actionListPreferredWidth);
            root.addProperty("actionLibraryCollapsed", actionLibraryCollapsed);
            root.addProperty("advancedEditorMode", advancedEditorMode);
            root.addProperty("showSummaryInfoCard", showSummaryInfoCard);
            root.addProperty("showValidationInfoCard", showValidationInfoCard);
            root.addProperty("showHelpInfoCard", showHelpInfoCard);
            root.addProperty("infoPopupX", infoPopupX);
            root.addProperty("infoPopupY", infoPopupY);
            root.addProperty("infoPopupPreferredWidth", infoPopupPreferredWidth);
            root.addProperty("infoPopupPreferredHeight", infoPopupPreferredHeight);
            root.addProperty("infoPanelDockMode", normalizeDockMode(infoPanelDockMode));
            root.addProperty("infoDockPreferredWidth", infoDockPreferredWidth);
            root.addProperty("infoDockPreferredHeight", infoDockPreferredHeight);

            com.google.gson.JsonArray favorites = new com.google.gson.JsonArray();
            if (favoriteActionTypes != null) {
                for (String actionType : favoriteActionTypes) {
                    String normalized = normalizeActionType(actionType);
                    if (!normalized.isEmpty()) {
                        favorites.add(normalized);
                    }
                }
            }
            root.add("favoriteActionTypes", favorites);

            com.google.gson.JsonArray recent = new com.google.gson.JsonArray();
            if (recentActionTypes != null) {
                for (String actionType : recentActionTypes) {
                    String normalized = normalizeActionType(actionType);
                    if (!normalized.isEmpty()) {
                        recent.add(normalized);
                    }
                }
            }
            root.add("recentActionTypes", recent);

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
        } catch (Exception ignored) {
        }
    }

    public static void save(String fileName, int actionListPreferredWidth, boolean actionLibraryCollapsed,
            boolean advancedEditorMode, Iterable<String> favoriteActionTypes, Iterable<String> recentActionTypes) {
        save(fileName, actionListPreferredWidth, actionLibraryCollapsed, advancedEditorMode,
                favoriteActionTypes, recentActionTypes,
                false, false, false,
                Integer.MIN_VALUE, Integer.MIN_VALUE, 280, 232,
                "float", 240, 180);
    }

    private static Path getPreferencesPath(String fileName) {
        try {
            Path profileDir = ProfileManager.getCurrentProfileDir();
            return profileDir == null ? null : profileDir.resolve(fileName);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeActionType(String actionType) {
        return actionType == null ? "" : actionType.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeDockMode(String dockMode) {
        String normalized = dockMode == null ? "" : dockMode.trim().toLowerCase(Locale.ROOT);
        if ("middle".equals(normalized) || "right".equals(normalized) || "bottom".equals(normalized)) {
            return normalized;
        }
        return "float";
    }
}
