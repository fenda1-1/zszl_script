/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zszl.zszlScriptMod.shadowbaritone.api.utils;

import com.zszl.zszlScriptMod.shadowbaritone.api.utils.accessor.IItemStack;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.util.Util;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class BlockOptionalMeta {
    // id or id[] or id[properties] where id and properties are any text with at least one character
    private static final Pattern PATTERN = Pattern.compile("^(?<id>.+?)(?:\\[(?<properties>.+?)?\\])?$");

    private final Block block;
    private final String propertiesDescription; // exists so toString() can return something more useful than a list of all blockstates
    private final Set<BlockState> blockstates;
    private final ImmutableSet<Integer> stateHashes;
    private final ImmutableSet<Integer> stackHashes;
    private static ReloadableServerRegistries.Holder lootTables;
    private static Map<Block, List<Item>> drops = new HashMap<>();

    public BlockOptionalMeta(@Nonnull Block block) {
        this.block = block;
        this.propertiesDescription = "{}";
        this.blockstates = getStates(block, Collections.emptyMap());
        this.stateHashes = getStateHashes(blockstates);
        this.stackHashes = getStackHashes(blockstates);
    }

    public BlockOptionalMeta(@Nonnull String selector) {
        Matcher matcher = PATTERN.matcher(selector);

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid block selector");
        }

        block = BlockUtils.stringToBlockRequired(matcher.group("id"));

        String props = matcher.group("properties");
        Map<Property<?>, ?> properties = props == null || props.equals("") ? Collections.emptyMap() : parseProperties(block, props);

        propertiesDescription = props == null ? "{}" : "{" + props.replace("=", ":") + "}";
        blockstates = getStates(block, properties);
        stateHashes = getStateHashes(blockstates);
        stackHashes = getStackHashes(blockstates);
    }

    private static <C extends Comparable<C>, P extends Property<C>> P castToIProperty(Object value) {
        //noinspection unchecked
        return (P) value;
    }

    private static Map<Property<?>, ?> parseProperties(Block block, String raw) {
        ImmutableMap.Builder<Property<?>, Object> builder = ImmutableMap.builder();
        for (String pair : raw.split(",")) {
            String[] parts = pair.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException(String.format("\"%s\" is not a valid property-value pair", pair));
            }
            String rawKey = parts[0];
            String rawValue = parts[1];
            Property<?> key = block.getStateDefinition().getProperty(rawKey);
            Comparable<?> value = castToIProperty(key).getValue(rawValue)
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "\"%s\" is not a valid value for %s on %s",
                            rawValue, key, block
                    )));
            builder.put(key, value);
        }
        return builder.build();
    }

    private static Set<BlockState> getStates(@Nonnull Block block, @Nonnull Map<Property<?>, ?> properties) {
        return block.getStateDefinition().getPossibleStates().stream()
                .filter(blockstate -> properties.entrySet().stream().allMatch(entry ->
                        blockstate.getValue(entry.getKey()) == entry.getValue()
                ))
                .collect(Collectors.toSet());
    }

    private static ImmutableSet<Integer> getStateHashes(Set<BlockState> blockstates) {
        return ImmutableSet.copyOf(
                blockstates.stream()
                        .map(BlockState::hashCode)
                        .toArray(Integer[]::new)
        );
    }

    private static ImmutableSet<Integer> getStackHashes(Set<BlockState> blockstates) {
        //noinspection ConstantConditions
        return ImmutableSet.copyOf(
                blockstates.stream()
                        .flatMap(state -> drops(state.getBlock())
                                .stream()
                                .map(item -> new ItemStack(item, 1))
                        )
                        .map(stack -> ((IItemStack) (Object) stack).getBaritoneHash())
                        .toArray(Integer[]::new)
        );
    }

    public Block getBlock() {
        return block;
    }

    public boolean matches(@Nonnull Block block) {
        return block == this.block;
    }

    public boolean matches(@Nonnull BlockState blockstate) {
        Block block = blockstate.getBlock();
        return block == this.block && stateHashes.contains(blockstate.hashCode());
    }

    public boolean matches(ItemStack stack) {
        //noinspection ConstantConditions
        int hash = ((IItemStack) (Object) stack).getBaritoneHash();

        hash -= stack.getDamageValue();

        return stackHashes.contains(hash);
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s,properties=%s}", block, propertiesDescription);
    }

    public BlockState getAnyBlockState() {
        if (blockstates.size() > 0) {
            return blockstates.iterator().next();
        }

        return null;
    }

    public Set<BlockState> getAllBlockStates() {
        return blockstates;
    }

    public Set<Integer> stackHashes() {
        return stackHashes;
    }

    public static synchronized ReloadableServerRegistries.Holder getManager() {
        if (lootTables == null) {
            VanillaPackResources vanillaPack = ServerPacksSource.createVanillaPackSource();
            try (MultiPackResourceManager resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(vanillaPack))) {
                ReloadableServerRegistries.LoadResult result = ReloadableServerRegistries.reload(
                        RegistryLayer.createRegistryAccess(),
                        Collections.<Registry.PendingTags<?>>emptyList(),
                        resources,
                        Util.backgroundExecutor()
                ).get();
                lootTables = new ReloadableServerRegistries.Holder(result.lookupWithUpdatedTags());
            } catch (Exception exception) {
                throw new RuntimeException("Failed to load vanilla loot tables", exception);
            }
        }
        return lootTables;
    }

    private static synchronized List<Item> drops(Block b) {
        return drops.computeIfAbsent(b, block -> {
            if (block == Blocks.AIR) {
                return Collections.emptyList();
            }
            Optional<ResourceKey<LootTable>> lootTable = block.getLootTable();
            if (lootTable.isEmpty()) {
                return fallbackDrops(block);
            }

            List<Item> items = new ArrayList<>();
            try {
                LootParams params = new LootParams.Builder(ServerLevelStub.fastCreate())
                        .withParameter(LootContextParams.ORIGIN, Vec3.atLowerCornerOf(BlockPos.ZERO))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, null)
                        .withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
                        .create(LootContextParamSets.BLOCK);
                getManager().getLootTable(lootTable.get()).getRandomItemsRaw(
                        new LootContext.Builder(params)
                                .withOptionalRandomSeed(1L)
                                .create(Optional.of(lootTable.get().identifier())),
                        stack -> items.add(stack.getItem())
                );
            } catch (Exception exception) {
                items.addAll(fallbackDrops(block));
            }
            return items.isEmpty() ? fallbackDrops(block) : items;
        });
    }

    private static List<Item> fallbackDrops(Block block) {
        Item item = block.asItem();
        if (item == null || item == ItemStack.EMPTY.getItem()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(item);
    }

    private static class ServerLevelStub extends ServerLevel {
        private static final Unsafe unsafe = getUnsafe();

        public ServerLevelStub(MinecraftServer server, Executor executor,
                LevelStorageSource.LevelStorageAccess storageAccess, ServerLevelData levelData,
                ResourceKey<Level> dimension, LevelStem dimensionTypeRegistration, boolean debug,
                long seed, List<CustomSpawner> customSpawners, boolean tickTime, RandomSequences randomSequences) {
            super(server, executor, storageAccess, levelData, dimension, dimensionTypeRegistration, debug, seed,
                    customSpawners, tickTime, randomSequences);
        }

        @Override
        public FeatureFlagSet enabledFeatures() {
            Minecraft client = Minecraft.getInstance();
            return client.level != null ? client.level.enabledFeatures() : FeatureFlags.DEFAULT_FLAGS;
        }

        public static ServerLevelStub fastCreate() {
            try {
                return (ServerLevelStub) unsafe.allocateInstance(ServerLevelStub.class);
            } catch (InstantiationException exception) {
                throw new RuntimeException(exception);
            }
        }

        private static Unsafe getUnsafe() {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(null);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}

