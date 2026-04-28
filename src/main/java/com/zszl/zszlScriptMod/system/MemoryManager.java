package com.zszl.zszlScriptMod.system;

import com.zszl.zszlScriptMod.shadowbaritone.utils.accessor.IChunkArray;
import com.zszl.zszlScriptMod.shadowbaritone.utils.accessor.IClientChunkProvider;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.storage.TagValueOutput;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class MemoryManager {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final String RENDER_SECTION_CLASS_NAME =
            "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection";

    public static final Map<String, MemorySnapshot> snapshots = new LinkedHashMap<>();

    private MemoryManager() {
    }

    public static void takeSnapshot(String name) {
        if (MC.level == null) {
            return;
        }

        System.gc();

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        Map<String, Integer> entityCounts = new HashMap<>();
        Map<String, Integer> tileEntityCounts = new HashMap<>();
        Map<String, Integer> chunkCounts = new HashMap<>();
        Map<String, Integer> renderChunkCounts = new HashMap<>();

        Map<String, Long> entityMemoryUsage = new HashMap<>();
        Map<String, Long> tileEntityMemoryUsage = new HashMap<>();
        Map<String, Long> chunkMemoryUsage = new HashMap<>();
        Map<String, Long> renderChunkMemoryUsage = new HashMap<>();

        snapshotEntities(MC.level, entityCounts, entityMemoryUsage);
        snapshotChunks(MC.level, tileEntityCounts, tileEntityMemoryUsage,
                chunkCounts, chunkMemoryUsage, renderChunkCounts, renderChunkMemoryUsage);

        snapshots.put(name, new MemorySnapshot(name, totalMemory, freeMemory,
                entityCounts, tileEntityCounts, chunkCounts, renderChunkCounts,
                entityMemoryUsage, tileEntityMemoryUsage, chunkMemoryUsage, renderChunkMemoryUsage));
    }

    public static void deleteSnapshot(String name) {
        snapshots.remove(name);
    }

    public static void clearSnapshots() {
        snapshots.clear();
    }

    public static ComparisonResult compare(MemorySnapshot before, MemorySnapshot after) {
        return new ComparisonResult(before, after);
    }

    private static void snapshotEntities(ClientLevel level, Map<String, Integer> entityCounts,
            Map<String, Long> entityMemoryUsage) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == null) {
                continue;
            }

            String className = entity.getClass().getName();
            entityCounts.put(className, entityCounts.getOrDefault(className, 0) + 1);
            try {
                TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
                entity.saveWithoutId(output);
                CompoundTag tag = output.buildResult();
                entityMemoryUsage.put(className,
                        entityMemoryUsage.getOrDefault(className, 0L) + estimateCompressedSize(tag));
            } catch (Exception e) {
                zszlScriptMod.LOGGER.debug("Failed to estimate entity memory for {}", className, e);
            }
        }
    }

    private static void snapshotChunks(ClientLevel level,
            Map<String, Integer> tileEntityCounts,
            Map<String, Long> tileEntityMemoryUsage,
            Map<String, Integer> chunkCounts,
            Map<String, Long> chunkMemoryUsage,
            Map<String, Integer> renderChunkCounts,
            Map<String, Long> renderChunkMemoryUsage) {
        Collection<LevelChunk> loadedChunks = getLoadedChunks(level);
        if (loadedChunks.isEmpty()) {
            return;
        }

        String chunkClassName = LevelChunk.class.getName();
        Set<BlockEntity> seenBlockEntities = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        int totalRenderSections = 0;

        for (LevelChunk chunk : loadedChunks) {
            if (chunk == null) {
                continue;
            }

            chunkCounts.put(chunkClassName, chunkCounts.getOrDefault(chunkClassName, 0) + 1);

            int nonEmptySections = 0;
            LevelChunkSection[] sections = chunk.getSections();
            if (sections != null) {
                for (LevelChunkSection section : sections) {
                    if (section != null && !section.hasOnlyAir()) {
                        nonEmptySections++;
                    }
                }
            }
            totalRenderSections += nonEmptySections;
            chunkMemoryUsage.put(chunkClassName,
                    chunkMemoryUsage.getOrDefault(chunkClassName, 0L) + estimateChunkSize(chunk, nonEmptySections));

            try {
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity == null || !seenBlockEntities.add(blockEntity)) {
                        continue;
                    }

                    String className = blockEntity.getClass().getName();
                    tileEntityCounts.put(className, tileEntityCounts.getOrDefault(className, 0) + 1);
                    try {
                        CompoundTag tag = blockEntity.saveWithFullMetadata(RegistryAccess.EMPTY);
                        tileEntityMemoryUsage.put(className,
                                tileEntityMemoryUsage.getOrDefault(className, 0L) + estimateCompressedSize(tag));
                    } catch (Exception e) {
                        zszlScriptMod.LOGGER.debug("Failed to estimate block entity memory for {}", className, e);
                    }
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.debug("Failed to iterate block entities for chunk {}", chunk.getPos(), e);
            }
        }

        if (totalRenderSections > 0) {
            renderChunkCounts.put(RENDER_SECTION_CLASS_NAME, totalRenderSections);
            renderChunkMemoryUsage.put(RENDER_SECTION_CLASS_NAME, 0L);
        }
    }

    private static Collection<LevelChunk> getLoadedChunks(ClientLevel level) {
        List<LevelChunk> result = new ArrayList<>();
        try {
            ClientChunkCache chunkSource = level.getChunkSource();
            if (!(chunkSource instanceof IClientChunkProvider provider)) {
                return result;
            }

            IChunkArray chunkArray = provider.extractReferenceArray();
            if (chunkArray == null) {
                return result;
            }

            AtomicReferenceArray<LevelChunk> chunks = chunkArray.getChunks();
            if (chunks == null) {
                return result;
            }

            for (int i = 0; i < chunks.length(); i++) {
                LevelChunk chunk = chunks.get(i);
                if (chunk != null) {
                    result.add(chunk);
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to collect loaded client chunks for memory snapshot", e);
        }
        return result;
    }

    private static long estimateChunkSize(LevelChunk chunk, int nonEmptySections) {
        try {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", chunk.getPos().x);
            tag.putInt("z", chunk.getPos().z);
            tag.putInt("non_empty_sections", Math.max(0, nonEmptySections));
            tag.putInt("block_entity_count", chunk.getBlockEntities().size());
            return estimateCompressedSize(tag);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static long estimateCompressedSize(CompoundTag tag) throws Exception {
        if (tag == null) {
            return 0L;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, output);
        return output.size();
    }

    public static final class ComparisonResult {
        public final MemorySnapshot before;
        public final MemorySnapshot after;
        public final List<Map.Entry<String, Long>> topMemoryIncreases;
        public final List<Map.Entry<String, Long>> topMemoryDecreases;

        public ComparisonResult(MemorySnapshot before, MemorySnapshot after) {
            this.before = before;
            this.after = after;

            Map<String, Long> memoryDeltas = new HashMap<>();
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(before.entityMemoryUsage.keySet());
            allKeys.addAll(after.entityMemoryUsage.keySet());
            allKeys.addAll(before.tileEntityMemoryUsage.keySet());
            allKeys.addAll(after.tileEntityMemoryUsage.keySet());
            allKeys.addAll(before.chunkMemoryUsage.keySet());
            allKeys.addAll(after.chunkMemoryUsage.keySet());
            allKeys.addAll(before.renderChunkMemoryUsage.keySet());
            allKeys.addAll(after.renderChunkMemoryUsage.keySet());

            for (String key : allKeys) {
                long beforeMemory = before.entityMemoryUsage.getOrDefault(key, 0L)
                        + before.tileEntityMemoryUsage.getOrDefault(key, 0L)
                        + before.chunkMemoryUsage.getOrDefault(key, 0L)
                        + before.renderChunkMemoryUsage.getOrDefault(key, 0L);
                long afterMemory = after.entityMemoryUsage.getOrDefault(key, 0L)
                        + after.tileEntityMemoryUsage.getOrDefault(key, 0L)
                        + after.chunkMemoryUsage.getOrDefault(key, 0L)
                        + after.renderChunkMemoryUsage.getOrDefault(key, 0L);
                long delta = afterMemory - beforeMemory;
                if (delta != 0L) {
                    memoryDeltas.put(key, delta);
                }
            }

            List<Map.Entry<String, Long>> sorted = new ArrayList<>(memoryDeltas.entrySet());
            topMemoryIncreases = new ArrayList<>();
            topMemoryDecreases = new ArrayList<>();
            for (Map.Entry<String, Long> entry : sorted) {
                if (entry.getValue() > 0L) {
                    topMemoryIncreases.add(entry);
                } else if (entry.getValue() < 0L) {
                    topMemoryDecreases.add(entry);
                }
            }

            topMemoryIncreases.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            topMemoryDecreases.sort(Map.Entry.comparingByValue());
        }
    }
}
