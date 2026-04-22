package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItemFilterHandler {

    public static final class MoveChestFilterRule {
        private final String itemName;
        private final List<String> requiredNbtTags;

        private MoveChestFilterRule(String itemName, List<String> requiredNbtTags) {
            this.itemName = itemName == null ? "" : itemName.trim();
            this.requiredNbtTags = requiredNbtTags == null ? new ArrayList<String>() : new ArrayList<String>(requiredNbtTags);
        }

        public String getItemName() {
            return itemName;
        }

        public List<String> getRequiredNbtTags() {
            return new ArrayList<String>(requiredNbtTags);
        }

        private boolean isEmpty() {
            return itemName.isEmpty() && requiredNbtTags.isEmpty();
        }
    }

    public static final File FILTER_CONFIG_FILE = new File(ModConfig.CONFIG_DIR, "filter_config.json");
    public static final String NBT_TAG_MATCH_MODE_CONTAINS = "CONTAINS";
    public static final String NBT_TAG_MATCH_MODE_NOT_CONTAINS = "NOT_CONTAINS";
    public static final String MOVE_DIRECTION_INVENTORY_TO_CHEST = "INVENTORY_TO_CHEST";
    public static final String MOVE_DIRECTION_CHEST_TO_INVENTORY = "CHEST_TO_INVENTORY";

    public static List<String> blacklistFilters = new ArrayList<>();
    public static List<String> whitelistFilters = new ArrayList<>();

    private static volatile boolean warehouseTransferInProgress = false;

    private ItemFilterHandler() {
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("filter_config.json").toFile();
    }

    public static void saveFilterConfig() {
        try {
            File configFile = getConfigFile();
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JsonObject json = new JsonObject();
            json.add("blacklist", new Gson().toJsonTree(blacklistFilters));
            json.add("whitelist", new Gson().toJsonTree(whitelistFilters));
            Files.write(configFile.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存过滤配置失败", e);
        }
    }

    public static void loadFilterConfig() {
        blacklistFilters = new ArrayList<>();
        whitelistFilters = new ArrayList<>();
        try {
            File configFile = getConfigFile();
            if (!configFile.exists()) {
                return;
            }
            JsonObject json = JsonParser.parseString(Files.readString(configFile.toPath(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (json.has("blacklist") && json.get("blacklist").isJsonArray()) {
                for (var element : json.getAsJsonArray("blacklist")) {
                    blacklistFilters.add(element.getAsString());
                }
            }
            if (json.has("whitelist") && json.get("whitelist").isJsonArray()) {
                for (var element : json.getAsJsonArray("whitelist")) {
                    whitelistFilters.add(element.getAsString());
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载过滤配置失败", e);
        }
    }

    public static void dropItemsByNameFilter() {
    }

    public static void transferItemsToWarehouse() {
        WarehouseEventHandler.startAutoDepositByHighlights();
        warehouseTransferInProgress = WarehouseEventHandler.isAutoDepositRouteRunning();
    }

    public static boolean isWarehouseTransferInProgress() {
        return warehouseTransferInProgress || WarehouseEventHandler.isAutoDepositRouteRunning();
    }

    public static void moveInventoryItemsToChestSlots(JsonObject params) {
        warehouseTransferInProgress = false;
    }

    public static String buildItemSearchableText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String name = stack.getHoverName().getString();
        CompoundTag tag = stack.getTag();
        return (name == null ? "" : name) + " " + (tag == null ? "" : tag.toString());
    }

    public static boolean matchesRequiredNbtTags(ItemStack stack, List<String> requiredNbtTags, String matchMode) {
        if (requiredNbtTags == null || requiredNbtTags.isEmpty()) {
            return true;
        }
        String searchable = buildItemSearchableText(stack).toLowerCase(Locale.ROOT);
        boolean containsMode = !NBT_TAG_MATCH_MODE_NOT_CONTAINS.equalsIgnoreCase(matchMode);
        for (String tag : requiredNbtTags) {
            String normalized = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            boolean contains = searchable.contains(normalized);
            if (containsMode && !contains) {
                return false;
            }
            if (!containsMode && contains) {
                return false;
            }
        }
        return true;
    }

    public static List<String> readTagFilters(JsonObject params, String arrayKey, String textKey) {
        List<String> tags = new ArrayList<>();
        if (params == null) {
            return tags;
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonArray()) {
            JsonArray array = params.getAsJsonArray(arrayKey);
            array.forEach(element -> {
                String text = element.getAsString().trim();
                if (!text.isEmpty()) {
                    tags.add(text);
                }
            });
            return tags;
        }
        if (params.has(textKey)) {
            String raw = params.get(textKey).getAsString();
            for (String part : raw.split("[,\\n]")) {
                String text = part.trim();
                if (!text.isEmpty()) {
                    tags.add(text);
                }
            }
        }
        return tags;
    }

    public static String readRequiredNbtTagMatchMode(JsonObject params) {
        if (params == null || !params.has("requiredNbtTagMatchMode")) {
            return NBT_TAG_MATCH_MODE_CONTAINS;
        }
        return params.get("requiredNbtTagMatchMode").getAsString();
    }

    public static String readMoveChestDirection(JsonObject params) {
        if (params == null || !params.has("moveDirection")) {
            return MOVE_DIRECTION_INVENTORY_TO_CHEST;
        }
        return params.get("moveDirection").getAsString();
    }

    public static String readMoveChestItemName(JsonObject params) {
        if (params == null || !params.has("itemName")) {
            return "";
        }
        try {
            String value = params.get("itemName").getAsString();
            return value == null ? "" : value.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static List<MoveChestFilterRule> readMoveChestFilterRules(JsonObject params) {
        List<MoveChestFilterRule> rules = new ArrayList<>();
        if (params == null) {
            return rules;
        }

        if (params.has("moveChestRules") && params.get("moveChestRules").isJsonArray()) {
            JsonArray array = params.getAsJsonArray("moveChestRules");
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject ruleObject = element.getAsJsonObject();
                String itemName = "";
                if (ruleObject.has("itemName") && ruleObject.get("itemName").isJsonPrimitive()) {
                    itemName = ruleObject.get("itemName").getAsString();
                }
                List<String> requiredNbtTags = readTagFilters(ruleObject, "requiredNbtTags", "requiredNbtTagsText");
                MoveChestFilterRule rule = new MoveChestFilterRule(itemName, requiredNbtTags);
                if (!rule.isEmpty()) {
                    rules.add(rule);
                }
            }
            if (!rules.isEmpty()) {
                return rules;
            }
        }

        String itemName = readMoveChestItemName(params);
        List<String> requiredNbtTags = readTagFilters(params, "requiredNbtTags", "requiredNbtTagsText");
        MoveChestFilterRule legacyRule = new MoveChestFilterRule(itemName, requiredNbtTags);
        if (!legacyRule.isEmpty()) {
            rules.add(legacyRule);
        }
        return rules;
    }
}
