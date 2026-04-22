package com.zszl.zszlScriptMod.utils.vision;

public final class ScreenVisionUtils {

    public static final class RegionMetrics {
        private final boolean found;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int averageR;
        private final int averageG;
        private final int averageB;
        private final int centerR;
        private final int centerG;
        private final int centerB;
        private final double brightness;
        private final double edgeDensity;

        public RegionMetrics(boolean found, int x, int y, int width, int height, int averageR, int averageG, int averageB,
                int centerR, int centerG, int centerB, double brightness, double edgeDensity) {
            this.found = found;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.averageR = averageR;
            this.averageG = averageG;
            this.averageB = averageB;
            this.centerR = centerR;
            this.centerG = centerG;
            this.centerB = centerB;
            this.brightness = brightness;
            this.edgeDensity = edgeDensity;
        }

        public boolean isFound() { return found; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getAverageR() { return averageR; }
        public int getAverageG() { return averageG; }
        public int getAverageB() { return averageB; }
        public int getCenterR() { return centerR; }
        public int getCenterG() { return centerG; }
        public int getCenterB() { return centerB; }
        public double getBrightness() { return brightness; }
        public double getEdgeDensity() { return edgeDensity; }
        public String getAverageHex() { return String.format("#%02X%02X%02X", averageR, averageG, averageB); }
        public String getCenterHex() { return String.format("#%02X%02X%02X", centerR, centerG, centerB); }
    }

    public static final class TemplateMatchResult {
        private final boolean found;
        private final double similarity;
        private final int width;
        private final int height;

        public TemplateMatchResult(boolean found, double similarity, int width, int height) {
            this.found = found;
            this.similarity = similarity;
            this.width = width;
            this.height = height;
        }

        public boolean isFound() { return found; }
        public double getSimilarity() { return similarity; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    private ScreenVisionUtils() {
    }

    public static RegionMetrics analyzeRegion(int x, int y, int width, int height) {
        return new RegionMetrics(false, x, y, width, height, 0, 0, 0, 0, 0, 0, 0D, 0D);
    }

    public static TemplateMatchResult compareRegionToTemplate(int x, int y, int width, int height, String imagePath) {
        return new TemplateMatchResult(false, 0D, width, height);
    }

    public static int parseColor(String value, int fallback) {
        try {
            String normalized = value == null ? "" : value.trim();
            if (normalized.startsWith("#")) {
                normalized = normalized.substring(1);
            }
            return Integer.parseInt(normalized, 16);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static double colorDistance(int a, int b) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int dr = ar - br;
        int dg = ag - bg;
        int db = ab - bb;
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
