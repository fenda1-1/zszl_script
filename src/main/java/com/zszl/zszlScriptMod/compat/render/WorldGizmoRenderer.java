package com.zszl.zszlScriptMod.compat.render;

import java.awt.Color;
import java.util.List;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class WorldGizmoRenderer {
    private static final int DEFAULT_SEGMENTS = 96;
    private static final double LABEL_LINE_SPACING = 0.22D;
    private static final ThreadLocal<LineState> LINE_STATE = ThreadLocal.withInitial(LineState::new);

    private WorldGizmoRenderer() {
    }

    public static void beginLines(Color color, float alpha, float lineWidth, boolean alwaysOnTop) {
        LineState state = LINE_STATE.get();
        state.color = toArgb(color, alpha);
        state.lineWidth = Math.max(0.5F, lineWidth);
        state.alwaysOnTop = alwaysOnTop;
    }

    public static void beginLines(float red, float green, float blue, float alpha, float lineWidth,
            boolean alwaysOnTop) {
        LineState state = LINE_STATE.get();
        state.color = toArgb(red, green, blue, alpha);
        state.lineWidth = Math.max(0.5F, lineWidth);
        state.alwaysOnTop = alwaysOnTop;
    }

    public static void setColor(Color color, float alpha) {
        LINE_STATE.get().color = toArgb(color, alpha);
    }

    public static void setColor(float red, float green, float blue, float alpha) {
        LINE_STATE.get().color = toArgb(red, green, blue, alpha);
    }

    public static void endLines() {
        // Vanilla 1.21 gizmos are submitted one primitive at a time.
    }

    public static void lineCameraSpace(double x1, double y1, double z1, double x2, double y2, double z2) {
        Vec3 camera = cameraPosition();
        lineWorld(camera.add(x1, y1, z1), camera.add(x2, y2, z2));
    }

    public static void lineWorld(Vec3 start, Vec3 end) {
        LineState state = LINE_STATE.get();
        lineWorld(start, end, state.color, state.lineWidth, state.alwaysOnTop);
    }

    public static void lineWorld(Vec3 start, Vec3 end, Color color, float alpha, float lineWidth,
            boolean alwaysOnTop) {
        lineWorld(start, end, toArgb(color, alpha), Math.max(0.5F, lineWidth), alwaysOnTop);
    }

    public static void lineWorld(Vec3 start, Vec3 end, float red, float green, float blue, float alpha,
            float lineWidth, boolean alwaysOnTop) {
        lineWorld(start, end, toArgb(red, green, blue, alpha), Math.max(0.5F, lineWidth), alwaysOnTop);
    }

    public static void boxCameraSpace(AABB box) {
        Vec3 camera = cameraPosition();
        boxWorld(box.move(camera.x, camera.y, camera.z));
    }

    public static void boxWorld(AABB box) {
        LineState state = LINE_STATE.get();
        boxWorld(box, state.color, state.lineWidth, state.alwaysOnTop);
    }

    public static void boxWorld(AABB box, Color color, float alpha, float lineWidth, boolean alwaysOnTop) {
        boxWorld(box, toArgb(color, alpha), Math.max(0.5F, lineWidth), alwaysOnTop);
    }

    public static void filledBoxWorld(AABB box, Color outlineColor, float outlineAlpha, float fillAlpha,
            float lineWidth, boolean alwaysOnTop) {
        if (box == null) {
            return;
        }
        int stroke = toArgb(outlineColor, outlineAlpha);
        int fill = toArgb(outlineColor, fillAlpha);
        try {
            applyAlwaysOnTop(Gizmos.cuboid(box, GizmoStyle.strokeAndFill(stroke, Math.max(0.5F, lineWidth), fill)),
                    alwaysOnTop);
        } catch (IllegalStateException ignored) {
            // Rendering can be asked for while no 1.21 gizmo collector is active.
        }
    }

    public static void shapeCameraSpace(VoxelShape shape, double offsetX, double offsetY, double offsetZ) {
        if (shape == null || shape.isEmpty()) {
            return;
        }
        Vec3 camera = cameraPosition();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> lineWorld(
                camera.add(offsetX + x1, offsetY + y1, offsetZ + z1),
                camera.add(offsetX + x2, offsetY + y2, offsetZ + z2)));
    }

    public static void ring(double centerX, double centerY, double centerZ, double radius, Color color, float alpha,
            float lineWidth, boolean alwaysOnTop) {
        ring(centerX, centerY, centerZ, radius, DEFAULT_SEGMENTS, color, alpha, lineWidth, alwaysOnTop);
    }

    public static void ring(double centerX, double centerY, double centerZ, double radius, int segments, Color color,
            float alpha, float lineWidth, boolean alwaysOnTop) {
        if (radius <= 0.0D) {
            return;
        }
        beginLines(color, alpha, lineWidth, alwaysOnTop);
        int safeSegments = Math.max(12, segments);
        Vec3 previous = null;
        for (int i = 0; i <= safeSegments; i++) {
            double angle = Math.PI * 2.0D * i / safeSegments;
            Vec3 current = new Vec3(centerX + Math.cos(angle) * radius, centerY, centerZ + Math.sin(angle) * radius);
            if (previous != null) {
                lineWorld(previous, current);
            }
            previous = current;
        }
        endLines();
    }

    public static void verticalRingWall(double centerX, double centerY, double centerZ, double radius,
            double bottomOffset, double topOffset, Color color, float alpha, float lineWidth, boolean alwaysOnTop) {
        if (radius <= 0.0D) {
            return;
        }
        double bottom = centerY + bottomOffset;
        double top = centerY + topOffset;
        beginLines(color, alpha, lineWidth, alwaysOnTop);
        Vec3 previousBottom = null;
        Vec3 previousTop = null;
        for (int i = 0; i <= DEFAULT_SEGMENTS; i++) {
            double angle = Math.PI * 2.0D * i / DEFAULT_SEGMENTS;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            Vec3 currentBottom = new Vec3(x, bottom, z);
            Vec3 currentTop = new Vec3(x, top, z);
            if (previousBottom != null) {
                lineWorld(previousBottom, currentBottom);
                lineWorld(previousTop, currentTop);
            }
            if (i % 12 == 0) {
                lineWorld(currentBottom, currentTop);
            }
            previousBottom = currentBottom;
            previousTop = currentTop;
        }
        endLines();
    }

    public static void label(Vec3 worldPos, List<String> lines, int textColor, boolean alwaysOnTop) {
        if (worldPos == null || lines == null || lines.isEmpty()) {
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            Vec3 pos = worldPos.add(0.0D, -i * LABEL_LINE_SPACING, 0.0D);
            try {
                applyAlwaysOnTop(Gizmos.billboardText(line, pos,
                        TextGizmo.Style.forColorAndCentered(textColor).withScale(0.34F)), alwaysOnTop);
            } catch (IllegalStateException ignored) {
                // Rendering can be asked for while no 1.21 gizmo collector is active.
            }
        }
    }

    public static Vec3 cameraPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameRenderer == null) {
            return Vec3.ZERO;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        return camera == null || !camera.isInitialized() ? Vec3.ZERO : camera.position();
    }

    public static AABB blockBox(BlockPos pos, double expand) {
        return new AABB(pos).inflate(expand, expand, expand);
    }

    private static void lineWorld(Vec3 start, Vec3 end, int color, float lineWidth, boolean alwaysOnTop) {
        if (start == null || end == null || start.distanceToSqr(end) <= 1.0E-8D) {
            return;
        }
        try {
            applyAlwaysOnTop(Gizmos.line(start, end, color, lineWidth), alwaysOnTop);
        } catch (IllegalStateException ignored) {
            // Rendering can be asked for while no 1.21 gizmo collector is active.
        }
    }

    private static void boxWorld(AABB box, int color, float lineWidth, boolean alwaysOnTop) {
        if (box == null) {
            return;
        }
        try {
            applyAlwaysOnTop(Gizmos.cuboid(box, GizmoStyle.stroke(color, Math.max(0.5F, lineWidth))), alwaysOnTop);
        } catch (IllegalStateException ignored) {
            // Rendering can be asked for while no 1.21 gizmo collector is active.
        }
    }

    private static void applyAlwaysOnTop(GizmoProperties properties, boolean alwaysOnTop) {
        if (properties != null && alwaysOnTop) {
            properties.setAlwaysOnTop();
        }
    }

    private static int toArgb(Color color, float alpha) {
        if (color == null) {
            return toArgb(1.0F, 1.0F, 1.0F, alpha);
        }
        return toArgb(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha);
    }

    private static int toArgb(float red, float green, float blue, float alpha) {
        int a = clamp255(alpha * 255.0F);
        int r = clamp255(red * 255.0F);
        int g = clamp255(green * 255.0F);
        int b = clamp255(blue * 255.0F);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static final class LineState {
        private int color = 0xFFFFFFFF;
        private float lineWidth = 2.0F;
        private boolean alwaysOnTop;
    }
}
