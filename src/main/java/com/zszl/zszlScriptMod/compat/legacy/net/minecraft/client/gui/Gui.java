package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class Gui {

    protected float zLevel;

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.fill(left, top, right, bottom, color);
        }
    }

    public static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.fillGradient(left, top, right, bottom, startColor, endColor);
        }
    }

    public static void drawScaledCustomSizeModalRect(int x, int y, float u, float v, int uWidth, int vHeight,
            int width, int height, float tileWidth, float tileHeight) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null || width <= 0 || height <= 0 || tileWidth <= 0.0F || tileHeight <= 0.0F) {
            return;
        }

        float minU = u / tileWidth;
        float maxU = (u + uWidth) / tileWidth;
        float minV = v / tileHeight;
        float maxV = (v + vHeight) / tileHeight;
        Matrix4f pose = graphics.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(pose, x, y + height, 0.0F).uv(minU, maxV).endVertex();
        bufferBuilder.vertex(pose, x + width, y + height, 0.0F).uv(maxU, maxV).endVertex();
        bufferBuilder.vertex(pose, x + width, y, 0.0F).uv(maxU, minV).endVertex();
        bufferBuilder.vertex(pose, x, y, 0.0F).uv(minU, minV).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}

