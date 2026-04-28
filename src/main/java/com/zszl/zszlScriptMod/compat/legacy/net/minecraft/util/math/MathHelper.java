package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math;

import net.minecraft.util.Mth;

public final class MathHelper {

    private MathHelper() {
    }

    public static int floor(double value) {
        return Mth.floor(value);
    }

    public static int ceil(double value) {
        return Mth.ceil(value);
    }

    public static float clamp(float value, float min, float max) {
        return Mth.clamp(value, min, max);
    }

    public static double clamp(double value, double min, double max) {
        return Mth.clamp(value, min, max);
    }

    public static int clamp(int value, int min, int max) {
        return Mth.clamp(value, min, max);
    }

    public static float wrapDegrees(float value) {
        return Mth.wrapDegrees(value);
    }

    public static double wrapDegrees(double value) {
        return Mth.wrapDegrees(value);
    }
}

