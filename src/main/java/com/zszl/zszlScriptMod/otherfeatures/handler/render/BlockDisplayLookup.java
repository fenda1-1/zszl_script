package com.zszl.zszlScriptMod.otherfeatures.handler.render;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Locale;

public final class BlockDisplayLookup {

    private BlockDisplayLookup() {
    }

    public static Block findBlockByUserInput(String raw) {
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
        ResourceLocation id = ResourceLocation.tryParse(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
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
}
