package com.zszl.zszlScriptMod.otherfeatures.handler.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zszl.zszlScriptMod.compat.render.WorldGizmoRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RenderFeatureSupport {

    private static final int MAX_BLOCK_SCAN_RESULTS = 384;
    private static final int BLOCK_SCAN_INTERVAL_TICKS = 10;
    private static final int ORE_SCAN_VERTICAL_RADIUS_LIMIT = 24;
    private static final List<BlockHighlightEntry> BLOCK_HIGHLIGHTS = new ArrayList<>();
    private static RenderFrameData cachedRenderFrameData = null;
    private static int cachedRenderFrameTick = Integer.MIN_VALUE;
    private static int blockScanCooldown = 0;

    private RenderFeatureSupport() {
    }

    static void clearRuntimeCaches() {
        BLOCK_HIGHLIGHTS.clear();
        cachedRenderFrameData = null;
        cachedRenderFrameTick = Integer.MIN_VALUE;
        blockScanCooldown = 0;
    }

    static void onClientTick(Minecraft mc, LocalPlayer player) {
        cachedRenderFrameData = null;
        cachedRenderFrameTick = Integer.MIN_VALUE;
        if (blockScanCooldown > 0) {
            blockScanCooldown--;
        }
        if (mc == null || player == null || mc.level == null) {
            BLOCK_HIGHLIGHTS.clear();
            return;
        }

        if ((RenderFeatureManager.isEnabled("block_highlight") || RenderFeatureManager.isEnabled("xray"))
                && blockScanCooldown <= 0) {
            updateBlockHighlights(mc, player);
            blockScanCooldown = BLOCK_SCAN_INTERVAL_TICKS;
        } else if (!RenderFeatureManager.isEnabled("block_highlight") && !RenderFeatureManager.isEnabled("xray")) {
            BLOCK_HIGHLIGHTS.clear();
        }
    }

    static void renderWorld(Minecraft mc, LevelRenderState state) {
        if (mc == null || mc.level == null || mc.player == null || state == null) {
            return;
        }

        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        RenderFrameData frameData = getRenderFrameData(mc);
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) {
            return;
        }
        RenderWorldContext renderContext = RenderWorldContext.create(camera);
        PoseStack geometryPoseStack = renderContext.createBasePoseStack();
        PoseStack linePoseStack = renderContext.createBasePoseStack();

        if (RenderFeatureManager.isEnabled("entity_visual")) {
            renderEntityVisuals(mc, renderContext, geometryPoseStack, frameData, partialTicks);
        }
        if (RenderFeatureManager.isEnabled("block_highlight") || RenderFeatureManager.isEnabled("xray")) {
            renderBlockHighlights(renderContext, geometryPoseStack);
        }
        if (RenderFeatureManager.isEnabled("item_esp")) {
            renderItemEspBoxes(renderContext, geometryPoseStack, frameData, partialTicks);
        }
        if (RenderFeatureManager.isEnabled("block_outline")) {
            renderBlockOutline(mc, renderContext, geometryPoseStack);
        }

        if (RenderFeatureManager.isEnabled("player_skeleton")) {
            renderPlayerSkeletons(mc, renderContext, linePoseStack, frameData, partialTicks);
        }
        if (RenderFeatureManager.isEnabled("tracer_line")) {
            renderTracers(mc, renderContext, linePoseStack, frameData, partialTicks);
        }
        if (RenderFeatureManager.isEnabled("trajectory_line")) {
            renderTrajectory(mc, renderContext, linePoseStack, partialTicks);
        }

        boolean renderEntityLabels = RenderFeatureManager.isEnabled("entity_tags");
        boolean renderItemLabels = RenderFeatureManager.isEnabled("item_esp")
                && (RenderFeatureManager.itemEspShowName || RenderFeatureManager.itemEspShowDistance);
        if (renderEntityLabels || renderItemLabels) {
            MultiBufferSource.BufferSource labelBuffer = mc.renderBuffers().bufferSource();
            if (renderEntityLabels) {
                renderEntityTags(mc, renderContext, frameData, partialTicks, labelBuffer);
            }
            if (renderItemLabels) {
                renderItemLabels(mc, renderContext, frameData, partialTicks, labelBuffer);
            }
            labelBuffer.endBatch();
        }
    }

    static void renderCrosshair(Minecraft mc, GuiGraphics graphics) {
        if (mc == null || graphics == null) {
            return;
        }
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;
        double horizontalSpeed = mc.player == null ? 0.0D : Math.sqrt(mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x
                + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z);
        float attackPenalty = mc.player == null ? 0.0F : 1.0F - mc.player.getAttackStrengthScale(0.0F);
        int gap = RenderFeatureManager.crosshairDynamicGap
                ? 4 + Math.round((float) Math.min(3.0D, horizontalSpeed * 10.0D) + attackPenalty * 4.0F)
                : 4;
        int size = Math.round(RenderFeatureManager.crosshairSize);
        int thickness = Math.max(1, Math.round(RenderFeatureManager.crosshairThickness));
        int color = 0xFF000000 | (RenderFeatureManager.crosshairColorRgb & 0xFFFFFF);

        graphics.fill(centerX - thickness / 2, centerY - gap - size, centerX + (thickness + 1) / 2, centerY - gap, color);
        graphics.fill(centerX - thickness / 2, centerY + gap, centerX + (thickness + 1) / 2, centerY + gap + size, color);
        graphics.fill(centerX - gap - size, centerY - thickness / 2, centerX - gap, centerY + (thickness + 1) / 2, color);
        graphics.fill(centerX + gap, centerY - thickness / 2, centerX + gap + size, centerY + (thickness + 1) / 2, color);
        graphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, color);
    }

    static void renderRadar(Minecraft mc, GuiGraphics graphics) {
        if (mc == null || mc.player == null || graphics == null) {
            return;
        }

        RenderFrameData frameData = getRenderFrameData(mc);
        int radarSize = Mth.clamp(RenderFeatureManager.radarSize, 60, 180);
        int x = mc.getWindow().getGuiScaledWidth() - radarSize - 12;
        int y = 12;
        int centerX = x + radarSize / 2;
        int centerY = y + radarSize / 2;
        int radius = radarSize / 2 - 7;
        float maxDistance = Math.max(8.0F, RenderFeatureManager.radarMaxDistance);
        double scale = radius / maxDistance;

        graphics.fill(x, y, x + radarSize, y + radarSize, 0x8A0E141C);
        graphics.fill(x, y, x + radarSize, y + 1, 0xFF5FB8FF);
        graphics.fill(centerX - 1, y + 6, centerX + 1, y + radarSize - 6, 0x443F6D8F);
        graphics.fill(x + 6, centerY - 1, x + radarSize - 6, centerY + 1, 0x443F6D8F);
        graphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFFFFFFFF);
        graphics.drawString(mc.font, "雷达", x + 5, y + 4, 0xFFEAF6FF, true);
        graphics.drawString(mc.font, String.valueOf((int) maxDistance), x + radarSize - 18, y + radarSize - 11, 0xFFB8D8F2, true);

        double yawRad = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double playerX = Mth.lerp(1.0F, mc.player.xo, mc.player.getX());
        double playerZ = Mth.lerp(1.0F, mc.player.zo, mc.player.getZ());
        double maxDistanceSq = maxDistance * maxDistance;

        for (EntityRenderSample sample : frameData.livingEntities) {
            if (!sample.matches(RenderFeatureManager.radarPlayers, RenderFeatureManager.radarMonsters,
                    RenderFeatureManager.radarAnimals, maxDistanceSq)) {
                continue;
            }
            Entity entity = sample.entity;
            double dx = Mth.lerp(1.0F, entity.xo, entity.getX()) - playerX;
            double dz = Mth.lerp(1.0F, entity.zo, entity.getZ()) - playerZ;
            double localX = RenderFeatureManager.radarRotateWithView ? dx * cos + dz * sin : dx;
            double localZ = RenderFeatureManager.radarRotateWithView ? dz * cos - dx * sin : dz;
            int px = centerX + (int) Math.round(localX * scale);
            int py = centerY + (int) Math.round(localZ * scale);
            int clampedX = Mth.clamp(px, x + 5, x + radarSize - 5);
            int clampedY = Mth.clamp(py, y + 5, y + radarSize - 5);
            int dotColor = getEntityHudColor(entity);
            graphics.fill(clampedX - 1, clampedY - 1, clampedX + 2, clampedY + 2, dotColor);
        }
    }

    static void renderEntityInfo(Minecraft mc, GuiGraphics graphics) {
        if (mc == null || mc.player == null || graphics == null || !(mc.hitResult instanceof EntityHitResult entityHitResult)) {
            return;
        }
        Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof LivingEntity living) || mc.player.distanceTo(entity) > RenderFeatureManager.entityInfoMaxDistance) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("§b" + entity.getName().getString() + " §7[" + entity.getClass().getSimpleName() + "]");
        if (RenderFeatureManager.entityInfoShowHealth) {
            lines.add("§c生命: §f" + formatFloat(Math.max(0.0F, living.getHealth())));
        }
        if (RenderFeatureManager.entityInfoShowDistance) {
            lines.add("§e距离: §f" + formatFloat(mc.player.distanceTo(entity)) + "m");
        }
        if (RenderFeatureManager.entityInfoShowPosition) {
            lines.add("§a坐标: §f" + formatFloat((float) entity.getX()) + ", "
                    + formatFloat((float) entity.getY()) + ", " + formatFloat((float) entity.getZ()));
        }
        if (RenderFeatureManager.entityInfoShowHeldItem) {
            ItemStack held = living.getMainHandItem();
            if (!held.isEmpty()) {
                lines.add("§d手持: §f" + held.getHoverName().getString());
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, mc.font.width(line));
        }
        int panelX = 10;
        int panelY = 10;
        int lineHeight = mc.font.lineHeight + 2;
        int panelWidth = maxWidth + 10;
        int panelHeight = lines.size() * lineHeight + 8;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xAA10161D);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF62C6FF);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(mc.font, lines.get(i), panelX + 5, panelY + 4 + i * lineHeight, 0xFFFFFFFF, true);
        }
    }

    private static void renderEntityVisuals(Minecraft mc, RenderWorldContext renderContext, PoseStack poseStack,
            RenderFrameData frameData, float partialTicks) {
        double maxDistanceSq = RenderFeatureManager.entityVisualMaxDistance * RenderFeatureManager.entityVisualMaxDistance;
        for (EntityRenderSample sample : frameData.livingEntities) {
            if (!sample.matches(RenderFeatureManager.entityVisualPlayers, RenderFeatureManager.entityVisualMonsters,
                    RenderFeatureManager.entityVisualAnimals, maxDistanceSq)) {
                continue;
            }
            float[] color = getEntityColor(sample.entity);
            Color outline = new Color(color[0], color[1], color[2]);
            AABB box = interpolateBoundingBox(sample.entity, partialTicks).inflate(0.035D);
            if (RenderFeatureManager.entityVisualFilledBox) {
                WorldGizmoRenderer.filledBoxWorld(box, outline, 0.95F, 0.12F, 2.0F,
                        RenderFeatureManager.entityVisualThroughWalls);
            } else {
                WorldGizmoRenderer.boxWorld(box, outline, 0.95F, 2.0F,
                        RenderFeatureManager.entityVisualThroughWalls);
            }
        }
    }

    private static void renderPlayerSkeletons(Minecraft mc, RenderWorldContext renderContext, PoseStack poseStack,
            RenderFrameData frameData, float partialTicks) {
        startLines(Math.max(1.0F, RenderFeatureManager.skeletonLineWidth), true, RenderFeatureManager.skeletonThroughWalls);
        double maxDistanceSq = RenderFeatureManager.skeletonMaxDistance * RenderFeatureManager.skeletonMaxDistance;
        for (EntityRenderSample sample : frameData.playerEntities) {
            if (sample.distanceSq > maxDistanceSq || !(sample.entity instanceof Player player) || player == mc.player) {
                continue;
            }
            AABB box = interpolateBoundingBox(player, partialTicks);
            Vec3 center = box.getCenter();
            double width = Math.max(0.3D, box.getXsize());
            double height = Math.max(1.0D, box.getYsize());
            double yawRad = Math.toRadians(Mth.lerp(partialTicks, player.yRotO, player.getYRot()));

            Vec3 pelvis = localToWorld(center.x, box.minY, center.z, 0.0D, height * 0.52D, 0.0D, yawRad);
            Vec3 neck = localToWorld(center.x, box.minY, center.z, 0.0D, height * 0.78D, 0.0D, yawRad);
            Vec3 head = localToWorld(center.x, box.minY, center.z, 0.0D, height * 1.02D, 0.0D, yawRad);
            Vec3 leftShoulder = localToWorld(center.x, box.minY, center.z, -width * 0.36D, height * 0.78D, 0.0D, yawRad);
            Vec3 rightShoulder = localToWorld(center.x, box.minY, center.z, width * 0.36D, height * 0.78D, 0.0D, yawRad);
            Vec3 leftArm = localToWorld(center.x, box.minY, center.z, -width * 0.52D, height * 0.54D, 0.0D, yawRad);
            Vec3 rightArm = localToWorld(center.x, box.minY, center.z, width * 0.52D, height * 0.54D, 0.0D, yawRad);
            Vec3 leftHip = localToWorld(center.x, box.minY, center.z, -width * 0.18D, height * 0.52D, 0.0D, yawRad);
            Vec3 rightHip = localToWorld(center.x, box.minY, center.z, width * 0.18D, height * 0.52D, 0.0D, yawRad);
            Vec3 leftLeg = localToWorld(center.x, box.minY, center.z, -width * 0.18D, 0.05D, 0.0D, yawRad);
            Vec3 rightLeg = localToWorld(center.x, box.minY, center.z, width * 0.18D, 0.05D, 0.0D, yawRad);

            head = renderContext.toCameraSpace(head);
            neck = renderContext.toCameraSpace(neck);
            pelvis = renderContext.toCameraSpace(pelvis);
            leftShoulder = renderContext.toCameraSpace(leftShoulder);
            rightShoulder = renderContext.toCameraSpace(rightShoulder);
            leftArm = renderContext.toCameraSpace(leftArm);
            rightArm = renderContext.toCameraSpace(rightArm);
            leftHip = renderContext.toCameraSpace(leftHip);
            rightHip = renderContext.toCameraSpace(rightHip);
            leftLeg = renderContext.toCameraSpace(leftLeg);
            rightLeg = renderContext.toCameraSpace(rightLeg);

            emitLine(poseStack, head, neck, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, neck, pelvis, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, leftShoulder, rightShoulder, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, neck, leftShoulder, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, neck, rightShoulder, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, leftShoulder, leftArm, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, rightShoulder, rightArm, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, pelvis, leftHip, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, pelvis, rightHip, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, leftHip, leftLeg, 0.30F, 0.88F, 1.0F, 0.92F);
            emitLine(poseStack, rightHip, rightLeg, 0.30F, 0.88F, 1.0F, 0.92F);
        }
        endLines(RenderFeatureManager.skeletonThroughWalls);
    }

    private static void renderTracers(Minecraft mc, RenderWorldContext renderContext, PoseStack poseStack,
            RenderFrameData frameData, float partialTicks) {
        startLines(RenderFeatureManager.tracerLineWidth, true, RenderFeatureManager.tracerThroughWalls);
        double maxDistanceSq = RenderFeatureManager.tracerMaxDistance * RenderFeatureManager.tracerMaxDistance;
        Vec3 eye = getCameraLineStart(mc, partialTicks);
        for (EntityRenderSample sample : frameData.livingEntities) {
            if (!sample.matches(RenderFeatureManager.tracerPlayers, RenderFeatureManager.tracerMonsters,
                    RenderFeatureManager.tracerAnimals, maxDistanceSq)) {
                continue;
            }
            float[] color = getEntityColor(sample.entity);
            WorldGizmoRenderer.setColor(color[0], color[1], color[2], 0.82F);
            WorldGizmoRenderer.lineWorld(eye, interpolateEntityCenter(sample.entity, partialTicks));
        }
        endLines(RenderFeatureManager.tracerThroughWalls);
    }

    private static void renderEntityTags(Minecraft mc, RenderWorldContext renderContext, RenderFrameData frameData,
            float partialTicks, MultiBufferSource.BufferSource buffer) {
        double maxDistanceSq = RenderFeatureManager.entityTagMaxDistance * RenderFeatureManager.entityTagMaxDistance;
        for (EntityRenderSample sample : frameData.livingEntities) {
            if (!sample.matches(RenderFeatureManager.entityTagPlayers, RenderFeatureManager.entityTagMonsters,
                    RenderFeatureManager.entityTagAnimals, maxDistanceSq)) {
                continue;
            }
            LivingEntity living = sample.entity;

            List<String> lines = new ArrayList<>();
            lines.add(sample.entity.getName().getString());
            if (RenderFeatureManager.entityTagShowHealth) {
                lines.add("§cHP: §f" + formatFloat(Math.max(0.0F, living.getHealth())));
            }
            if (RenderFeatureManager.entityTagShowDistance) {
                lines.add("§b距离: §f" + formatFloat(mc.player.distanceTo(sample.entity)) + "m");
            }
            if (RenderFeatureManager.entityTagShowHeldItem && !living.getMainHandItem().isEmpty()) {
                lines.add("§e手持: §f" + living.getMainHandItem().getHoverName().getString());
            }
            drawWorldLabel(mc, renderContext, buffer,
                    interpolateEntityCenter(sample.entity, partialTicks).add(0.0D, sample.entity.getBbHeight() * 0.45D, 0.0D),
                    lines, 0xFFFFFFFF, true);
        }
    }

    private static void renderBlockHighlights(RenderWorldContext renderContext, PoseStack poseStack) {
        for (BlockHighlightEntry entry : BLOCK_HIGHLIGHTS) {
            Color color = new Color(entry.r, entry.g, entry.b);
            if (entry.filled) {
                WorldGizmoRenderer.filledBoxWorld(entry.box.inflate(0.003D), color, 0.88F, entry.alpha, 2.0F,
                        RenderFeatureManager.blockHighlightThroughWalls);
            } else {
                WorldGizmoRenderer.boxWorld(entry.box.inflate(0.003D), color, 0.88F, 2.0F,
                        RenderFeatureManager.blockHighlightThroughWalls);
            }
        }
    }

    private static void renderItemEspBoxes(RenderWorldContext renderContext, PoseStack poseStack,
            RenderFrameData frameData, float partialTicks) {
        double maxDistanceSq = RenderFeatureManager.itemEspMaxDistance * RenderFeatureManager.itemEspMaxDistance;
        Color color = new Color(1.0F, 0.86F, 0.28F);
        for (ItemRenderSample sample : frameData.itemEntities) {
            if (sample.distanceSq > maxDistanceSq) {
                continue;
            }
            WorldGizmoRenderer.boxWorld(interpolateBoundingBox(sample.entityItem, partialTicks).inflate(0.03D),
                    color, 0.9F, 1.7F, RenderFeatureManager.itemEspThroughWalls);
        }
    }

    private static void renderItemLabels(Minecraft mc, RenderWorldContext renderContext, RenderFrameData frameData,
            float partialTicks, MultiBufferSource.BufferSource buffer) {
        double maxDistanceSq = RenderFeatureManager.itemEspMaxDistance * RenderFeatureManager.itemEspMaxDistance;
        for (ItemRenderSample sample : frameData.itemEntities) {
            if (sample.distanceSq > maxDistanceSq) {
                continue;
            }
            List<String> lines = new ArrayList<>();
            if (RenderFeatureManager.itemEspShowName) {
                lines.add(sample.entityItem.getItem().getHoverName().getString());
            }
            if (RenderFeatureManager.itemEspShowDistance) {
                lines.add("§e距离: §f" + formatFloat(mc.player.distanceTo(sample.entityItem)) + "m");
            }
            if (!lines.isEmpty()) {
                drawWorldLabel(mc, renderContext, buffer,
                        interpolateEntityCenter(sample.entityItem, partialTicks).add(0.0D, 0.35D, 0.0D),
                        lines, 0xFFFFFFAA, RenderFeatureManager.itemEspThroughWalls);
            }
        }
    }

    private static void renderTrajectory(Minecraft mc, RenderWorldContext renderContext, PoseStack poseStack, float partialTicks) {
        if (mc.player == null) {
            return;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()
                || !(held.getItem() instanceof BowItem
                || held.getItem() instanceof EnderpearlItem
                || held.getItem() instanceof SnowballItem
                || held.getItem() instanceof EggItem
                || held.getItem() instanceof PotionItem
                || held.getItem() instanceof SplashPotionItem
                || held.getItem() instanceof LingeringPotionItem
                || held.getItem() instanceof ExperienceBottleItem)) {
            held = mc.player.getOffhandItem();
        }
        if (held.isEmpty()) {
            return;
        }

        double velocity;
        double gravity;
        if (held.getItem() instanceof BowItem) {
            if (!RenderFeatureManager.trajectoryBows || !mc.player.isUsingItem()) {
                return;
            }
            int useTicks = held.getUseDuration(mc.player) - mc.player.getUseItemRemainingTicks();
            float charge = useTicks / 20.0F;
            charge = (charge * charge + charge * 2.0F) / 3.0F;
            if (charge < 0.1F) {
                return;
            }
            velocity = Math.min(charge, 1.0F) * 3.0D;
            gravity = 0.05D;
        } else if (held.getItem() instanceof EnderpearlItem) {
            if (!RenderFeatureManager.trajectoryPearls) {
                return;
            }
            velocity = 1.5D;
            gravity = 0.03D;
        } else if (held.getItem() instanceof SnowballItem || held.getItem() instanceof EggItem) {
            if (!RenderFeatureManager.trajectoryThrowables) {
                return;
            }
            velocity = 1.5D;
            gravity = 0.03D;
        } else {
            if (!RenderFeatureManager.trajectoryPotions) {
                return;
            }
            velocity = 0.9D;
            gravity = 0.05D;
        }

        startLines(2.0F, true, false);
        Vec3 previous = getSafeLineStart(mc, partialTicks);
        Vec3 motion = getCameraLookDirection(mc).scale(velocity);
        for (int i = 0; i < RenderFeatureManager.trajectoryMaxSteps; i++) {
            Vec3 next = previous.add(motion);
            emitLine(poseStack, renderContext.toCameraSpace(previous), renderContext.toCameraSpace(next),
                    0.35F, 1.0F, 1.0F, 1.0F);
            previous = next;
            motion = motion.add(0.0D, -gravity, 0.0D).scale(0.99D);
            if (!mc.level.noCollision(mc.player, new AABB(previous, previous).inflate(0.15D))) {
                break;
            }
        }
        endLines(false);
    }

    private static void renderBlockOutline(Minecraft mc, RenderWorldContext renderContext, PoseStack poseStack) {
        if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }
        BlockPos pos = blockHitResult.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        Entity viewEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        VoxelShape shape = state.getShape(mc.level, pos, viewEntity == null ? CollisionContext.empty() : CollisionContext.of(viewEntity));
        if (shape.isEmpty()) {
            return;
        }

        Vec3 cameraSpaceOrigin = renderContext.toCameraSpace(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
        startLines(Math.max(0.5F, RenderFeatureManager.blockOutlineLineWidth), true, false);
        renderShapeLines(poseStack, shape, cameraSpaceOrigin.x, cameraSpaceOrigin.y, cameraSpaceOrigin.z,
                0.3F, 0.9F, 1.0F, 0.95F);
        endLines(false);

        if (RenderFeatureManager.blockOutlineFilledBox) {
            WorldGizmoRenderer.filledBoxWorld(shape.bounds().move(pos).inflate(0.002D),
                    new Color(0.3F, 0.9F, 1.0F), 0.0F, 0.12F, 1.0F, false);
        }
    }

    private static void updateBlockHighlights(Minecraft mc, LocalPlayer player) {
        BLOCK_HIGHLIGHTS.clear();
        if (mc == null || mc.level == null || player == null) {
            return;
        }

        int radius = Math.max(4, Math.round(RenderFeatureManager.blockHighlightMaxDistance));
        int verticalRadius = Math.min(radius, ORE_SCAN_VERTICAL_RADIUS_LIMIT);
        BlockPos center = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -verticalRadius, -radius),
                center.offset(radius, verticalRadius, radius))) {
            if (BLOCK_HIGHLIGHTS.size() >= MAX_BLOCK_SCAN_RESULTS) {
                break;
            }
            BlockState state = mc.level.getBlockState(pos);
            Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            String id = key == null ? "" : key.toString();
            boolean storage = RenderFeatureManager.blockHighlightStorages
                    && (id.contains("chest") || id.contains("shulker_box") || id.contains("barrel") || id.contains("ender_chest"));
            boolean spawner = RenderFeatureManager.blockHighlightSpawners && id.contains("spawner");
            boolean ore = RenderFeatureManager.blockHighlightOres && RenderFeatureManager.isOreBlock(state);
            boolean xrayVisible = RenderFeatureManager.isEnabled("xray") && RenderFeatureManager.isXrayBlockVisible(state);
            if (!storage && !spawner && !ore && !xrayVisible) {
                continue;
            }
            int color = (ore || xrayVisible)
                    ? RenderFeatureManager.getBlockHighlightColor(state)
                    : (storage ? 0x55FFFF : 0xFFAA55);
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            boolean filled = RenderFeatureManager.blockHighlightFilledBox || RenderFeatureManager.isEnabled("xray");
            float alpha = RenderFeatureManager.isEnabled("xray") ? 0.18F : 0.08F;
            BLOCK_HIGHLIGHTS.add(new BlockHighlightEntry(new AABB(pos), r, g, b, filled, alpha));
        }
    }

    private static RenderFrameData getRenderFrameData(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) {
            return RenderFrameData.EMPTY;
        }
        if (cachedRenderFrameData != null && cachedRenderFrameTick == mc.player.tickCount) {
            return cachedRenderFrameData;
        }

        double maxLivingDistanceSq = getMaxLivingDistanceSq();
        double maxSkeletonDistanceSq = RenderFeatureManager.isEnabled("player_skeleton")
                ? RenderFeatureManager.skeletonMaxDistance * RenderFeatureManager.skeletonMaxDistance
                : 0.0D;
        double maxItemDistanceSq = RenderFeatureManager.isEnabled("item_esp")
                ? RenderFeatureManager.itemEspMaxDistance * RenderFeatureManager.itemEspMaxDistance
                : 0.0D;

        List<EntityRenderSample> livingEntities = new ArrayList<>();
        List<EntityRenderSample> playerEntities = new ArrayList<>();
        List<ItemRenderSample> itemEntities = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || entity == mc.player || !entity.isAlive()) {
                continue;
            }

            if (entity instanceof LivingEntity living) {
                double distanceSq = mc.player.distanceToSqr(living);
                boolean isPlayer = living instanceof Player;
                boolean isMonster = isMonsterEntity(living);
                boolean isAnimal = isAnimalEntity(living);
                if (maxLivingDistanceSq > 0.0D && distanceSq <= maxLivingDistanceSq && (isPlayer || isMonster || isAnimal)) {
                    livingEntities.add(new EntityRenderSample(living, distanceSq, isPlayer, isMonster, isAnimal));
                }
                if (maxSkeletonDistanceSq > 0.0D && distanceSq <= maxSkeletonDistanceSq && isPlayer) {
                    playerEntities.add(new EntityRenderSample(living, distanceSq, true, false, false));
                }
            }

            if (maxItemDistanceSq > 0.0D && entity instanceof ItemEntity itemEntity) {
                double distanceSq = mc.player.distanceToSqr(itemEntity);
                if (distanceSq <= maxItemDistanceSq && itemEntity.onGround()) {
                    itemEntities.add(new ItemRenderSample(itemEntity, distanceSq));
                }
            }
        }

        cachedRenderFrameData = new RenderFrameData(livingEntities, playerEntities, itemEntities);
        cachedRenderFrameTick = mc.player.tickCount;
        return cachedRenderFrameData;
    }

    private static double getMaxLivingDistanceSq() {
        double maxDistance = 0.0D;
        if (RenderFeatureManager.isEnabled("radar")) {
            maxDistance = Math.max(maxDistance, RenderFeatureManager.radarMaxDistance);
        }
        if (RenderFeatureManager.isEnabled("entity_visual")) {
            maxDistance = Math.max(maxDistance, RenderFeatureManager.entityVisualMaxDistance);
        }
        if (RenderFeatureManager.isEnabled("tracer_line")) {
            maxDistance = Math.max(maxDistance, RenderFeatureManager.tracerMaxDistance);
        }
        if (RenderFeatureManager.isEnabled("entity_tags")) {
            maxDistance = Math.max(maxDistance, RenderFeatureManager.entityTagMaxDistance);
        }
        return maxDistance <= 0.0D ? 0.0D : maxDistance * maxDistance;
    }

    private static boolean isMonsterEntity(Entity entity) {
        return entity instanceof Enemy;
    }

    private static boolean isAnimalEntity(Entity entity) {
        return entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal;
    }

    private static float[] getEntityColor(Entity entity) {
        if (entity instanceof Player) {
            return new float[] { 0.22F, 0.86F, 1.0F };
        }
        if (entity instanceof Enemy) {
            return new float[] { 1.0F, 0.30F, 0.30F };
        }
        return new float[] { 0.35F, 1.0F, 0.48F };
    }

    private static int getEntityHudColor(Entity entity) {
        if (entity instanceof Player) {
            return 0xFF55E3FF;
        }
        if (entity instanceof Enemy) {
            return 0xFFFF6464;
        }
        return 0xFF66FF7A;
    }

    private static AABB interpolateBoundingBox(Entity entity, float partialTicks) {
        AABB box = entity.getBoundingBox();
        double x = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double y = Mth.lerp(partialTicks, entity.yo, entity.getY());
        double z = Mth.lerp(partialTicks, entity.zo, entity.getZ());
        return box.move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
    }

    private static Vec3 interpolateEntityCenter(Entity entity, float partialTicks) {
        AABB box = interpolateBoundingBox(entity, partialTicks);
        return box.getCenter();
    }

    private static Vec3 localToWorld(double baseX, double baseY, double baseZ, double localX, double localY,
            double localZ, double yawRad) {
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double worldX = baseX + localX * cos - localZ * sin;
        double worldZ = baseZ + localX * sin + localZ * cos;
        return new Vec3(worldX, baseY + localY, worldZ);
    }

    private static Vec3 getSafeLineStart(Minecraft mc, float partialTicks) {
        if (mc == null || mc.player == null) {
            return Vec3.ZERO;
        }
        LocalPlayer player = mc.player;
        double x = Mth.lerp(partialTicks, player.xo, player.getX());
        double y = Mth.lerp(partialTicks, player.yo, player.getY()) + player.getEyeHeight();
        double z = Mth.lerp(partialTicks, player.zo, player.getZ());
        return new Vec3(x, y, z);
    }

    private static Vec3 getCameraLineStart(Minecraft mc, float partialTicks) {
        Camera camera = mc == null || mc.gameRenderer == null ? null : mc.gameRenderer.getMainCamera();
        if (camera != null && camera.isInitialized()) {
            org.joml.Vector3fc look = camera.forwardVector();
            Vec3 forward = new Vec3(look.x(), look.y(), look.z());
            return camera.position().add(forward.scale(0.35D));
        }
        return getSafeLineStart(mc, partialTicks);
    }

    private static Vec3 getCameraLookDirection(Minecraft mc) {
        Camera camera = mc == null || mc.gameRenderer == null ? null : mc.gameRenderer.getMainCamera();
        if (camera != null && camera.isInitialized()) {
            org.joml.Vector3fc look = camera.forwardVector();
            return new Vec3(look.x(), look.y(), look.z());
        }
        return mc != null && mc.player != null ? mc.player.getLookAngle() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static void drawWorldLabel(Minecraft mc, RenderWorldContext renderContext, MultiBufferSource.BufferSource buffer,
            Vec3 worldPos, List<String> lines, int textColor, boolean throughWalls) {
        WorldGizmoRenderer.label(worldPos, lines, textColor, throughWalls);
    }

    private static void startLines(float lineWidth, boolean blend, boolean ignoreDepth) {
        WorldGizmoRenderer.beginLines(1.0F, 1.0F, 1.0F, 1.0F, lineWidth, ignoreDepth);
    }

    private static void endLines(boolean ignoredDepth) {
        WorldGizmoRenderer.endLines();
    }

    private static void startFilledBoxes(boolean ignoreDepth) {
        WorldGizmoRenderer.beginLines(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, ignoreDepth);
    }

    private static void endFilledBoxes(boolean ignoredDepth) {
        WorldGizmoRenderer.endLines();
    }

    private static void emitLine(PoseStack poseStack, Vec3 start, Vec3 end, float r, float g, float b, float a) {
        WorldGizmoRenderer.setColor(r, g, b, a);
        WorldGizmoRenderer.lineCameraSpace(start.x, start.y, start.z, end.x, end.y, end.z);
    }

    private static void renderShapeLines(PoseStack poseStack, VoxelShape shape, double offsetX, double offsetY,
            double offsetZ, float r, float g, float b, float a) {
        WorldGizmoRenderer.setColor(r, g, b, a);
        WorldGizmoRenderer.shapeCameraSpace(shape, offsetX, offsetY, offsetZ);
    }

    private static Matrix4f matrixFromRotation(Matrix3f rotation) {
        Matrix4f matrix = new Matrix4f().identity();
        matrix.m00(rotation.m00());
        matrix.m01(rotation.m01());
        matrix.m02(rotation.m02());
        matrix.m10(rotation.m10());
        matrix.m11(rotation.m11());
        matrix.m12(rotation.m12());
        matrix.m20(rotation.m20());
        matrix.m21(rotation.m21());
        matrix.m22(rotation.m22());
        return matrix;
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class RenderWorldContext {
        private final Vec3 cameraPos;
        private final Matrix3f inverseViewRotation;
        private final Matrix3f viewRotation;
        private final Matrix4f inverseViewRotationMatrix;
        private final Matrix4f viewRotationMatrix;

        private RenderWorldContext(Vec3 cameraPos, Matrix3f inverseViewRotation, Matrix3f viewRotation) {
            this.cameraPos = cameraPos;
            this.inverseViewRotation = inverseViewRotation;
            this.viewRotation = viewRotation;
            this.inverseViewRotationMatrix = matrixFromRotation(inverseViewRotation);
            this.viewRotationMatrix = matrixFromRotation(viewRotation);
        }

        private static RenderWorldContext create(Camera camera) {
            return new RenderWorldContext(camera.position(), new Matrix3f().identity(), new Matrix3f().identity());
        }

        private PoseStack createBasePoseStack() {
            return new PoseStack();
        }

        private Vec3 toCameraSpace(Vec3 worldPos) {
            return worldPos.subtract(cameraPos);
        }

        private AABB toCameraSpace(AABB worldBox) {
            return worldBox.move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        }
    }

    private static final class RenderFrameData {
        private static final RenderFrameData EMPTY = new RenderFrameData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        private final List<EntityRenderSample> livingEntities;
        private final List<EntityRenderSample> playerEntities;
        private final List<ItemRenderSample> itemEntities;

        private RenderFrameData(List<EntityRenderSample> livingEntities, List<EntityRenderSample> playerEntities,
                List<ItemRenderSample> itemEntities) {
            this.livingEntities = livingEntities;
            this.playerEntities = playerEntities;
            this.itemEntities = itemEntities;
        }
    }

    private static final class EntityRenderSample {
        private final LivingEntity entity;
        private final double distanceSq;
        private final boolean player;
        private final boolean monster;
        private final boolean animal;

        private EntityRenderSample(LivingEntity entity, double distanceSq, boolean player, boolean monster, boolean animal) {
            this.entity = entity;
            this.distanceSq = distanceSq;
            this.player = player;
            this.monster = monster;
            this.animal = animal;
        }

        private boolean matches(boolean includePlayers, boolean includeMonsters, boolean includeAnimals, double maxDistanceSq) {
            return distanceSq <= maxDistanceSq
                    && ((includePlayers && player) || (includeMonsters && monster) || (includeAnimals && animal));
        }
    }

    private static final class ItemRenderSample {
        private final ItemEntity entityItem;
        private final double distanceSq;

        private ItemRenderSample(ItemEntity entityItem, double distanceSq) {
            this.entityItem = entityItem;
            this.distanceSq = distanceSq;
        }
    }

    private static final class BlockHighlightEntry {
        private final AABB box;
        private final float r;
        private final float g;
        private final float b;
        private final boolean filled;
        private final float alpha;

        private BlockHighlightEntry(AABB box, float r, float g, float b, boolean filled, float alpha) {
            this.box = box;
            this.r = r;
            this.g = g;
            this.b = b;
            this.filled = filled;
            this.alpha = alpha;
        }
    }
}
