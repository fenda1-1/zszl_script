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

package com.zszl.zszlScriptMod.shadowbaritone.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszl.zszlScriptMod.compat.render.WorldGizmoRenderer;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.Settings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

public interface IRenderer {

    TextureManager textureManager = Minecraft.getInstance().getTextureManager();
    Settings settings = BaritoneAPI.getSettings();

    static Vec3 renderPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameRenderer == null) {
            return Vec3.ZERO;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) {
            return Vec3.ZERO;
        }
        return camera.position();
    }

    static double renderPosX() {
        return renderPosition().x;
    }

    static double renderPosY() {
        return renderPosition().y;
    }

    static double renderPosZ() {
        return renderPosition().z;
    }

    static void glColor(Color color, float alpha) {
        WorldGizmoRenderer.setColor(color, alpha);
    }

    static void startLines(Color color, float alpha, float lineWidth, boolean ignoreDepth) {
        WorldGizmoRenderer.beginLines(color, alpha, lineWidth, ignoreDepth);
    }

    static void startLines(Color color, float lineWidth, boolean ignoreDepth) {
        startLines(color, .4f, lineWidth, ignoreDepth);
    }

    static void endLines(boolean ignoredDepth) {
        WorldGizmoRenderer.endLines();
    }

    static void emitLine(PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2) {
        WorldGizmoRenderer.lineCameraSpace(x1, y1, z1, x2, y2, z2);
    }

    static void emitLine(PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double nx, double ny, double nz) {
        WorldGizmoRenderer.lineCameraSpace(x1, y1, z1, x2, y2, z2);
    }

    static void emitLine(PoseStack stack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float nx, float ny, float nz) {
        WorldGizmoRenderer.lineCameraSpace(x1, y1, z1, x2, y2, z2);
    }

    static void emitAABB(PoseStack stack, AABB aabb) {
        WorldGizmoRenderer.boxWorld(aabb);
    }

    static void emitAABB(PoseStack stack, AABB aabb, double expand) {
        WorldGizmoRenderer.boxWorld(aabb.inflate(expand, expand, expand));
    }

    static void emitLine(PoseStack stack, Vec3 start, Vec3 end) {
        WorldGizmoRenderer.lineWorld(start, end);
    }
}
