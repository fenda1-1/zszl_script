package com.zszl.zszlScriptMod.gui.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BaritoneBlockSettingEditorSupport {

    private BaritoneBlockSettingEditorSupport() {
    }

    static String normalizeBlockId(String raw) {
        String token = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (token.isEmpty()) {
            return "";
        }
        Block block = findBlockByUserInput(token);
        return block == null ? "" : BlockUtils.blockToString(block);
    }

    static List<String> copyNormalizedBlockIds(Collection<String> blockIds) {
        LinkedHashSet<String> deduped = new LinkedHashSet<String>();
        if (blockIds != null) {
            for (String blockId : blockIds) {
                String normalized = normalizeBlockId(blockId);
                if (!normalized.isEmpty()) {
                    deduped.add(normalized);
                }
            }
        }
        return new ArrayList<String>(deduped);
    }

    static List<String> parseBlockListValue(String raw) {
        String normalized = unwrapCollection(raw);
        if (normalized.isEmpty()) {
            return new ArrayList<String>();
        }
        String[] tokens = normalized.split(",");
        List<String> parsed = new ArrayList<String>(tokens.length);
        for (String token : tokens) {
            String blockId = normalizeBlockId(token);
            if (!blockId.isEmpty()) {
                parsed.add(blockId);
            }
        }
        return copyNormalizedBlockIds(parsed);
    }

    static String serializeBlockListValue(Collection<String> blockIds) {
        return String.join(",", copyNormalizedBlockIds(blockIds));
    }

    static LinkedHashMap<String, List<String>> parseBlockMapValue(String raw) {
        LinkedHashMap<String, List<String>> mappings = new LinkedHashMap<String, List<String>>();
        String normalized = unwrapCollection(raw);
        if (normalized.isEmpty()) {
            return mappings;
        }
        String[] entries = normalized.split(",(?=[^,]*->)");
        for (String entry : entries) {
            String[] pair = entry.split("->", 2);
            if (pair.length < 2) {
                continue;
            }
            String sourceBlockId = normalizeBlockId(pair[0]);
            List<String> targetBlockIds = parseBlockListValue(pair[1]);
            if (sourceBlockId.isEmpty() || targetBlockIds.isEmpty()) {
                continue;
            }
            mappings.put(sourceBlockId, targetBlockIds);
        }
        return mappings;
    }

    static LinkedHashMap<String, List<String>> copyNormalizedBlockMap(Map<String, List<String>> mappings) {
        LinkedHashMap<String, List<String>> normalized = new LinkedHashMap<String, List<String>>();
        if (mappings != null) {
            for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
                String sourceBlockId = normalizeBlockId(entry.getKey());
                List<String> targetBlockIds = copyNormalizedBlockIds(entry.getValue());
                if (sourceBlockId.isEmpty() || targetBlockIds.isEmpty()) {
                    continue;
                }
                normalized.put(sourceBlockId, targetBlockIds);
            }
        }
        return normalized;
    }

    static String serializeBlockMapValue(Map<String, List<String>> mappings) {
        LinkedHashMap<String, List<String>> normalized = copyNormalizedBlockMap(mappings);
        List<String> entries = new ArrayList<String>(normalized.size());
        for (Map.Entry<String, List<String>> entry : normalized.entrySet()) {
            String targetValue = serializeBlockListValue(entry.getValue());
            if (!targetValue.isEmpty()) {
                entries.add(entry.getKey() + "->" + targetValue);
            }
        }
        return String.join(",", entries);
    }

    static ItemStack getBlockStack(Block block) {
        if (block == null || block == Blocks.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(block);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    static String getDisplayName(String blockId) {
        Block block = resolveBlock(blockId);
        ItemStack stack = getBlockStack(block);
        if (!stack.isEmpty()) {
            return stack.getHoverName().getString();
        }
        return blockId == null ? "" : blockId;
    }

    static Block resolveBlock(String blockId) {
        String normalized = normalizeBlockId(blockId);
        return normalized.isEmpty() ? null : findBlockByUserInput(normalized);
    }

    private static String unwrapCollection(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        if ((normalized.startsWith("[") && normalized.endsWith("]"))
                || (normalized.startsWith("{") && normalized.endsWith("}"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private static Block findBlockByUserInput(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }

        Block byId = resolveById(normalized);
        if (byId != null) {
            return byId;
        }

        String normalizedDisplay = normalizeDisplayLookup(raw);
        if (normalizedDisplay.isEmpty()) {
            return null;
        }

        Block fuzzyMatch = null;
        boolean fuzzyAmbiguous = false;
        for (Block candidate : BuiltInRegistries.BLOCK) {
            if (candidate == null || candidate == Blocks.AIR) {
                continue;
            }
            String displayName = normalizeDisplayLookup(getDisplayName(candidate));
            if (displayName.isEmpty()) {
                continue;
            }
            if (displayName.equals(normalizedDisplay)) {
                return candidate;
            }
            if (displayName.contains(normalizedDisplay) || normalizedDisplay.contains(displayName)) {
                if (fuzzyMatch == null) {
                    fuzzyMatch = candidate;
                } else if (fuzzyMatch != candidate) {
                    fuzzyAmbiguous = true;
                }
            }
        }
        return fuzzyAmbiguous ? null : fuzzyMatch;
    }

    private static Block resolveById(String normalized) {
        try {
            String candidateId = normalized.contains(":") ? normalized : "minecraft:" + normalized;
            ResourceLocation id = ResourceLocation.tryParse(candidateId);
            if (id == null) {
                return null;
            }
            return BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String getDisplayName(Block block) {
        ItemStack stack = new ItemStack(block);
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private static String normalizeDisplayLookup(String raw) {
        return (raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT))
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .replace(":", "");
    }

    private static final class BlockUtils {
        private BlockUtils() {
        }

        private static String blockToString(Block block) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) {
                return "";
            }
            return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
        }
    }
}
