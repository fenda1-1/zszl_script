package com.zszl.zszlScriptMod.utils;

import com.mojang.blaze3d.platform.NativeImage;

import java.awt.image.BufferedImage;

public final class NativeImageHelper {

    private NativeImageHelper() {
    }

    public static NativeImage fromBufferedImage(BufferedImage image) {
        if (image == null) {
            return null;
        }

        NativeImage nativeImage = null;
        try {
            int width = Math.max(1, image.getWidth());
            int height = Math.max(1, image.getHeight());
            nativeImage = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    nativeImage.setPixelABGR(x, y, argbToAbgr(image.getRGB(x, y)));
                }
            }
            return nativeImage;
        } catch (Throwable throwable) {
            if (nativeImage != null) {
                nativeImage.close();
            }
            return null;
        }
    }

    private static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00)
                | ((argb >> 16) & 0x000000FF)
                | ((argb << 16) & 0x00FF0000);
    }
}
