package com.zszl.zszlScriptMod.otherfeatures.handler.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.shaders.FogShape;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RenderFeatureManager {

    public static final RenderFeatureManager INSTANCE = new RenderFeatureManager();

    private static final Map<String, FeatureState> FEATURES = new LinkedHashMap<>();
    private static final String[] DEFAULT_XRAY_BLOCK_IDS = new String[] {
            "minecraft:coal_ore",
            "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore",
            "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore",
            "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore",
            "minecraft:deepslate_gold_ore",
            "minecraft:redstone_ore",
            "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore",
            "minecraft:deepslate_lapis_ore",
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:deepslate_emerald_ore",
            "minecraft:nether_quartz_ore",
            "minecraft:nether_gold_ore",
            "minecraft:ancient_debris",
            "minecraft:spawner",
            "minecraft:lava",
            "minecraft:end_portal",
            "minecraft:end_portal_frame",
            "minecraft:portal"
    };
    private static final Set<String> XRAY_VISIBLE_BLOCK_IDS = new LinkedHashSet<>();

    public static boolean brightnessSoftMode = false;
    public static float brightnessGamma = 10.0F;
    public static boolean noFogRemoveLiquid = true;
    public static boolean noFogBrightenColor = true;
    public static boolean entityVisualPlayers = true;
    public static boolean entityVisualMonsters = true;
    public static boolean entityVisualAnimals = false;
    public static boolean entityVisualThroughWalls = true;
    public static boolean entityVisualFilledBox = true;
    public static float entityVisualMaxDistance = 48.0F;
    public static boolean tracerPlayers = true;
    public static boolean tracerMonsters = true;
    public static boolean tracerAnimals = false;
    public static boolean tracerThroughWalls = true;
    public static float tracerMaxDistance = 64.0F;
    public static float tracerLineWidth = 1.6F;
    public static boolean entityTagPlayers = true;
    public static boolean entityTagMonsters = true;
    public static boolean entityTagAnimals = false;
    public static boolean entityTagShowHealth = true;
    public static boolean entityTagShowDistance = true;
    public static boolean entityTagShowHeldItem = false;
    public static float entityTagMaxDistance = 32.0F;
    public static boolean blockHighlightStorages = true;
    public static boolean blockHighlightSpawners = true;
    public static boolean blockHighlightOres = true;
    public static boolean blockHighlightThroughWalls = true;
    public static boolean blockHighlightFilledBox = true;
    public static float blockHighlightMaxDistance = 24.0F;
    public static boolean itemEspShowName = true;
    public static boolean itemEspShowDistance = true;
    public static boolean itemEspThroughWalls = true;
    public static float itemEspMaxDistance = 24.0F;
    public static boolean trajectoryBows = true;
    public static boolean trajectoryPearls = true;
    public static boolean trajectoryThrowables = true;
    public static boolean trajectoryPotions = true;
    public static int trajectoryMaxSteps = 120;
    public static boolean crosshairDynamicGap = true;
    public static int crosshairColorRgb = 0x55FFFF;
    public static float crosshairSize = 6.0F;
    public static float crosshairThickness = 2.0F;
    public static boolean antiBobRemoveViewBobbing = true;
    public static boolean antiBobRemoveHurtShake = true;
    public static boolean radarPlayers = true;
    public static boolean radarMonsters = true;
    public static boolean radarAnimals = false;
    public static boolean radarRotateWithView = true;
    public static float radarMaxDistance = 48.0F;
    public static int radarSize = 90;
    public static boolean skeletonThroughWalls = true;
    public static float skeletonMaxDistance = 48.0F;
    public static float skeletonLineWidth = 1.7F;
    public static boolean blockOutlineFilledBox = false;
    public static float blockOutlineLineWidth = 2.0F;
    public static boolean entityInfoShowHealth = true;
    public static boolean entityInfoShowDistance = true;
    public static boolean entityInfoShowPosition = true;
    public static boolean entityInfoShowHeldItem = true;
    public static float entityInfoMaxDistance = 48.0F;

    private boolean antiBobApplied;
    private boolean previousViewBobbing = true;

    static {
        register(new FeatureState("brightness_boost", "亮度增强", "通过 Gamma 提升整体亮度。"));
        register(new FeatureState("no_fog", "迷雾移除", "尽量移除世界和液体中的雾气遮挡。"));
        register(new FeatureState("entity_visual", "实体视觉", "高亮玩家、怪物和动物轮廓。"));
        register(new FeatureState("tracer_line", "Tracer线", "向附近目标绘制引导线。"));
        register(new FeatureState("entity_tags", "实体标签", "显示实体名称、血量、距离和手持物。"));
        register(new FeatureState("block_highlight", "方块高亮", "高亮矿石、箱子和刷怪笼等重要方块。"));
        register(new FeatureState("xray", "X光", "仅保留透视列表中的方块可见。"));
        register(new FeatureState("item_esp", "物品ESP", "高亮地面掉落物。"));
        register(new FeatureState("trajectory_line", "轨迹线", "预估弓箭和投掷物的飞行轨迹。"));
        register(new FeatureState("custom_crosshair", "自定义十字准星", "使用可调颜色、大小和动态间距的准星。"));
        register(new FeatureState("anti_bob", "防抖动", "抑制走路视角晃动和受伤镜头抖动。"));
        register(new FeatureState("radar", "雷达", "在屏幕角落显示附近目标的相对位置。"));
        register(new FeatureState("player_skeleton", "玩家骨骼", "以骨架线条形式显示附近玩家站姿和朝向。"));
        register(new FeatureState("block_outline", "方块轮廓", "为当前选中的方块显示更清晰的边框。"));
        register(new FeatureState("entity_info", "实体信息", "准星指向实体时显示更详细的信息面板。"));
        loadConfig();
    }

    private RenderFeatureManager() {
    }

    public static final class FeatureState {
        public final String id;
        public final String name;
        public final String description;
        private boolean enabled;

        private FeatureState(String id, String name, String description) {
            this.id = safe(id);
            this.name = safe(name);
            this.description = safe(description);
        }

        public boolean isEnabled() {
            return enabled;
        }

        private void setEnabledInternal(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static List<FeatureState> getFeatures() {
        return new ArrayList<>(FEATURES.values());
    }

    public static FeatureState getFeature(String featureId) {
        return FEATURES.get(normalizeId(featureId));
    }

    public static boolean isManagedFeature(String featureId) {
        return FEATURES.containsKey(normalizeId(featureId));
    }

    public static boolean isEnabled(String featureId) {
        FeatureState state = getFeature(featureId);
        return state != null && state.isEnabled();
    }

    public static void toggleFeature(String featureId) {
        setEnabled(featureId, !isEnabled(featureId));
    }

    public static void setEnabled(String featureId, boolean enabled) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        boolean previous = state.isEnabled();
        state.setEnabledInternal(enabled);
        handleRuntimeSideEffectsAfterToggle(featureId, previous, enabled);
        saveConfig();
    }

    public static void setEnabledTransient(String featureId, boolean enabled) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        boolean previous = state.isEnabled();
        state.setEnabledInternal(enabled);
        handleRuntimeSideEffectsAfterToggle(featureId, previous, enabled);
    }

    public static void resetFeature(String featureId) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        boolean previous = state.isEnabled();
        state.setEnabledInternal(false);
        applyFeatureDefaultsWithoutSave(state.id);
        handleRuntimeSideEffectsAfterToggle(state.id, previous, false);
        saveConfig();
    }

    public static void applyFeatureDefaultsWithoutSave(String featureId) {
        String normalizedId = normalizeId(featureId);
        switch (normalizedId) {
        case "brightness_boost":
            brightnessSoftMode = false;
            brightnessGamma = 10.0F;
            break;
        case "no_fog":
            noFogRemoveLiquid = true;
            noFogBrightenColor = true;
            break;
        case "entity_visual":
            entityVisualPlayers = true;
            entityVisualMonsters = true;
            entityVisualAnimals = false;
            entityVisualThroughWalls = true;
            entityVisualFilledBox = true;
            entityVisualMaxDistance = 48.0F;
            break;
        case "tracer_line":
            tracerPlayers = true;
            tracerMonsters = true;
            tracerAnimals = false;
            tracerThroughWalls = true;
            tracerMaxDistance = 64.0F;
            tracerLineWidth = 1.6F;
            break;
        case "entity_tags":
            entityTagPlayers = true;
            entityTagMonsters = true;
            entityTagAnimals = false;
            entityTagShowHealth = true;
            entityTagShowDistance = true;
            entityTagShowHeldItem = false;
            entityTagMaxDistance = 32.0F;
            break;
        case "block_highlight":
            blockHighlightStorages = true;
            blockHighlightSpawners = true;
            blockHighlightOres = true;
            blockHighlightThroughWalls = true;
            blockHighlightFilledBox = true;
            blockHighlightMaxDistance = 24.0F;
            break;
        case "xray":
            resetXrayVisibleBlocksWithoutSave();
            break;
        case "item_esp":
            itemEspShowName = true;
            itemEspShowDistance = true;
            itemEspThroughWalls = true;
            itemEspMaxDistance = 24.0F;
            break;
        case "trajectory_line":
            trajectoryBows = true;
            trajectoryPearls = true;
            trajectoryThrowables = true;
            trajectoryPotions = true;
            trajectoryMaxSteps = 120;
            break;
        case "custom_crosshair":
            crosshairDynamicGap = true;
            crosshairColorRgb = 0x55FFFF;
            crosshairSize = 6.0F;
            crosshairThickness = 2.0F;
            break;
        case "anti_bob":
            antiBobRemoveViewBobbing = true;
            antiBobRemoveHurtShake = true;
            break;
        case "radar":
            radarPlayers = true;
            radarMonsters = true;
            radarAnimals = false;
            radarRotateWithView = true;
            radarMaxDistance = 48.0F;
            radarSize = 90;
            break;
        case "player_skeleton":
            skeletonThroughWalls = true;
            skeletonMaxDistance = 48.0F;
            skeletonLineWidth = 1.7F;
            break;
        case "block_outline":
            blockOutlineFilledBox = false;
            blockOutlineLineWidth = 2.0F;
            break;
        case "entity_info":
            entityInfoShowHealth = true;
            entityInfoShowDistance = true;
            entityInfoShowPosition = true;
            entityInfoShowHeldItem = true;
            entityInfoMaxDistance = 48.0F;
            break;
        default:
            break;
        }
    }

    public static String getFeatureRuntimeSummary(String featureId) {
        String normalizedId = normalizeId(featureId);
        switch (normalizedId) {
        case "brightness_boost":
            return isEnabled(normalizedId) ? "Gamma增强中，当前值 " + formatFloat(brightnessGamma) : "未启用";
        case "no_fog":
            return isEnabled(normalizedId)
                    ? "世界雾已压低" + (noFogRemoveLiquid ? "，液体雾同步移除" : "")
                    : "未启用";
        case "entity_visual":
            return isEnabled(normalizedId) ? "实体轮廓范围 " + formatFloat(entityVisualMaxDistance) + " 格" : "未启用";
        case "tracer_line":
            return isEnabled(normalizedId) ? "引导线范围 " + formatFloat(tracerMaxDistance) + " 格" : "未启用";
        case "entity_tags":
            return isEnabled(normalizedId) ? "头顶信息范围 " + formatFloat(entityTagMaxDistance) + " 格" : "未启用";
        case "block_highlight":
            return isEnabled(normalizedId)
                    ? "高亮重要方块，范围 " + formatFloat(blockHighlightMaxDistance) + " 格"
                    : "未启用";
        case "xray":
            return isEnabled(normalizedId)
                    ? "透视列表 " + XRAY_VISIBLE_BLOCK_IDS.size() + " 项 / 需要重编译区块"
                    : "未启用";
        case "item_esp":
            return isEnabled(normalizedId) ? "掉落物高亮范围 " + formatFloat(itemEspMaxDistance) + " 格" : "未启用";
        case "trajectory_line":
            return isEnabled(normalizedId) ? "支持弓/珍珠/雪球/药水轨迹预估" : "未启用";
        case "custom_crosshair":
            return isEnabled(normalizedId)
                    ? "准星颜色 #" + String.format(Locale.ROOT, "%06X", crosshairColorRgb & 0xFFFFFF)
                    : "未启用";
        case "anti_bob":
            return isEnabled(normalizedId) ? "已抑制镜头晃动" : "未启用";
        case "radar":
            return isEnabled(normalizedId) ? "雷达范围 " + formatFloat(radarMaxDistance) + " 格" : "未启用";
        case "player_skeleton":
            return isEnabled(normalizedId) ? "骨架范围 " + formatFloat(skeletonMaxDistance) + " 格" : "未启用";
        case "block_outline":
            return isEnabled(normalizedId) ? "高亮当前选中方块边框" : "未启用";
        case "entity_info":
            return isEnabled(normalizedId) ? "实体信息范围 " + formatFloat(entityInfoMaxDistance) + " 格" : "未启用";
        default:
            return "待机";
        }
    }

    public static boolean shouldSuppressViewBobbing() {
        return isEnabled("anti_bob") && antiBobRemoveViewBobbing;
    }

    public static boolean shouldSuppressHurtCamera() {
        return isEnabled("anti_bob") && antiBobRemoveHurtShake;
    }

    public static boolean isOreBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        String id = normalizeBlockId(state.getBlock());
        return id.contains("ore") || id.contains("debris");
    }

    public static int getBlockHighlightColor(BlockState state) {
        String id = normalizeBlockId(state == null ? null : state.getBlock());
        if (id.contains("coal")) {
            return 0x707070;
        }
        if (id.contains("iron")) {
            return 0xD2A97A;
        }
        if (id.contains("copper")) {
            return 0xD07A4E;
        }
        if (id.contains("gold")) {
            return 0xFFD53D;
        }
        if (id.contains("redstone")) {
            return 0xFF4545;
        }
        if (id.contains("lapis")) {
            return 0x4A72FF;
        }
        if (id.contains("diamond")) {
            return 0x34F0EF;
        }
        if (id.contains("emerald")) {
            return 0x3DFF66;
        }
        if (id.contains("quartz")) {
            return 0xF3F3F3;
        }
        if (id.contains("ancient_debris")) {
            return 0x9B6947;
        }
        if (id.contains("spawner")) {
            return 0xD35AFF;
        }
        if (id.contains("lava")) {
            return 0xFF8A33;
        }
        if (id.contains("portal")) {
            return 0xB97CFF;
        }
        return 0x55FFAA;
    }

    public static boolean shouldRenderBlockInXray(BlockState state) {
        return !isEnabled("xray") || isXrayBlockVisible(state);
    }

    public static boolean isXrayBlockVisible(BlockState state) {
        return state != null && isXrayBlockVisible(state.getBlock());
    }

    public static boolean isXrayBlockVisible(Block block) {
        String blockId = normalizeBlockId(block);
        return !blockId.isEmpty() && XRAY_VISIBLE_BLOCK_IDS.contains(blockId);
    }

    public static boolean isXrayBlockIdVisible(String rawBlockId) {
        String blockId = normalizeXrayBlockId(rawBlockId);
        return !blockId.isEmpty() && XRAY_VISIBLE_BLOCK_IDS.contains(blockId);
    }

    public static List<String> getXrayVisibleBlockIds() {
        return sortBlockIds(XRAY_VISIBLE_BLOCK_IDS);
    }

    public static Block resolveBlock(String rawBlockId) {
        Block block = BlockDisplayLookup.findBlockByUserInput(rawBlockId);
        return block == null || block == Blocks.AIR ? null : block;
    }

    public static String normalizeXrayBlockId(String rawBlockId) {
        return normalizeBlockId(resolveBlock(rawBlockId));
    }

    public static boolean setXrayBlockVisible(String rawBlockId, boolean visible) {
        String blockId = normalizeXrayBlockId(rawBlockId);
        if (blockId.isEmpty()) {
            return false;
        }
        boolean changed = visible ? XRAY_VISIBLE_BLOCK_IDS.add(blockId) : XRAY_VISIBLE_BLOCK_IDS.remove(blockId);
        if (changed) {
            requestXrayRendererReloadIfEnabled();
        }
        return changed;
    }

    public static void clearXrayVisibleBlocksWithoutSave() {
        if (XRAY_VISIBLE_BLOCK_IDS.isEmpty()) {
            return;
        }
        XRAY_VISIBLE_BLOCK_IDS.clear();
        requestXrayRendererReloadIfEnabled();
    }

    public static void resetXrayVisibleBlocksWithoutSave() {
        resetXrayVisibleBlocksToDefaultInternal();
        requestXrayRendererReloadIfEnabled();
    }

    public static void requestXrayRendererReload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.levelRenderer == null) {
            return;
        }
        mc.levelRenderer.allChanged();
    }

    public void tick(Minecraft mc) {
        LocalPlayer player = mc == null ? null : mc.player;
        if (mc == null || mc.level == null || player == null) {
            resetRuntimeState();
            RenderFeatureSupport.clearRuntimeCaches();
            return;
        }
        applyAntiBob(mc);
        RenderFeatureSupport.onClientTick(mc, player);
    }

    public void onClientDisconnect() {
        resetRuntimeState();
        RenderFeatureSupport.clearRuntimeCaches();
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }
        RenderFeatureSupport.renderWorld(mc, event);
    }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event == null || !isCrosshairOverlay(event) || !isEnabled("custom_crosshair")) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.screen != null || mc.player == null) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event == null) {
            return;
        }
        String overlayPath = overlayPath(event.getOverlay() == null ? null : event.getOverlay().id());
        if (!"hotbar".equals(overlayPath)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.screen != null) {
            return;
        }
        if (isEnabled("custom_crosshair")) {
            RenderFeatureSupport.renderCrosshair(mc, event.getGuiGraphics());
        }
        if (isEnabled("radar")) {
            RenderFeatureSupport.renderRadar(mc, event.getGuiGraphics());
        }
        if (isEnabled("entity_info")) {
            RenderFeatureSupport.renderEntityInfo(mc, event.getGuiGraphics());
        }
    }

    @SubscribeEvent
    public void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isEnabled("no_fog") || event == null) {
            return;
        }
        if (event.getType() != FogType.NONE && !noFogRemoveLiquid) {
            return;
        }
        float far = Math.max(event.getFarPlaneDistance(), 256.0F);
        event.setNearPlaneDistance(far * 0.96F);
        event.setFarPlaneDistance(far * 1.75F);
        event.setFogShape(FogShape.CYLINDER);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isEnabled("no_fog") || event == null || !noFogBrightenColor) {
            return;
        }
        event.setRed(Math.max(event.getRed(), 0.95F));
        event.setGreen(Math.max(event.getGreen(), 0.96F));
        event.setBlue(Math.max(event.getBlue(), 0.98F));
    }

    @SubscribeEvent
    public void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!shouldSuppressHurtCamera() || event == null) {
            return;
        }
        event.setRoll(0.0F);
    }

    public static void loadConfig() {
        for (FeatureState state : FEATURES.values()) {
            state.setEnabledInternal(false);
        }
        applyAllDefaultsWithoutSave();

        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_render.json");
            if (!Files.exists(file)) {
                saveConfig();
                return;
            }

            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (FeatureState state : FEATURES.values()) {
                state.setEnabledInternal(readFeatureEnabled(root, state.id));
            }

            brightnessSoftMode = readBoolean(root, "brightness_soft_mode", "brightnessSoftMode", brightnessSoftMode);
            brightnessGamma = readFloat(root, "brightness_gamma", "brightnessGamma", brightnessGamma);
            noFogRemoveLiquid = readBoolean(root, "no_fog_remove_liquid", "noFogRemoveLiquid", noFogRemoveLiquid);
            noFogBrightenColor = readBoolean(root, "no_fog_brighten_color", "noFogBrightenColor", noFogBrightenColor);
            entityVisualPlayers = readBoolean(root, "entity_visual_players", "entityVisualPlayers", entityVisualPlayers);
            entityVisualMonsters = readBoolean(root, "entity_visual_monsters", "entityVisualMonsters", entityVisualMonsters);
            entityVisualAnimals = readBoolean(root, "entity_visual_animals", "entityVisualAnimals", entityVisualAnimals);
            entityVisualThroughWalls = readBoolean(root, "entity_visual_through_walls", "entityVisualThroughWalls", entityVisualThroughWalls);
            entityVisualFilledBox = readBoolean(root, "entity_visual_filled_box", "entityVisualFilledBox", entityVisualFilledBox);
            entityVisualMaxDistance = readFloat(root, "entity_visual_max_distance", "entityVisualMaxDistance", entityVisualMaxDistance);
            tracerPlayers = readBoolean(root, "tracer_players", "tracerPlayers", tracerPlayers);
            tracerMonsters = readBoolean(root, "tracer_monsters", "tracerMonsters", tracerMonsters);
            tracerAnimals = readBoolean(root, "tracer_animals", "tracerAnimals", tracerAnimals);
            tracerThroughWalls = readBoolean(root, "tracer_through_walls", "tracerThroughWalls", tracerThroughWalls);
            tracerMaxDistance = readFloat(root, "tracer_max_distance", "tracerMaxDistance", tracerMaxDistance);
            tracerLineWidth = readFloat(root, "tracer_line_width", "tracerLineWidth", tracerLineWidth);
            entityTagPlayers = readBoolean(root, "entity_tag_players", "entityTagPlayers", entityTagPlayers);
            entityTagMonsters = readBoolean(root, "entity_tag_monsters", "entityTagMonsters", entityTagMonsters);
            entityTagAnimals = readBoolean(root, "entity_tag_animals", "entityTagAnimals", entityTagAnimals);
            entityTagShowHealth = readBoolean(root, "entity_tag_show_health", "entityTagShowHealth", entityTagShowHealth);
            entityTagShowDistance = readBoolean(root, "entity_tag_show_distance", "entityTagShowDistance", entityTagShowDistance);
            entityTagShowHeldItem = readBoolean(root, "entity_tag_show_held_item", "entityTagShowHeldItem", entityTagShowHeldItem);
            entityTagMaxDistance = readFloat(root, "entity_tag_max_distance", "entityTagMaxDistance", entityTagMaxDistance);
            blockHighlightStorages = readBoolean(root, "block_highlight_storages", "blockHighlightStorages", blockHighlightStorages);
            blockHighlightSpawners = readBoolean(root, "block_highlight_spawners", "blockHighlightSpawners", blockHighlightSpawners);
            blockHighlightOres = readBoolean(root, "block_highlight_ores", "blockHighlightOres", blockHighlightOres);
            blockHighlightThroughWalls = readBoolean(root, "block_highlight_through_walls", "blockHighlightThroughWalls", blockHighlightThroughWalls);
            blockHighlightFilledBox = readBoolean(root, "block_highlight_filled_box", "blockHighlightFilledBox", blockHighlightFilledBox);
            blockHighlightMaxDistance = readFloat(root, "block_highlight_max_distance", "blockHighlightMaxDistance", blockHighlightMaxDistance);
            loadXrayVisibleBlocks(root);
            itemEspShowName = readBoolean(root, "item_esp_show_name", "itemEspShowName", itemEspShowName);
            itemEspShowDistance = readBoolean(root, "item_esp_show_distance", "itemEspShowDistance", itemEspShowDistance);
            itemEspThroughWalls = readBoolean(root, "item_esp_through_walls", "itemEspThroughWalls", itemEspThroughWalls);
            itemEspMaxDistance = readFloat(root, "item_esp_max_distance", "itemEspMaxDistance", itemEspMaxDistance);
            trajectoryBows = readBoolean(root, "trajectory_bows", "trajectoryBows", trajectoryBows);
            trajectoryPearls = readBoolean(root, "trajectory_pearls", "trajectoryPearls", trajectoryPearls);
            trajectoryThrowables = readBoolean(root, "trajectory_throwables", "trajectoryThrowables", trajectoryThrowables);
            trajectoryPotions = readBoolean(root, "trajectory_potions", "trajectoryPotions", trajectoryPotions);
            trajectoryMaxSteps = readInt(root, "trajectory_max_steps", "trajectoryMaxSteps", trajectoryMaxSteps);
            crosshairDynamicGap = readBoolean(root, "crosshair_dynamic_gap", "crosshairDynamicGap", crosshairDynamicGap);
            crosshairColorRgb = readInt(root, "crosshair_color_rgb", "crosshairColorRgb", crosshairColorRgb);
            crosshairSize = readFloat(root, "crosshair_size", "crosshairSize", crosshairSize);
            crosshairThickness = readFloat(root, "crosshair_thickness", "crosshairThickness", crosshairThickness);
            antiBobRemoveViewBobbing = readBoolean(root, "anti_bob_remove_view_bobbing", "antiBobRemoveViewBobbing", antiBobRemoveViewBobbing);
            antiBobRemoveHurtShake = readBoolean(root, "anti_bob_remove_hurt_shake", "antiBobRemoveHurtShake", antiBobRemoveHurtShake);
            radarPlayers = readBoolean(root, "radar_players", "radarPlayers", radarPlayers);
            radarMonsters = readBoolean(root, "radar_monsters", "radarMonsters", radarMonsters);
            radarAnimals = readBoolean(root, "radar_animals", "radarAnimals", radarAnimals);
            radarRotateWithView = readBoolean(root, "radar_rotate_with_view", "radarRotateWithView", radarRotateWithView);
            radarMaxDistance = readFloat(root, "radar_max_distance", "radarMaxDistance", radarMaxDistance);
            radarSize = readInt(root, "radar_size", "radarSize", radarSize);
            skeletonThroughWalls = readBoolean(root, "skeleton_through_walls", "skeletonThroughWalls", skeletonThroughWalls);
            skeletonMaxDistance = readFloat(root, "skeleton_max_distance", "skeletonMaxDistance", skeletonMaxDistance);
            skeletonLineWidth = readFloat(root, "skeleton_line_width", "skeletonLineWidth", skeletonLineWidth);
            blockOutlineFilledBox = readBoolean(root, "block_outline_filled_box", "blockOutlineFilledBox", blockOutlineFilledBox);
            blockOutlineLineWidth = readFloat(root, "block_outline_line_width", "blockOutlineLineWidth", blockOutlineLineWidth);
            entityInfoShowHealth = readBoolean(root, "entity_info_show_health", "entityInfoShowHealth", entityInfoShowHealth);
            entityInfoShowDistance = readBoolean(root, "entity_info_show_distance", "entityInfoShowDistance", entityInfoShowDistance);
            entityInfoShowPosition = readBoolean(root, "entity_info_show_position", "entityInfoShowPosition", entityInfoShowPosition);
            entityInfoShowHeldItem = readBoolean(root, "entity_info_show_held_item", "entityInfoShowHeldItem", entityInfoShowHeldItem);
            entityInfoMaxDistance = readFloat(root, "entity_info_max_distance", "entityInfoMaxDistance", entityInfoMaxDistance);
            requestXrayRendererReload();
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载渲染功能配置失败", e);
        }
    }

    public static void saveConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_render.json");
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            for (FeatureState state : FEATURES.values()) {
                root.addProperty(state.id + "_enabled", state.isEnabled());
            }
            root.addProperty("brightness_soft_mode", brightnessSoftMode);
            root.addProperty("brightness_gamma", brightnessGamma);
            root.addProperty("no_fog_remove_liquid", noFogRemoveLiquid);
            root.addProperty("no_fog_brighten_color", noFogBrightenColor);
            root.addProperty("entity_visual_players", entityVisualPlayers);
            root.addProperty("entity_visual_monsters", entityVisualMonsters);
            root.addProperty("entity_visual_animals", entityVisualAnimals);
            root.addProperty("entity_visual_through_walls", entityVisualThroughWalls);
            root.addProperty("entity_visual_filled_box", entityVisualFilledBox);
            root.addProperty("entity_visual_max_distance", entityVisualMaxDistance);
            root.addProperty("tracer_players", tracerPlayers);
            root.addProperty("tracer_monsters", tracerMonsters);
            root.addProperty("tracer_animals", tracerAnimals);
            root.addProperty("tracer_through_walls", tracerThroughWalls);
            root.addProperty("tracer_max_distance", tracerMaxDistance);
            root.addProperty("tracer_line_width", tracerLineWidth);
            root.addProperty("entity_tag_players", entityTagPlayers);
            root.addProperty("entity_tag_monsters", entityTagMonsters);
            root.addProperty("entity_tag_animals", entityTagAnimals);
            root.addProperty("entity_tag_show_health", entityTagShowHealth);
            root.addProperty("entity_tag_show_distance", entityTagShowDistance);
            root.addProperty("entity_tag_show_held_item", entityTagShowHeldItem);
            root.addProperty("entity_tag_max_distance", entityTagMaxDistance);
            root.addProperty("block_highlight_storages", blockHighlightStorages);
            root.addProperty("block_highlight_spawners", blockHighlightSpawners);
            root.addProperty("block_highlight_ores", blockHighlightOres);
            root.addProperty("block_highlight_through_walls", blockHighlightThroughWalls);
            root.addProperty("block_highlight_filled_box", blockHighlightFilledBox);
            root.addProperty("block_highlight_max_distance", blockHighlightMaxDistance);
            JsonArray xrayBlocks = new JsonArray();
            for (String blockId : getXrayVisibleBlockIds()) {
                xrayBlocks.add(blockId);
            }
            root.add("xray_visible_blocks", xrayBlocks);
            root.addProperty("item_esp_show_name", itemEspShowName);
            root.addProperty("item_esp_show_distance", itemEspShowDistance);
            root.addProperty("item_esp_through_walls", itemEspThroughWalls);
            root.addProperty("item_esp_max_distance", itemEspMaxDistance);
            root.addProperty("trajectory_bows", trajectoryBows);
            root.addProperty("trajectory_pearls", trajectoryPearls);
            root.addProperty("trajectory_throwables", trajectoryThrowables);
            root.addProperty("trajectory_potions", trajectoryPotions);
            root.addProperty("trajectory_max_steps", trajectoryMaxSteps);
            root.addProperty("crosshair_dynamic_gap", crosshairDynamicGap);
            root.addProperty("crosshair_color_rgb", crosshairColorRgb);
            root.addProperty("crosshair_size", crosshairSize);
            root.addProperty("crosshair_thickness", crosshairThickness);
            root.addProperty("anti_bob_remove_view_bobbing", antiBobRemoveViewBobbing);
            root.addProperty("anti_bob_remove_hurt_shake", antiBobRemoveHurtShake);
            root.addProperty("radar_players", radarPlayers);
            root.addProperty("radar_monsters", radarMonsters);
            root.addProperty("radar_animals", radarAnimals);
            root.addProperty("radar_rotate_with_view", radarRotateWithView);
            root.addProperty("radar_max_distance", radarMaxDistance);
            root.addProperty("radar_size", radarSize);
            root.addProperty("skeleton_through_walls", skeletonThroughWalls);
            root.addProperty("skeleton_max_distance", skeletonMaxDistance);
            root.addProperty("skeleton_line_width", skeletonLineWidth);
            root.addProperty("block_outline_filled_box", blockOutlineFilledBox);
            root.addProperty("block_outline_line_width", blockOutlineLineWidth);
            root.addProperty("entity_info_show_health", entityInfoShowHealth);
            root.addProperty("entity_info_show_distance", entityInfoShowDistance);
            root.addProperty("entity_info_show_position", entityInfoShowPosition);
            root.addProperty("entity_info_show_held_item", entityInfoShowHeldItem);
            root.addProperty("entity_info_max_distance", entityInfoMaxDistance);
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存渲染功能配置失败", e);
        }
    }

    private static void register(FeatureState state) {
        FEATURES.put(state.id, state);
    }

    private static void handleRuntimeSideEffectsAfterToggle(String featureId, boolean previousEnabled, boolean enabled) {
        String normalizedId = normalizeId(featureId);
        if (previousEnabled == enabled) {
            return;
        }
        if ("xray".equals(normalizedId)) {
            requestXrayRendererReload();
            return;
        }
        if ("anti_bob".equals(normalizedId) && !enabled) {
            INSTANCE.restoreAntiBob(Minecraft.getInstance());
        }
    }

    private static void applyAllDefaultsWithoutSave() {
        brightnessSoftMode = false;
        brightnessGamma = 10.0F;
        noFogRemoveLiquid = true;
        noFogBrightenColor = true;
        entityVisualPlayers = true;
        entityVisualMonsters = true;
        entityVisualAnimals = false;
        entityVisualThroughWalls = true;
        entityVisualFilledBox = true;
        entityVisualMaxDistance = 48.0F;
        tracerPlayers = true;
        tracerMonsters = true;
        tracerAnimals = false;
        tracerThroughWalls = true;
        tracerMaxDistance = 64.0F;
        tracerLineWidth = 1.6F;
        entityTagPlayers = true;
        entityTagMonsters = true;
        entityTagAnimals = false;
        entityTagShowHealth = true;
        entityTagShowDistance = true;
        entityTagShowHeldItem = false;
        entityTagMaxDistance = 32.0F;
        blockHighlightStorages = true;
        blockHighlightSpawners = true;
        blockHighlightOres = true;
        blockHighlightThroughWalls = true;
        blockHighlightFilledBox = true;
        blockHighlightMaxDistance = 24.0F;
        resetXrayVisibleBlocksToDefaultInternal();
        itemEspShowName = true;
        itemEspShowDistance = true;
        itemEspThroughWalls = true;
        itemEspMaxDistance = 24.0F;
        trajectoryBows = true;
        trajectoryPearls = true;
        trajectoryThrowables = true;
        trajectoryPotions = true;
        trajectoryMaxSteps = 120;
        crosshairDynamicGap = true;
        crosshairColorRgb = 0x55FFFF;
        crosshairSize = 6.0F;
        crosshairThickness = 2.0F;
        antiBobRemoveViewBobbing = true;
        antiBobRemoveHurtShake = true;
        radarPlayers = true;
        radarMonsters = true;
        radarAnimals = false;
        radarRotateWithView = true;
        radarMaxDistance = 48.0F;
        radarSize = 90;
        skeletonThroughWalls = true;
        skeletonMaxDistance = 48.0F;
        skeletonLineWidth = 1.7F;
        blockOutlineFilledBox = false;
        blockOutlineLineWidth = 2.0F;
        entityInfoShowHealth = true;
        entityInfoShowDistance = true;
        entityInfoShowPosition = true;
        entityInfoShowHeldItem = true;
        entityInfoMaxDistance = 48.0F;
    }

    private void resetRuntimeState() {
        Minecraft mc = Minecraft.getInstance();
        restoreAntiBob(mc);
    }

    public static boolean isBrightnessOverrideActive() {
        return isEnabled("brightness_boost") || isEnabled("xray");
    }

    public static float getEffectiveBrightnessGammaOverride() {
        boolean brightnessEnabled = isEnabled("brightness_boost");
        boolean xrayEnabled = isEnabled("xray");
        float targetGamma = 1.0F;
        if (brightnessEnabled) {
            targetGamma = brightnessSoftMode ? Math.max(4.0F, brightnessGamma * 0.55F) : Math.max(1.0F, brightnessGamma);
        }
        if (xrayEnabled) {
            targetGamma = Math.max(targetGamma, 16.0F);
        }
        return targetGamma;
    }

    private void applyAntiBob(Minecraft mc) {
        if (!isEnabled("anti_bob") || !antiBobRemoveViewBobbing) {
            restoreAntiBob(mc);
            return;
        }
        if (mc == null || mc.options == null) {
            return;
        }
        if (!antiBobApplied) {
            previousViewBobbing = mc.options.bobView().get();
            antiBobApplied = true;
        }
        mc.options.bobView().set(false);
    }

    private void restoreAntiBob(Minecraft mc) {
        if (!antiBobApplied || mc == null || mc.options == null) {
            return;
        }
        mc.options.bobView().set(previousViewBobbing);
        antiBobApplied = false;
    }

    private static boolean readFeatureEnabled(JsonObject root, String featureId) {
        if (root == null) {
            return false;
        }
        String flatKey = normalizeId(featureId) + "_enabled";
        if (root.has(flatKey)) {
            return root.get(flatKey).getAsBoolean();
        }
        if (root.has(featureId) && root.get(featureId).isJsonObject()) {
            JsonObject feature = root.getAsJsonObject(featureId);
            if (feature.has("enabled")) {
                return feature.get("enabled").getAsBoolean();
            }
        }
        return false;
    }

    private static boolean readBoolean(JsonObject root, String legacyKey, String camelKey, boolean fallback) {
        if (root.has(legacyKey)) {
            return root.get(legacyKey).getAsBoolean();
        }
        if (root.has(camelKey)) {
            return root.get(camelKey).getAsBoolean();
        }
        return fallback;
    }

    private static float readFloat(JsonObject root, String legacyKey, String camelKey, float fallback) {
        if (root.has(legacyKey)) {
            return root.get(legacyKey).getAsFloat();
        }
        if (root.has(camelKey)) {
            return root.get(camelKey).getAsFloat();
        }
        return fallback;
    }

    private static int readInt(JsonObject root, String legacyKey, String camelKey, int fallback) {
        if (root.has(legacyKey)) {
            return root.get(legacyKey).getAsInt();
        }
        if (root.has(camelKey)) {
            return root.get(camelKey).getAsInt();
        }
        return fallback;
    }

    private static void loadXrayVisibleBlocks(JsonObject root) {
        resetXrayVisibleBlocksToDefaultInternal();
        JsonArray blocks = null;
        if (root.has("xray_visible_blocks") && root.get("xray_visible_blocks").isJsonArray()) {
            blocks = root.getAsJsonArray("xray_visible_blocks");
        } else if (root.has("xrayVisibleBlocks") && root.get("xrayVisibleBlocks").isJsonArray()) {
            blocks = root.getAsJsonArray("xrayVisibleBlocks");
        }
        if (blocks == null) {
            return;
        }
        XRAY_VISIBLE_BLOCK_IDS.clear();
        for (JsonElement element : blocks) {
            if (element == null || !element.isJsonPrimitive()) {
                continue;
            }
            String blockId = normalizeXrayBlockId(element.getAsString());
            if (!blockId.isEmpty()) {
                XRAY_VISIBLE_BLOCK_IDS.add(blockId);
            }
        }
        if (XRAY_VISIBLE_BLOCK_IDS.isEmpty()) {
            resetXrayVisibleBlocksToDefaultInternal();
        }
    }

    private static void resetXrayVisibleBlocksToDefaultInternal() {
        XRAY_VISIBLE_BLOCK_IDS.clear();
        Arrays.stream(DEFAULT_XRAY_BLOCK_IDS)
                .map(RenderFeatureManager::normalizeXrayBlockId)
                .filter(id -> !id.isEmpty())
                .forEach(XRAY_VISIBLE_BLOCK_IDS::add);
    }

    private static void requestXrayRendererReloadIfEnabled() {
        if (isEnabled("xray")) {
            requestXrayRendererReload();
        }
    }

    private static List<String> sortBlockIds(Set<String> blockIds) {
        List<String> sorted = new ArrayList<>(blockIds);
        Collections.sort(sorted);
        return sorted;
    }

    private static boolean isCrosshairOverlay(RenderGuiOverlayEvent event) {
        String path = overlayPath(event.getOverlay() == null ? null : event.getOverlay().id());
        return "crosshair".equals(path) || "crosshairs".equals(path);
    }

    private static String overlayPath(ResourceLocation id) {
        return id == null ? "" : safe(id.getPath()).toLowerCase(Locale.ROOT);
    }

    private static String normalizeBlockId(Block block) {
        if (block == null || block == Blocks.AIR) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            return "";
        }
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static String normalizeId(String featureId) {
        return safe(featureId).toLowerCase(Locale.ROOT);
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
