package com.zszl.zszlScriptMod.otherfeatures.handler.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BlockFeatureManager {

    public static final BlockFeatureManager INSTANCE = new BlockFeatureManager();

    private static final Map<String, FeatureState> FEATURES = new LinkedHashMap<>();
    private static final Direction[] PLACE_SEARCH_ORDER = {
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
    };

    private static Field rightClickDelayField;
    private static Field destroyDelayField;

    private int autoToolCooldownTicks;
    private int placeAssistCooldownTicks;
    private int blockRefillCooldownTicks;
    private int blockSwapLockTicks;
    private int lastAutoToolHotbarSlot = -1;
    private int lockedHotbarSlot = -1;
    private int autoLightCooldownTicks;
    private BlockPos lastAssistPlacePos;
    private BlockPos lastTorchPlacePos;
    private int lastRefillSlot = -1;
    private int ghostHandCooldownTicks;
    private BlockPos lastGhostInteractPos;
    private int surroundCooldownTicks;
    private final List<BlockPos> surroundPositions = new ArrayList<>();

    static {
        register(new FeatureState("auto_tool", "自动切工具", "挖方块时自动切到热栏中更适合当前方块的工具。", "", 0.0F, 0.0F, 0.0F, true));
        register(new FeatureState("fast_place", "快速放置", "缩短右键放置延迟。", "放置延迟", 1.0F, 0.0F, 4.0F, true));
        register(new FeatureState("place_assist", "精准放置辅助", "尝试补一个合法支撑面与朝向。", "搜索半径", 1.0F, 1.0F, 3.0F, true));
        register(new FeatureState("fast_break", "基础快速挖掘", "降低客户端挖掘打击延迟。", "挖掘延迟", 1.0F, 0.0F, 4.0F, true));
        register(new FeatureState("block_swap_lock", "方块热栏锁定", "放置建筑方块时短暂锁定当前热栏槽位。", "", 0.0F, 0.0F, 0.0F, true));
        register(new FeatureState("auto_light", "自动补光", "自动在周围补火把。", "光照阈值", 7.0F, 0.0F, 15.0F, true));
        register(new FeatureState("block_refill", "方块自动补栏", "建筑方块不足时自动补到热栏。", "补充阈值", 16.0F, 1.0F, 64.0F, true));
        register(new FeatureState("ghost_hand_block", "穿墙方块交互", "尝试对视线方向更深处的方块交互。", "交互距离", 6.0F, 4.5F, 10.0F, true));
        register(new FeatureState("surround", "自动围身", "自动在脚边放方块保护自身。", "", 0.0F, 0.0F, 0.0F, true));
        loadConfig();
    }

    private BlockFeatureManager() {
    }

    public static final class FeatureState {
        public final String id;
        public final String name;
        public final String description;
        public final String valueLabel;
        public final float defaultValue;
        public final float minValue;
        public final float maxValue;
        public final boolean behaviorImplemented;

        private boolean enabled;
        private float value;
        private boolean statusHudEnabled = true;

        private FeatureState(String id, String name, String description, String valueLabel,
                float defaultValue, float minValue, float maxValue, boolean behaviorImplemented) {
            this.id = safe(id);
            this.name = safe(name);
            this.description = safe(description);
            this.valueLabel = valueLabel == null ? "" : valueLabel.trim();
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.behaviorImplemented = behaviorImplemented;
            this.value = defaultValue;
        }

        public boolean supportsValue() {
            return !valueLabel.isEmpty() && maxValue > minValue;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public float getValue() {
            return value;
        }

        public boolean isStatusHudEnabled() {
            return statusHudEnabled;
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
        return state != null && state.enabled;
    }

    public static void toggleFeature(String featureId) {
        setEnabled(featureId, !isEnabled(featureId));
    }

    public static void setEnabled(String featureId, boolean enabled) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.enabled = enabled;
        saveConfig();
    }

    public static void setValue(String featureId, float value) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.value = clamp(value, state.minValue, state.maxValue);
        saveConfig();
    }

    public static void setFeatureStatusHudEnabled(String featureId, boolean enabled) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.statusHudEnabled = enabled;
        saveConfig();
    }

    public static void resetFeature(String featureId) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.enabled = false;
        state.value = state.defaultValue;
        state.statusHudEnabled = true;
        saveConfig();
    }

    public void tick(Minecraft mc) {
        runClientTick(mc);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            runPreClientTick(mc);
            return;
        }
        if (event.phase == TickEvent.Phase.END) {
            runClientTick(mc);
        }
    }

    private void runPreClientTick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        if (mc.player.isDeadOrDying() || mc.player.getHealth() <= 0.0F) {
            return;
        }
        handleFastBreak(mc);
        handleFastPlace(mc);
    }

    private void runClientTick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            resetRuntimeState();
            return;
        }
        if (mc.player.isDeadOrDying() || mc.player.getHealth() <= 0.0F) {
            resetRuntimeState();
            return;
        }

        tickCooldowns();
        handleBlockSwapLock(mc.player);
        handleAutoTool(mc);
        handlePlaceAssist(mc);
        handleAutoLight(mc);
        handleBlockRefill(mc);
        handleGhostHandBlock(mc);
        handleSurround(mc);
    }

    public static String getFeatureRuntimeSummary(String featureId) {
        return INSTANCE.buildFeatureRuntimeSummary(normalizeId(featureId));
    }

    public static List<String> getStatusLines() {
        return getStatusLines(false);
    }

    public static List<String> getStatusLines(boolean forcePreview) {
        if (!forcePreview && !MovementFeatureManager.isMasterStatusHudEnabled()) {
            return new ArrayList<>();
        }

        List<String> activeNames = new ArrayList<>();
        for (FeatureState state : FEATURES.values()) {
            if (state != null && state.enabled && state.statusHudEnabled) {
                activeNames.add(state.name);
            }
        }

        List<String> lines = new ArrayList<>();
        if (activeNames.isEmpty()) {
            return lines;
        }

        lines.add("§a[方块交互] §f" + activeNames.size() + " 项开启");
        StringBuilder builder = new StringBuilder("§7");
        for (int i = 0; i < activeNames.size() && i < 4; i++) {
            if (i > 0) {
                builder.append("、");
            }
            builder.append(activeNames.get(i));
        }
        if (activeNames.size() > 4) {
            builder.append(" §8+").append(activeNames.size() - 4);
        }
        lines.add(builder.toString());

        String runtime = INSTANCE.getRuntimeHudLine();
        if (!runtime.isEmpty()) {
            lines.add(runtime);
        }
        return lines;
    }

    public static String getFeatureRuntimeSummaryLegacy(String featureId) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return "未注册";
        }
        if (!state.enabled) {
            return "未启用";
        }
        return state.supportsValue() ? "已启用 / " + formatFloat(state.value) : "已启用";
    }

    public static void loadConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_block.json");
            if (!Files.exists(file)) {
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject features = root.has("features") && root.get("features").isJsonObject()
                    ? root.getAsJsonObject("features")
                    : root;
            for (FeatureState state : FEATURES.values()) {
                if (!features.has(state.id) || !features.get(state.id).isJsonObject()) {
                    continue;
                }
                JsonObject json = features.getAsJsonObject(state.id);
                if (json.has("enabled")) {
                    state.enabled = json.get("enabled").getAsBoolean();
                }
                if (json.has("value")) {
                    state.value = clamp(json.get("value").getAsFloat(), state.minValue, state.maxValue);
                }
                if (json.has("statusHudEnabled")) {
                    state.statusHudEnabled = json.get("statusHudEnabled").getAsBoolean();
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载方块功能配置失败", e);
        }
    }

    public static void saveConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_block.json");
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            JsonObject features = new JsonObject();
            for (FeatureState state : FEATURES.values()) {
                JsonObject json = new JsonObject();
                json.addProperty("enabled", state.enabled);
                json.addProperty("value", state.value);
                json.addProperty("statusHudEnabled", state.statusHudEnabled);
                features.add(state.id, json);
            }
            root.add("features", features);
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存方块功能配置失败", e);
        }
    }

    public void onClientDisconnect() {
        resetRuntimeState();
    }

    private void tickCooldowns() {
        if (autoToolCooldownTicks > 0) {
            autoToolCooldownTicks--;
        }
        if (placeAssistCooldownTicks > 0) {
            placeAssistCooldownTicks--;
        }
        if (blockRefillCooldownTicks > 0) {
            blockRefillCooldownTicks--;
        }
        if (blockSwapLockTicks > 0) {
            blockSwapLockTicks--;
            if (blockSwapLockTicks == 0) {
                lockedHotbarSlot = -1;
            }
        }
        if (autoLightCooldownTicks > 0) {
            autoLightCooldownTicks--;
        }
        if (ghostHandCooldownTicks > 0) {
            ghostHandCooldownTicks--;
        }
        if (surroundCooldownTicks > 0) {
            surroundCooldownTicks--;
        }
    }

    private void handleAutoTool(Minecraft mc) {
        if (!isEnabled("auto_tool")
                || autoToolCooldownTicks > 0
                || mc.screen != null
                || mc.hitResult == null
                || mc.hitResult.getType() != HitResult.Type.BLOCK
                || !mc.options.keyAttack.isDown()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        if (lockedHotbarSlot >= 0 && blockSwapLockTicks > 0) {
            return;
        }

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state == null || state.getDestroySpeed(mc.level, pos) < 0.0F || state.getFluidState().isSource()) {
            return;
        }

        int currentSlot = player.getInventory().selected;
        int bestSlot = findBestToolHotbarSlot(player, state);
        if (bestSlot < 0 || bestSlot == currentSlot) {
            return;
        }

        double currentScore = getToolScore(player.getInventory().getItem(currentSlot), state);
        double bestScore = getToolScore(player.getInventory().getItem(bestSlot), state);
        if (bestScore <= Math.max(1.0D, currentScore + 0.15D)) {
            return;
        }

        if (bestSlot >= 0 && bestSlot != currentSlot) {
            ModUtils.switchToHotbarSlot(bestSlot + 1);
            autoToolCooldownTicks = 2;
            lastAutoToolHotbarSlot = bestSlot;
        }
    }

    private void handleFastPlace(Minecraft mc) {
        if (!isEnabled("fast_place")
                || mc.screen != null
                || mc.player == null
                || !isHoldingPlaceableBlock(mc.player)) {
            return;
        }
        Field field = resolveRightClickDelayField();
        if (field == null) {
            return;
        }
        try {
            int current = field.getInt(mc);
            field.setInt(mc, Math.min(current, Math.round(getConfiguredValue("fast_place", 1.0F))));
        } catch (Exception ignored) {
        }
    }

    private void handleFastBreak(Minecraft mc) {
        if (!isEnabled("fast_break") || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK || !mc.options.keyAttack.isDown()) {
            return;
        }
        Field field = resolveDestroyDelayField();
        if (field == null) {
            return;
        }
        try {
            int current = field.getInt(mc.gameMode);
            field.setInt(mc.gameMode, Math.min(current, Math.round(getConfiguredValue("fast_break", 1.0F))));
        } catch (Exception ignored) {
        }
    }

    private void handleBlockSwapLock(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled("block_swap_lock")) {
            blockSwapLockTicks = 0;
            lockedHotbarSlot = -1;
            return;
        }
        if (player == null || mc == null || mc.screen != null) {
            return;
        }
        if (mc.options.keyUse.isDown() && isHoldingPlaceableBlock(player)) {
            lockedHotbarSlot = player.getInventory().selected;
            blockSwapLockTicks = 8;
        }
    }

    private void handleBlockRefill(Minecraft mc) {
        if (!isEnabled("block_refill") || blockRefillCooldownTicks > 0) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || player.containerMenu != player.inventoryMenu) {
            return;
        }

        int hotbarSlot = player.getInventory().selected;
        ItemStack selected = player.getInventory().getItem(hotbarSlot);
        if (!(selected.getItem() instanceof BlockItem) || selected.getCount() > getConfiguredValue("block_refill", 16.0F)) {
            return;
        }

        for (int invIndex = 9; invIndex < player.getInventory().items.size(); invIndex++) {
            ItemStack candidate = player.getInventory().items.get(invIndex);
            if (candidate.isEmpty() || !ItemStack.isSameItemSameTags(selected, candidate)) {
                continue;
            }
            mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, inventoryIndexToMenuSlot(invIndex),
                    hotbarSlot, ClickType.SWAP, player);
            blockRefillCooldownTicks = 6;
            lastRefillSlot = hotbarSlot;
            return;
        }
    }

    private void handlePlaceAssist(Minecraft mc) {
        if (!isEnabled("place_assist")
                || placeAssistCooldownTicks > 0
                || mc.player == null
                || !isHoldingPlaceableBlock(mc.player)
                || !mc.options.keyUse.isDown()
                || mc.screen != null) {
            return;
        }
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            return;
        }
        if (isVanillaPlacementLikelyEnough(mc, mc.hitResult)) {
            return;
        }

        int radius = Math.max(1, Math.round(getConfiguredValue("place_assist", 1.0F)));
        PlacementTarget target = findAssistPlacement(mc, mc.hitResult, radius);
        if (target == null) {
            return;
        }

        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(target.hitVec, target.supportFace, target.supportPos, false));
        if (result.consumesAction()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            placeAssistCooldownTicks = 2;
            lastAssistPlacePos = target.placePos;
            if (isEnabled("block_swap_lock")) {
                lockedHotbarSlot = mc.player.getInventory().selected;
                blockSwapLockTicks = 10;
            }
            if (isEnabled("fast_place")) {
                Field field = resolveRightClickDelayField();
                if (field != null) {
                    try {
                        int current = field.getInt(mc);
                        field.setInt(mc, Math.min(current, Math.round(getConfiguredValue("fast_place", 1.0F))));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void handleAutoLight(Minecraft mc) {
        if (!isEnabled("auto_light") || autoLightCooldownTicks > 0 || mc.player == null) {
            return;
        }
        BlockPos center = mc.player.blockPosition();
        if (mc.level.getMaxLocalRawBrightness(center) > getConfiguredValue("auto_light", 7.0F)) {
            return;
        }

        int torchSlot = findTorchHotbarSlot(mc.player);
        if (torchSlot < 0) {
            return;
        }

        BlockPos[] targets = {
                center,
                center.north(),
                center.south(),
                center.east(),
                center.west()
        };
        for (BlockPos target : targets) {
            if (mc.level.getBlockState(target).canBeReplaced() && tryPlaceAt(mc, target, torchSlot)) {
                autoLightCooldownTicks = 10;
                lastTorchPlacePos = target.immutable();
                return;
            }
        }
    }

    private void handleGhostHandBlock(Minecraft mc) {
        if (!isEnabled("ghost_hand_block") || ghostHandCooldownTicks > 0 || mc.player == null || mc.screen != null) {
            return;
        }
        if (!mc.options.keyUse.isDown() && !mc.options.keyAttack.isDown()) {
            return;
        }

        double maxDistance = Math.max(4.5D, getConfiguredValue("ghost_hand_block", 6.0F));
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        for (double step = 1.5D; step <= maxDistance; step += 0.5D) {
            BlockPos pos = BlockPos.containing(eye.add(look.scale(step)));
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            if (mc.options.keyAttack.isDown()) {
                mc.gameMode.startDestroyBlock(pos, Direction.UP);
            } else {
                for (Direction face : PLACE_SEARCH_ORDER) {
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
                    InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                    if (result.consumesAction()) {
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        break;
                    }
                }
            }
            ghostHandCooldownTicks = 4;
            lastGhostInteractPos = pos.immutable();
            return;
        }
    }

    private void handleSurround(Minecraft mc) {
        if (!isEnabled("surround") || surroundCooldownTicks > 0 || mc.player == null || !mc.player.onGround()) {
            return;
        }
        int blockSlot = findSolidBlockHotbarSlot(mc.player);
        if (blockSlot < 0) {
            return;
        }
        BlockPos feet = mc.player.blockPosition();
        BlockPos[] targets = { feet.north(), feet.south(), feet.east(), feet.west() };
        for (BlockPos target : targets) {
            if (mc.level.getBlockState(target).canBeReplaced() && tryPlaceAt(mc, target, blockSlot)) {
                surroundCooldownTicks = 2;
                surroundPositions.add(target.immutable());
                return;
            }
        }
    }

    private int findBestToolHotbarSlot(LocalPlayer player, BlockState state) {
        int bestSlot = -1;
        double bestScore = 0.0D;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            double score = getToolScore(stack, state);
            if (score > bestScore + 1.0E-4D) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private boolean tryPlaceAt(Minecraft mc, BlockPos targetPos) {
        return tryPlaceAt(mc, targetPos, mc.player.getInventory().selected);
    }

    private boolean tryPlaceAt(Minecraft mc, BlockPos targetPos, int hotbarSlot) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return false;
        }
        int originalSlot = player.getInventory().selected;
        if (originalSlot != hotbarSlot && !ModUtils.switchToHotbarSlot(hotbarSlot + 1)) {
            return false;
        }

        try {
            for (Direction direction : PLACE_SEARCH_ORDER) {
                BlockPos support = targetPos.relative(direction);
                BlockState supportState = mc.level.getBlockState(support);
                if (supportState.isAir() || supportState.canBeReplaced() || supportState.getCollisionShape(mc.level, support).isEmpty()) {
                    continue;
                }
                Direction face = direction.getOpposite();
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support), face, support, false);
                InteractionResult result = mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                if (result.consumesAction()) {
                    player.swing(InteractionHand.MAIN_HAND);
                    return true;
                }
            }
            return false;
        } finally {
            if (originalSlot != hotbarSlot) {
                ModUtils.switchToHotbarSlot(originalSlot + 1);
            }
        }
    }

    private int findTorchHotbarSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            Block block = blockItem.getBlock();
            if (block instanceof TorchBlock || block instanceof WallTorchBlock) {
                return i;
            }
        }
        return -1;
    }

    private int findSolidBlockHotbarSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            BlockState state = blockItem.getBlock().defaultBlockState();
            if (!state.isAir()
                    && !state.canBeReplaced()
                    && !state.hasBlockEntity()
                    && !(blockItem.getBlock() instanceof FallingBlock)
                    && state.isCollisionShapeFullBlock(player.level(), player.blockPosition())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isHoldingPlaceableBlock(LocalPlayer player) {
        if (player == null) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        return block != null && !block.defaultBlockState().isAir() && !block.defaultBlockState().liquid();
    }

    private double getToolScore(ItemStack stack, BlockState state) {
        if (stack == null || stack.isEmpty() || state == null) {
            return 0.0D;
        }

        float destroySpeed = stack.getDestroySpeed(state);
        boolean canHarvest = stack.isCorrectToolForDrops(state);
        if (destroySpeed <= 1.0F && !canHarvest) {
            return 0.0D;
        }

        double score = destroySpeed;
        if (canHarvest) {
            score += 32.0D;
        }
        if (stack.getItem() instanceof BlockItem) {
            score -= 4.0D;
        }
        return score;
    }

    private boolean isVanillaPlacementLikelyEnough(Minecraft mc, HitResult hit) {
        if (!(hit instanceof BlockHitResult blockHitResult)) {
            return false;
        }
        BlockPos hitPos = blockHitResult.getBlockPos();
        if (hitPos == null) {
            return false;
        }
        BlockState hitState = mc.level.getBlockState(hitPos);
        BlockPos primary = hitState.canBeReplaced() ? hitPos : hitPos.relative(blockHitResult.getDirection());
        return isReplaceable(mc.level, primary) && canPlaceAt(mc, primary);
    }

    private PlacementTarget findAssistPlacement(Minecraft mc, HitResult hit, int searchRadius) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 referencePoint;
        BlockPos anchor;

        if (hit instanceof BlockHitResult blockHitResult) {
            anchor = resolvePrimaryPlacePos(mc.level, blockHitResult);
            referencePoint = blockHitResult.getLocation();
        } else {
            referencePoint = eyePos.add(mc.player.getLookAngle().scale(4.0D));
            anchor = BlockPos.containing(referencePoint);
        }
        if (anchor == null) {
            anchor = BlockPos.containing(referencePoint);
        }

        PlacementTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos candidate = anchor.offset(dx, dy, dz);
                    if (!isReplaceable(mc.level, candidate)) {
                        continue;
                    }
                    PlacementTarget target = buildPlacementTarget(mc.player, candidate);
                    if (target == null) {
                        continue;
                    }
                    double score = referencePoint.distanceToSqr(Vec3.atCenterOf(candidate));
                    if (best == null || score < bestScore) {
                        best = target;
                        bestScore = score;
                    }
                }
            }
        }
        return best;
    }

    private PlacementTarget buildPlacementTarget(LocalPlayer player, BlockPos placePos) {
        if (player == null || player.level() == null || placePos == null || !isReplaceable(player.level(), placePos)) {
            return null;
        }

        Vec3 eyePos = player.getEyePosition();
        for (Direction facing : PLACE_SEARCH_ORDER) {
            BlockPos supportPos = placePos.relative(facing);
            if (!canPlaceAgainst(player.level(), supportPos)) {
                continue;
            }

            Direction supportFace = facing.getOpposite();
            Vec3 hitVec = new Vec3(
                    supportPos.getX() + 0.5D + supportFace.getStepX() * 0.5D,
                    supportPos.getY() + 0.5D + supportFace.getStepY() * 0.5D,
                    supportPos.getZ() + 0.5D + supportFace.getStepZ() * 0.5D);

            if (eyePos.distanceToSqr(hitVec) > 36.0D) {
                continue;
            }

            HitResult rayTrace = player.level().clip(new net.minecraft.world.level.ClipContext(
                    eyePos, hitVec,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    player));
            if (rayTrace instanceof BlockHitResult blockHitResult) {
                BlockPos hitPos = blockHitResult.getBlockPos();
                if (!supportPos.equals(hitPos) && !placePos.equals(hitPos)) {
                    continue;
                }
            }

            return new PlacementTarget(placePos, supportPos, supportFace, hitVec);
        }

        return null;
    }

    private BlockPos resolvePrimaryPlacePos(Level level, BlockHitResult hit) {
        if (level == null || hit == null || hit.getBlockPos() == null || hit.getDirection() == null) {
            return null;
        }
        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        if (hitState.canBeReplaced()) {
            return hitPos;
        }
        return hitPos.relative(hit.getDirection());
    }

    private boolean canPlaceAgainst(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && !state.canBeReplaced()
                && !state.hasBlockEntity()
                && !state.getCollisionShape(level, pos).isEmpty();
    }

    private boolean isReplaceable(Level level, BlockPos pos) {
        return level != null && pos != null && level.getBlockState(pos).canBeReplaced();
    }

    private boolean canPlaceAt(Minecraft mc, BlockPos targetPos) {
        for (Direction direction : PLACE_SEARCH_ORDER) {
            BlockPos support = targetPos.relative(direction);
            BlockState supportState = mc.level.getBlockState(support);
            if (supportState.isAir() || supportState.canBeReplaced() || supportState.getCollisionShape(mc.level, support).isEmpty()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private Field resolveRightClickDelayField() {
        if (rightClickDelayField != null) {
            return rightClickDelayField;
        }
        try {
            rightClickDelayField = Minecraft.class.getDeclaredField("rightClickDelay");
            rightClickDelayField.setAccessible(true);
            return rightClickDelayField;
        } catch (Exception ignored) {
        }
        try {
            rightClickDelayField = Minecraft.class.getDeclaredField("f_91077_");
            rightClickDelayField.setAccessible(true);
            return rightClickDelayField;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Field resolveDestroyDelayField() {
        if (destroyDelayField != null) {
            return destroyDelayField;
        }
        try {
            destroyDelayField = MultiPlayerGameMode.class.getDeclaredField("destroyDelay");
            destroyDelayField.setAccessible(true);
            return destroyDelayField;
        } catch (Exception ignored) {
        }
        try {
            destroyDelayField = MultiPlayerGameMode.class.getDeclaredField("f_105189_");
            destroyDelayField.setAccessible(true);
            return destroyDelayField;
        } catch (Exception ignored) {
            return null;
        }
    }

    private float getConfiguredValue(String featureId, float fallback) {
        FeatureState state = getFeature(featureId);
        return state == null ? fallback : clamp(state.value, state.minValue, state.maxValue);
    }

    private int inventoryIndexToMenuSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }

    private void resetRuntimeState() {
        autoToolCooldownTicks = 0;
        placeAssistCooldownTicks = 0;
        blockRefillCooldownTicks = 0;
        blockSwapLockTicks = 0;
        lastAutoToolHotbarSlot = -1;
        lockedHotbarSlot = -1;
        autoLightCooldownTicks = 0;
        lastAssistPlacePos = null;
        lastTorchPlacePos = null;
        lastRefillSlot = -1;
        ghostHandCooldownTicks = 0;
        lastGhostInteractPos = null;
        surroundCooldownTicks = 0;
        surroundPositions.clear();
    }

    private static final class PlacementTarget {
        private final BlockPos placePos;
        private final BlockPos supportPos;
        private final Direction supportFace;
        private final Vec3 hitVec;

        private PlacementTarget(BlockPos placePos, BlockPos supportPos, Direction supportFace, Vec3 hitVec) {
            this.placePos = placePos;
            this.supportPos = supportPos;
            this.supportFace = supportFace;
            this.hitVec = hitVec;
        }
    }

    private String getRuntimeHudLine() {
        List<String> parts = new ArrayList<>();
        if (isEnabled("auto_tool") && getFeature("auto_tool").statusHudEnabled && lastAutoToolHotbarSlot >= 0
                && autoToolCooldownTicks > 0) {
            parts.add("§e切到槽 " + (lastAutoToolHotbarSlot + 1));
        }
        if (isEnabled("place_assist") && getFeature("place_assist").statusHudEnabled && placeAssistCooldownTicks > 0
                && lastAssistPlacePos != null) {
            parts.add("§b辅助放置");
        }
        if (isEnabled("block_swap_lock") && getFeature("block_swap_lock").statusHudEnabled && blockSwapLockTicks > 0
                && lockedHotbarSlot >= 0) {
            parts.add("§a建筑锁 " + (lockedHotbarSlot + 1));
        }
        if (isEnabled("auto_light") && getFeature("auto_light").statusHudEnabled && autoLightCooldownTicks > 0
                && lastTorchPlacePos != null) {
            parts.add("§6补光");
        }
        if (isEnabled("block_refill") && getFeature("block_refill").statusHudEnabled && blockRefillCooldownTicks > 0
                && lastRefillSlot >= 0) {
            parts.add("§d补栏 " + (lastRefillSlot + 1));
        }
        if (isEnabled("ghost_hand_block") && getFeature("ghost_hand_block").statusHudEnabled && ghostHandCooldownTicks > 0
                && lastGhostInteractPos != null) {
            parts.add("§c穿墙交互");
        }
        if (isEnabled("surround") && getFeature("surround").statusHudEnabled && !surroundPositions.isEmpty()) {
            parts.add("§9围身 " + surroundPositions.size());
        }
        if (parts.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("§7");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append("  ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    private String buildFeatureRuntimeSummary(String featureId) {
        if (featureId == null || featureId.isEmpty()) {
            return "待机";
        }

        switch (featureId) {
        case "auto_tool":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            return lastAutoToolHotbarSlot >= 0 && autoToolCooldownTicks > 0
                    ? "最近切换到热栏 " + (lastAutoToolHotbarSlot + 1)
                    : "挖方块时自动切换到更合适的工具";
        case "fast_place":
            return isEnabled(featureId)
                    ? "方块放置延迟上限 " + Math.round(getConfiguredValue(featureId, 1.0F)) + " tick"
                    : "未启用";
        case "place_assist":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            return placeAssistCooldownTicks > 0 && lastAssistPlacePos != null
                    ? "刚辅助放置到 " + formatBlockPos(lastAssistPlacePos)
                    : "待机，点空气或边缘失败时尝试补合法支撑面";
        case "fast_break":
            return isEnabled(featureId)
                    ? "挖掘延迟上限 " + Math.round(getConfiguredValue(featureId, 1.0F)) + " tick"
                    : "未启用";
        case "block_swap_lock":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            return blockSwapLockTicks > 0 && lockedHotbarSlot >= 0
                    ? "当前锁定热栏 " + (lockedHotbarSlot + 1) + "，剩余 " + blockSwapLockTicks + " tick"
                    : "待机，放置建筑方块时会短暂保持当前热栏";
        case "auto_light":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            return autoLightCooldownTicks > 0 && lastTorchPlacePos != null
                    ? "刚在 " + formatBlockPos(lastTorchPlacePos) + " 放置火把"
                    : "待机，光照低于 " + Math.round(getConfiguredValue(featureId, 7.0F)) + " 时自动放火把";
        case "block_refill":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            return blockRefillCooldownTicks > 0 && lastRefillSlot >= 0
                    ? "刚补充热栏 " + (lastRefillSlot + 1)
                    : "待机，方块少于 " + Math.round(getConfiguredValue(featureId, 16.0F)) + " 时自动补充";
        case "ghost_hand_block":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            return ghostHandCooldownTicks > 0 && lastGhostInteractPos != null
                    ? "刚与 " + formatBlockPos(lastGhostInteractPos) + " 交互"
                    : "待机，可在 " + formatFloat(getConfiguredValue(featureId, 6.0F)) + " 格距离内穿墙交互";
        case "surround":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            return !surroundPositions.isEmpty() ? "已放置 " + surroundPositions.size() + " 个保护方块" : "待机，自动在脚边放置保护方块";
        default:
            FeatureState state = getFeature(featureId);
            if (state == null) {
                return "未找到功能";
            }
            return state.enabled ? "已启用" : "未启用";
        }
    }

    private static void register(FeatureState state) {
        FEATURES.put(state.id, state);
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static String normalizeId(String featureId) {
        return safe(featureId).toLowerCase(Locale.ROOT);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatBlockPos(BlockPos pos) {
        if (pos == null) {
            return "unknown";
        }
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
