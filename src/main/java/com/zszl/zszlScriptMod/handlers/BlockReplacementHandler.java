package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zszl.zszlScriptMod.system.BlockReplacementRule;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BlockReplacementHandler {

    public static final BlockReplacementHandler INSTANCE = new BlockReplacementHandler();
    public static final List<BlockReplacementRule> rules = new CopyOnWriteArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CATEGORY_DEFAULT = "默认";
    private static final List<String> categories = new CopyOnWriteArrayList<>();
    private static final Minecraft MC = Minecraft.getInstance();
    private static final long CACHE_REFRESH_MS = 1000L;
    private static final double MAX_RENDER_DISTANCE_SQ = 96.0D * 96.0D;

    private enum SelectionMode {
        NONE,
        REGION,
        SOURCE_BLOCK,
        TARGET_BLOCK
    }

    private static final class RegionCache {
        long lastRefreshAt;
        List<BlockCountEntry> blockCounts = new ArrayList<>();
        Map<String, List<BlockPos>> positionsBySource = new HashMap<>();
    }

    private final Map<BlockReplacementRule, RegionCache> regionCacheMap = new HashMap<>();
    private static volatile SelectionMode selectionMode = SelectionMode.NONE;
    private static volatile BlockReplacementRule selectionRule = null;
    private static volatile BlockReplacementRule.BlockReplacementEntry selectionEntry = null;
    private static volatile GuiScreen selectionReturnScreen = null;
    private static volatile boolean regionCorner1SelectedThisSession = false;
    private static volatile boolean regionCorner2SelectedThisSession = false;

    public static final class BlockCountEntry {
        public final String blockId;
        public final int count;

        public BlockCountEntry(String blockId, int count) {
            this.blockId = blockId;
            this.count = count;
        }
    }

    private BlockReplacementHandler() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("block_replacement_rules.json");
    }

    public static synchronized void loadConfig() {
        rules.clear();
        categories.clear();
        Path configFile = getConfigFile();
        if (!Files.exists(configFile)) {
            ensureCategoriesSynced();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("categories") && root.get("categories").isJsonArray()) {
                root.getAsJsonArray("categories").forEach(element -> categories.add(normalizeCategory(element.getAsString())));
            }
            if (root.has("rules")) {
                Type listType = new TypeToken<ArrayList<BlockReplacementRule>>() {
                }.getType();
                List<BlockReplacementRule> loaded = GSON.fromJson(root.get("rules"), listType);
                if (loaded != null) {
                    for (BlockReplacementRule rule : loaded) {
                        if (rule == null) {
                            continue;
                        }
                        if (rule.replacements == null) {
                            rule.replacements = new ArrayList<>();
                        }
                        rule.category = normalizeCategory(rule.category);
                        rule.dirty = true;
                        rules.add(rule);
                    }
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法加载区域方块替换规则", e);
        }
        ensureCategoriesSynced();
    }

    public static synchronized void saveConfig() {
        try {
            ensureCategoriesSynced();
            Path configFile = getConfigFile();
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonObject();
                root.add("categories", GSON.toJsonTree(new ArrayList<>(categories)));
                root.add("rules", GSON.toJsonTree(rules));
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法保存区域方块替换规则", e);
        }
    }

    public static synchronized List<String> getCategoriesSnapshot() {
        ensureCategoriesSynced();
        return new ArrayList<>(categories);
    }

    public static synchronized void replaceCategoryOrder(List<String> orderedCategories) {
        ensureCategoriesSynced();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (orderedCategories != null) {
            for (String category : orderedCategories) {
                normalized.add(normalizeCategory(category));
            }
        }
        for (String category : categories) {
            normalized.add(normalizeCategory(category));
        }
        categories.clear();
        categories.addAll(normalized);
        saveConfig();
    }

    public static synchronized boolean addCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();
        if (categories.contains(normalized)) {
            return false;
        }
        categories.add(normalized);
        saveConfig();
        return true;
    }

    public static synchronized boolean renameCategory(String oldCategory, String newCategory) {
        String normalizedOld = normalizeCategory(oldCategory);
        String normalizedNew = normalizeCategory(newCategory);
        boolean changed = false;
        for (int i = 0; i < categories.size(); i++) {
            if (normalizeCategory(categories.get(i)).equalsIgnoreCase(normalizedOld)) {
                categories.set(i, normalizedNew);
                changed = true;
            }
        }
        for (BlockReplacementRule rule : rules) {
            if (rule != null && normalizeCategory(rule.category).equalsIgnoreCase(normalizedOld)) {
                rule.category = normalizedNew;
                changed = true;
            }
        }
        if (changed) {
            saveConfig();
        }
        return changed;
    }

    public static synchronized boolean deleteCategory(String category) {
        String normalized = normalizeCategory(category);
        boolean changed = categories.removeIf(existing -> normalizeCategory(existing).equalsIgnoreCase(normalized));
        for (BlockReplacementRule rule : rules) {
            if (rule != null && normalizeCategory(rule.category).equalsIgnoreCase(normalized)) {
                rule.category = CATEGORY_DEFAULT;
                changed = true;
            }
        }
        if (changed) {
            saveConfig();
        }
        return changed;
    }

    public static void startRegionSelection(BlockReplacementRule rule, GuiScreen returnScreen) {
        selectionMode = SelectionMode.REGION;
        selectionRule = rule;
        selectionEntry = null;
        selectionReturnScreen = returnScreen;
        regionCorner1SelectedThisSession = false;
        regionCorner2SelectedThisSession = false;
        if (MC.player != null) {
            MC.player.sendSystemMessage(new TextComponentString(
                    "§b[区域方块替换] §f可视化选择已开启：§a左键选择角1§f，§e右键选择角2§f。"));
        }
        MC.setScreen(null);
    }

    public static void startSourceBlockSelection(BlockReplacementRule rule,
            BlockReplacementRule.BlockReplacementEntry entry, GuiScreen returnScreen) {
        selectionMode = SelectionMode.SOURCE_BLOCK;
        selectionRule = rule;
        selectionEntry = entry;
        selectionReturnScreen = returnScreen;
        if (MC.player != null) {
            MC.player.sendSystemMessage(new TextComponentString(
                    "§b[区域方块替换] §f请左键或右键点击一个方块，作为§a被替换方块§f。"));
        }
        MC.setScreen(null);
    }

    public static void startSourceBlockSelection(BlockReplacementRule.BlockReplacementEntry entry, GuiScreen returnScreen) {
        startSourceBlockSelection(null, entry, returnScreen);
    }

    public static void startTargetBlockSelection(BlockReplacementRule rule,
            BlockReplacementRule.BlockReplacementEntry entry, GuiScreen returnScreen) {
        selectionMode = SelectionMode.TARGET_BLOCK;
        selectionRule = rule;
        selectionEntry = entry;
        selectionReturnScreen = returnScreen;
        if (MC.player != null) {
            MC.player.sendSystemMessage(new TextComponentString(
                    "§b[区域方块替换] §f请左键或右键点击一个方块，作为§a替换后显示方块§f。"));
        }
        MC.setScreen(null);
    }

    public static void startTargetBlockSelection(BlockReplacementRule.BlockReplacementEntry entry, GuiScreen returnScreen) {
        startTargetBlockSelection(null, entry, returnScreen);
    }

    public static void markRuleDirty(BlockReplacementRule rule) {
        if (rule != null) {
            rule.dirty = true;
        }
    }

    public static List<BlockCountEntry> getAvailableBlocks(BlockReplacementRule rule) {
        return INSTANCE.getOrBuildCache(rule).blockCounts;
    }

    public static VoxelShape getCollisionShapeOverride(BlockGetter world, BlockPos pos, CollisionContext context) {
        return INSTANCE.resolveReplacementCollisionShape(world, pos, context);
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide || selectionMode == SelectionMode.NONE || event.getEntity() != MC.player) {
            return;
        }
        handleSelectionClick(event.getPos(), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide
                || selectionMode == SelectionMode.NONE
                || event.getEntity() != MC.player
                || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        handleSelectionClick(event.getPos(), false);
        event.setCanceled(true);
    }

    private void handleSelectionClick(BlockPos pos, boolean leftClick) {
        if (MC.level == null || pos == null) {
            return;
        }
        switch (selectionMode) {
            case REGION:
                if (selectionRule == null) {
                    return;
                }
                if (leftClick) {
                    selectionRule.setCorner1(pos.getX(), pos.getY(), pos.getZ());
                    regionCorner1SelectedThisSession = true;
                    if (MC.player != null) {
                        MC.player.sendSystemMessage(new TextComponentString("§a[区域方块替换] 已设置角1: " + pos));
                    }
                } else {
                    selectionRule.setCorner2(pos.getX(), pos.getY(), pos.getZ());
                    regionCorner2SelectedThisSession = true;
                    if (MC.player != null) {
                        MC.player.sendSystemMessage(new TextComponentString("§e[区域方块替换] 已设置角2: " + pos));
                    }
                }
                finishSelectionIfPossible();
                break;
            case SOURCE_BLOCK:
            case TARGET_BLOCK:
                if (selectionEntry == null) {
                    return;
                }
                String blockId = getBlockId(MC.level.getBlockState(pos));
                if (selectionMode == SelectionMode.SOURCE_BLOCK) {
                    selectionEntry.sourceBlockId = blockId;
                    if (MC.player != null) {
                        MC.player.sendSystemMessage(new TextComponentString("§a[区域方块替换] 被替换方块: " + blockId));
                    }
                } else {
                    selectionEntry.targetBlockId = blockId;
                    if (MC.player != null) {
                        MC.player.sendSystemMessage(new TextComponentString("§a[区域方块替换] 替换后方块: " + blockId));
                    }
                }
                if (selectionRule != null) {
                    markRuleDirty(selectionRule);
                }
                finishSelectionIfPossible();
                break;
            default:
                break;
        }
    }

    private static void finishSelectionIfPossible() {
        if (selectionMode == SelectionMode.REGION) {
            if (selectionRule != null && selectionRule.hasValidRegion()
                    && (regionCorner1SelectedThisSession || selectionRule.hasCorner1())
                    && (regionCorner2SelectedThisSession || selectionRule.hasCorner2())) {
                GuiScreen returnScreen = selectionReturnScreen;
                selectionMode = SelectionMode.NONE;
                selectionRule = null;
                selectionEntry = null;
                selectionReturnScreen = null;
                if (returnScreen != null) {
                    MC.execute(() -> MC.setScreen(returnScreen));
                }
            }
            return;
        }

        GuiScreen returnScreen = selectionReturnScreen;
        selectionMode = SelectionMode.NONE;
        selectionRule = null;
        selectionEntry = null;
        selectionReturnScreen = null;
        if (returnScreen != null) {
            MC.execute(() -> MC.setScreen(returnScreen));
        }
    }

    private synchronized RegionCache getOrBuildCache(BlockReplacementRule rule) {
        RegionCache cache = regionCacheMap.get(rule);
        long now = System.currentTimeMillis();
        if (rule == null || !rule.hasValidRegion() || MC.level == null) {
            return new RegionCache();
        }
        if (cache == null || rule.dirty || now - cache.lastRefreshAt > CACHE_REFRESH_MS) {
            cache = rebuildCache(rule, now);
            regionCacheMap.put(rule, cache);
            rule.dirty = false;
        }
        return cache;
    }

    private RegionCache rebuildCache(BlockReplacementRule rule, long now) {
        RegionCache cache = new RegionCache();
        cache.lastRefreshAt = now;

        Map<String, Integer> counts = new HashMap<>();
        Map<String, List<BlockPos>> positions = new HashMap<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = rule.getMinX(); x <= rule.getMaxX(); x++) {
            for (int y = rule.getMinY(); y <= rule.getMaxY(); y++) {
                for (int z = rule.getMinZ(); z <= rule.getMaxZ(); z++) {
                    mutable.set(x, y, z);
                    String blockId = getBlockId(MC.level.getBlockState(mutable));
                    counts.put(blockId, counts.getOrDefault(blockId, 0) + 1);
                    positions.computeIfAbsent(blockId, key -> new ArrayList<>()).add(mutable.immutable());
                }
            }
        }

        List<BlockCountEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            entries.add(new BlockCountEntry(entry.getKey(), entry.getValue()));
        }
        entries.sort(Comparator.comparing(e -> e.blockId));
        cache.blockCounts = entries;
        cache.positionsBySource = positions;
        return cache;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        RenderWorldLastEvent.WorldRenderContext renderContext = event.getWorldRenderContext();
        if (MC.player == null || MC.level == null || renderContext == null) {
            return;
        }

        PoseStack blockPoseStack = event.createWorldPoseStack();
        PoseStack linePoseStack = event.createWorldPoseStack();
        if (blockPoseStack == null || linePoseStack == null) {
            return;
        }
        MultiBufferSource.BufferSource bufferSource = MC.renderBuffers().bufferSource();
        List<AABB> highlightBoxes = new ArrayList<>();

        for (BlockReplacementRule rule : rules) {
            if (rule == null || !rule.enabled || !rule.hasValidRegion() || rule.replacements == null
                    || rule.replacements.isEmpty()) {
                continue;
            }

            RegionCache cache = getOrBuildCache(rule);
            for (BlockReplacementRule.BlockReplacementEntry entry : rule.replacements) {
                if (entry == null || !entry.enabled || entry.sourceBlockId == null || entry.targetBlockId == null) {
                    continue;
                }
                List<BlockPos> positions = cache.positionsBySource.get(entry.sourceBlockId);
                if (positions == null || positions.isEmpty()) {
                    continue;
                }

                BlockState targetState = getBlockStateById(entry.targetBlockId);
                if (targetState == null) {
                    continue;
                }

                for (BlockPos pos : positions) {
                    if (MC.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > MAX_RENDER_DISTANCE_SQ) {
                        continue;
                    }
                    renderReplacementBlock(blockPoseStack, bufferSource, renderContext, targetState, pos);
                    if (rule.highlightReplacedBlocks) {
                        highlightBoxes.add(renderContext.toCameraSpace(new AABB(pos)).inflate(0.002D));
                    }
                }
            }
        }

        if (selectionMode == SelectionMode.REGION && selectionRule != null) {
            addSelectionRegionBox(highlightBoxes, renderContext);
        }

        bufferSource.endBatch();

        if (!highlightBoxes.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            BufferBuilder lineBuffer = Tesselator.getInstance().getBuilder();
            lineBuffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
            for (AABB box : highlightBoxes) {
                LevelRenderer.renderLineBox(linePoseStack, lineBuffer, box, 0.2F, 1.0F, 0.3F, 0.8F);
            }
            Tesselator.getInstance().end();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private void renderReplacementBlock(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
            RenderWorldLastEvent.WorldRenderContext renderContext, BlockState targetState, BlockPos pos) {
        Vec3 cameraSpacePos = renderContext.toCameraSpace(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
        poseStack.pushPose();
        poseStack.translate(cameraSpacePos.x, cameraSpacePos.y, cameraSpacePos.z);
        MC.getBlockRenderer().renderSingleBlock(targetState, poseStack, bufferSource,
                LevelRenderer.getLightColor(MC.level, pos), OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private void addSelectionRegionBox(List<AABB> highlightBoxes, RenderWorldLastEvent.WorldRenderContext renderContext) {
        if (selectionRule == null) {
            return;
        }
        BlockReplacementRule rule = selectionRule;
        if (!rule.hasCorner1() && !rule.hasCorner2()) {
            return;
        }
        Integer minXValue = rule.hasValidRegion() ? rule.getMinX()
                : (rule.corner1X != null ? rule.corner1X : rule.corner2X);
        Integer minYValue = rule.hasValidRegion() ? rule.getMinY()
                : (rule.corner1Y != null ? rule.corner1Y : rule.corner2Y);
        Integer minZValue = rule.hasValidRegion() ? rule.getMinZ()
                : (rule.corner1Z != null ? rule.corner1Z : rule.corner2Z);
        Integer maxXValue = rule.hasValidRegion() ? rule.getMaxX()
                : (rule.corner2X != null ? rule.corner2X : rule.corner1X);
        Integer maxYValue = rule.hasValidRegion() ? rule.getMaxY()
                : (rule.corner2Y != null ? rule.corner2Y : rule.corner1Y);
        Integer maxZValue = rule.hasValidRegion() ? rule.getMaxZ()
                : (rule.corner2Z != null ? rule.corner2Z : rule.corner1Z);
        if (minXValue == null || minYValue == null || minZValue == null
                || maxXValue == null || maxYValue == null || maxZValue == null) {
            return;
        }
        AABB box = renderContext.toCameraSpace(new AABB(
                minXValue, minYValue, minZValue,
                maxXValue + 1.0D, maxYValue + 1.0D, maxZValue + 1.0D)).inflate(0.002D);
        highlightBoxes.add(box);
    }

    private VoxelShape resolveReplacementCollisionShape(BlockGetter world, BlockPos pos, CollisionContext context) {
        if (!(world instanceof Level) || pos == null || MC.level == null || world != MC.level) {
            return null;
        }
        String currentBlockId = getBlockId(world.getBlockState(pos));
        for (BlockReplacementRule rule : rules) {
            if (rule == null || !rule.enabled || !rule.useSolidCollision || !rule.hasValidRegion()
                    || rule.replacements == null || rule.replacements.isEmpty()) {
                continue;
            }
            if (pos.getX() < rule.getMinX() || pos.getX() > rule.getMaxX()
                    || pos.getY() < rule.getMinY() || pos.getY() > rule.getMaxY()
                    || pos.getZ() < rule.getMinZ() || pos.getZ() > rule.getMaxZ()) {
                continue;
            }
            for (BlockReplacementRule.BlockReplacementEntry entry : rule.replacements) {
                if (entry == null || !entry.enabled || entry.sourceBlockId == null || entry.targetBlockId == null) {
                    continue;
                }
                if (!entry.sourceBlockId.equals(currentBlockId)) {
                    continue;
                }
                BlockState targetState = getBlockStateById(entry.targetBlockId);
                if (targetState != null) {
                    return targetState.getBlock().getCollisionShape(targetState, world, pos, context);
                }
            }
        }
        return null;
    }

    private static String getBlockId(BlockState state) {
        if (state == null) {
            return "minecraft:air";
        }
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "minecraft:air" : key.toString();
    }

    private static BlockState getBlockStateById(String blockId) {
        ResourceLocation id = ResourceLocation.tryParse(blockId == null ? "" : blockId.trim());
        if (id == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        return block.defaultBlockState();
    }

    private static String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
    }

    private static void ensureCategoriesSynced() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String category : categories) {
            normalized.add(normalizeCategory(category));
        }
        for (BlockReplacementRule rule : rules) {
            if (rule != null) {
                rule.category = normalizeCategory(rule.category);
                normalized.add(rule.category);
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(CATEGORY_DEFAULT);
        }
        categories.clear();
        categories.addAll(normalized);
    }
}


