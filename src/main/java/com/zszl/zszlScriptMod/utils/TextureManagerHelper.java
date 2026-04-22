package com.zszl.zszlScriptMod.utils;

import com.mojang.blaze3d.platform.NativeImage;
import com.zszl.zszlScriptMod.config.ChatOptimizationConfig.ImageQuality;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class TextureManagerHelper {

    private static final String BUILTIN_SCHEME = "builtin:";
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, int[]> TEXTURE_SIZE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_ACCESS_AT = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Void>> LOADING_TASKS = new ConcurrentHashMap<>();
    private static final Map<String, Long> FAILED_AT = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED_SOURCE_DIAGNOSTICS =
            java.util.Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final long RETRY_COOLDOWN_MS = 10_000L;
    private static final long IDLE_EVICT_MS = 120_000L;
    private static final int MAX_CACHE_ENTRIES = 12;
    private static final long EVICT_CHECK_INTERVAL_MS = 5_000L;

    private static final ExecutorService IO_POOL = Executors.newFixedThreadPool(2, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "zszl-texture-loader");
            thread.setDaemon(true);
            return thread;
        }
    });

    private static volatile boolean imageIOReady;
    private static volatile long lastEvictCheckAt;

    private TextureManagerHelper() {
    }

    private static Path getDiskCacheDir() {
        return ProfileManager.getCurrentProfileDir().resolve("theme_image_cache");
    }

    public static Path getThemeImageCacheDir() {
        Path dir = getDiskCacheDir();
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.warn("Failed to create theme image cache dir: {}", dir, e);
        }
        return dir;
    }

    public static String canonicalizeImagePath(String path) {
        String source = normalizeInputPath(path);
        if (source.isEmpty() || isBuiltinResourcePath(source) || !isHttpUrl(source)) {
            return source;
        }
        Path cached = getCachedFilePathForUrl(source);
        if (cached != null && Files.exists(cached)) {
            return cached.toAbsolutePath().toString();
        }
        return source;
    }

    public static ResourceLocation getResourceLocationForPath(String path, ImageQuality quality) {
        ensureImageIOPlugins();
        String normalizedInput = normalizeInputPath(path);
        if (normalizedInput.isEmpty()) {
            return null;
        }

        String source = canonicalizeImagePath(normalizedInput);
        String cacheKey = source + "::" + quality.name();
        ResourceLocation cached = TEXTURE_CACHE.get(cacheKey);
        if (cached != null) {
            LAST_ACCESS_AT.put(cacheKey, System.currentTimeMillis());
            maybeEvictCache(false);
            return cached;
        }

        if (isInRetryCooldown(cacheKey)) {
            return null;
        }

        LOADING_TASKS.computeIfAbsent(cacheKey, key -> startAsyncLoad(key, source, quality));
        maybeEvictCache(false);
        return null;
    }

    public static void prefetch(String path, ImageQuality quality) {
        getResourceLocationForPath(path, quality);
    }

    public static void unloadTexture(String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        String source = canonicalizeImagePath(path);
        Minecraft mc = Minecraft.getInstance();
        for (ImageQuality quality : ImageQuality.values()) {
            String cacheKey = source + "::" + quality.name();
            ResourceLocation location = TEXTURE_CACHE.remove(cacheKey);
            LAST_ACCESS_AT.remove(cacheKey);
            TEXTURE_SIZE_CACHE.remove(cacheKey);
            FAILED_AT.remove(cacheKey);
            LOADING_TASKS.remove(cacheKey);
            if (location != null && mc != null) {
                mc.execute(() -> mc.getTextureManager().release(location));
            }
        }
    }

    public static int[] getTextureSizeForPath(String path, ImageQuality quality) {
        String normalizedInput = normalizeInputPath(path);
        if (normalizedInput.isEmpty()) {
            return null;
        }
        String cacheKey = canonicalizeImagePath(normalizedInput) + "::" + quality.name();
        int[] size = TEXTURE_SIZE_CACHE.get(cacheKey);
        if (size == null || size.length < 2) {
            return null;
        }
        return new int[] { Math.max(1, size[0]), Math.max(1, size[1]) };
    }

    public static void clearCache() {
        if (TEXTURE_CACHE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation location : TEXTURE_CACHE.values()) {
            if (location != null && mc != null) {
                mc.execute(() -> mc.getTextureManager().release(location));
            }
        }
        TEXTURE_CACHE.clear();
        LAST_ACCESS_AT.clear();
        TEXTURE_SIZE_CACHE.clear();
        FAILED_AT.clear();
        LOADING_TASKS.clear();
        zszlScriptMod.LOGGER.info("Cleared all chat background caches and unloaded textures from GPU memory");
    }

    private static CompletableFuture<Void> startAsyncLoad(String cacheKey, String source, ImageQuality quality) {
        return CompletableFuture.runAsync(() -> {
            try {
                BufferedImage originalImage = loadImage(source);
                if (originalImage == null) {
                    FAILED_AT.put(cacheKey, System.currentTimeMillis());
                    zszlScriptMod.LOGGER.warn("Background image load failed: {}", source);
                    return;
                }

                BufferedImage processedImage = processImage(originalImage, quality);
                NativeImage nativeImage = toNativeImage(processedImage);
                if (nativeImage == null) {
                    FAILED_AT.put(cacheKey, System.currentTimeMillis());
                    zszlScriptMod.LOGGER.warn("Native image conversion failed: {}", source);
                    return;
                }

                Minecraft mc = Minecraft.getInstance();
                if (mc == null) {
                    nativeImage.close();
                    FAILED_AT.put(cacheKey, System.currentTimeMillis());
                    return;
                }

                mc.execute(() -> {
                    try {
                        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                        String textureName = "chat_bg/" + Integer.toHexString(source.hashCode()) + "_"
                                + quality.name().toLowerCase(Locale.ROOT);
                        ResourceLocation location = mc.getTextureManager().register(textureName, dynamicTexture);
                        TEXTURE_CACHE.put(cacheKey, location);
                        TEXTURE_SIZE_CACHE.put(cacheKey, new int[] {
                                Math.max(1, processedImage.getWidth()),
                                Math.max(1, processedImage.getHeight())
                        });
                        LAST_ACCESS_AT.put(cacheKey, System.currentTimeMillis());
                        FAILED_AT.remove(cacheKey);
                        maybeEvictCache(true);
                    } catch (Exception e) {
                        FAILED_AT.put(cacheKey, System.currentTimeMillis());
                        zszlScriptMod.LOGGER.error("GPU texture upload failed: {}", source, e);
                    }
                });
            } catch (Exception e) {
                FAILED_AT.put(cacheKey, System.currentTimeMillis());
                zszlScriptMod.LOGGER.error("Error while async loading background image source: {}", source, e);
            }
        }, IO_POOL).whenComplete((ignored, throwable) -> LOADING_TASKS.remove(cacheKey));
    }

    private static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = NativeImageHelper.fromBufferedImage(image);
        if (nativeImage == null) {
            zszlScriptMod.LOGGER.warn("Failed to convert buffered image to NativeImage");
        }
        return nativeImage;
    }

    private static boolean isInRetryCooldown(String cacheKey) {
        Long lastFail = FAILED_AT.get(cacheKey);
        return lastFail != null && (System.currentTimeMillis() - lastFail) < RETRY_COOLDOWN_MS;
    }

    private static BufferedImage loadImage(String source) throws Exception {
        ensureImageIOPlugins();
        if (isBuiltinResourcePath(source)) {
            return readBuiltinResourceImage(source);
        }
        if (isHttpUrl(source)) {
            Path cached = getCachedFilePathForUrl(source);
            if (cached != null && Files.exists(cached) && Files.isReadable(cached)) {
                BufferedImage local = readImageFromFile(cached.toFile());
                if (local != null) {
                    return local;
                }
            }

            URL url = new URL(source);
            URLConnection connection = HttpsCompat.openConnection(url);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            applyCommonRequestHeaders(connection, url);
            if (connection instanceof HttpURLConnection http) {
                http.setInstanceFollowRedirects(true);
                http.setRequestMethod("GET");
            }

            try (InputStream in = connection.getInputStream()) {
                byte[] bytes = readAllBytes(in);
                if (bytes.length == 0) {
                    return null;
                }
                logSourceDiagnostics(source, "url", bytes);
                cacheDownloadedBytes(source, bytes);
                return decodeImageBytes(bytes);
            }
        }

        File file = new File(source);
        if (!file.exists() || !file.canRead()) {
            zszlScriptMod.LOGGER.warn("Background image file does not exist or is unreadable: {}", source);
            return null;
        }
        return readImageFromFile(file);
    }

    private static BufferedImage readBuiltinResourceImage(String source) throws Exception {
        String resourcePath = toBuiltinResourcePath(source);
        if (resourcePath.isEmpty()) {
            return null;
        }
        try (InputStream in = openBuiltinResourceStream(resourcePath)) {
            if (in == null) {
                zszlScriptMod.LOGGER.warn("Built-in theme image resource not found: {} (tried classpath/assets/build/src fallbacks)",
                        resourcePath);
                return null;
            }
            byte[] bytes = readAllBytes(in);
            if (bytes.length == 0) {
                zszlScriptMod.LOGGER.warn("Built-in theme image resource is empty: {}", resourcePath);
                return null;
            }
            logSourceDiagnostics(resourcePath, "builtin", bytes);
            BufferedImage decoded = decodeImageBytes(bytes);
            if (decoded == null) {
                zszlScriptMod.LOGGER.warn(
                        "Built-in theme image decode failed: {} (bytes={}, head={}, webpHeader={})",
                        resourcePath, bytes.length, toHexHead(bytes, 16), hasWebpHeader(bytes));
            }
            return decoded;
        }
    }

    private static InputStream openBuiltinResourceStream(String resourcePath) {
        String normalized = resourcePath == null ? "" : resourcePath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return null;
        }

        String[] candidates = new String[] {
                normalized,
                "assets/zszl_script/" + normalized
        };

        ClassLoader loader = TextureManagerHelper.class.getClassLoader();
        for (String candidate : candidates) {
            try {
                InputStream stream = loader.getResourceAsStream(candidate);
                if (stream != null) {
                    return stream;
                }
            } catch (Exception ignored) {
            }
        }

        Path[] fileCandidates = new Path[] {
                Path.of("src", "main", "resources", normalized),
                Path.of("src", "main", "resources", "assets", "zszl_script", normalized),
                Path.of("build", "resources", "main", normalized),
                Path.of("build", "resources", "main", "assets", "zszl_script", normalized),
                Path.of("..", "src", "main", "resources", normalized),
                Path.of("..", "src", "main", "resources", "assets", "zszl_script", normalized),
                Path.of("..", "build", "resources", "main", normalized),
                Path.of("..", "build", "resources", "main", "assets", "zszl_script", normalized)
        };

        for (Path candidate : fileCandidates) {
            try {
                if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                    return Files.newInputStream(candidate);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static void applyCommonRequestHeaders(URLConnection connection, URL url) {
        if (connection == null) {
            return;
        }

        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");

        if (url == null) {
            return;
        }

        String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("haowallpaper.com") || host.endsWith(".haowallpaper.com")) {
            connection.setRequestProperty("Referer", "https://haowallpaper.com/homeView");
            connection.setRequestProperty("Origin", "https://haowallpaper.com");
        }
    }

    private static BufferedImage readImageFromFile(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            logSourceDiagnostics(file.getAbsolutePath(), "file", bytes);
            BufferedImage decoded = decodeImageBytes(bytes);
            if (decoded == null) {
                zszlScriptMod.LOGGER.warn("Image decode failed for file: {} (head={})",
                        file.getAbsolutePath(), toHexHead(bytes, 16));
            }
            return decoded;
        } catch (Exception e) {
            zszlScriptMod.LOGGER.warn("Image decode exception for file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private static BufferedImage decodeImageBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image != null) {
                return image;
            }
        } catch (Exception ignored) {
        }

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (imageInputStream == null) {
                return null;
            }
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInputStream, true, true);
                    BufferedImage image = reader.read(0);
                    if (image != null) {
                        return image;
                    }
                } catch (Exception ignored) {
                } finally {
                    try {
                        reader.dispose();
                    } catch (Exception ignored) {
                    }
                    try {
                        imageInputStream.seek(0);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        BufferedImage luciad = decodeWithExplicitWebpSpi(bytes, "com.luciad.imageio.webp.WebPImageReaderSpi");
        if (luciad != null) {
            return luciad;
        }
        return decodeWithExplicitWebpSpi(bytes, "com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi");
    }

    private static BufferedImage decodeWithExplicitWebpSpi(byte[] bytes, String spiClassName) {
        try {
            Class<?> type = Class.forName(spiClassName);
            Object spiObject = type.getDeclaredConstructor().newInstance();
            if (!(spiObject instanceof ImageReaderSpi spi)) {
                return null;
            }
            ImageReader reader = spi.createReaderInstance();
            try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                if (imageInputStream == null) {
                    return null;
                }
                reader.setInput(imageInputStream, true, true);
                return reader.read(0);
            } finally {
                try {
                    reader.dispose();
                } catch (Exception ignored) {
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static synchronized void ensureImageIOPlugins() {
        if (imageIOReady) {
            return;
        }
        try {
            boolean luciad = registerWebpReaderSpi("com.luciad.imageio.webp.WebPImageReaderSpi");
            boolean twelveMonkeys = registerWebpReaderSpi("com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi");
            zszlScriptMod.LOGGER.info("WEBP reader registration status: luciad={}, twelvemonkeys={}",
                    luciad, twelveMonkeys);
        } catch (Throwable t) {
            zszlScriptMod.LOGGER.warn("ImageIO plugin init failed", t);
        } finally {
            imageIOReady = true;
        }
    }

    private static boolean registerWebpReaderSpi(String className) {
        try {
            Class<?> spiClass = Class.forName(className);
            Object spi = spiClass.getDeclaredConstructor().newInstance();
            IIORegistry.getDefaultInstance().registerServiceProvider(spi);
            zszlScriptMod.LOGGER.info("Registered WEBP ImageReader SPI: {}", className);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static BufferedImage processImage(BufferedImage original, ImageQuality quality) {
        if (quality == ImageQuality.ORIGINAL) {
            return original;
        }

        int targetWidth;
        Object interpolation;
        switch (quality) {
        case HIGH:
            targetWidth = 1920;
            interpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
            break;
        case MEDIUM:
            targetWidth = 854;
            interpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            break;
        case LOW:
            targetWidth = 426;
            interpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            break;
        default:
            return original;
        }

        if (original.getWidth() <= targetWidth) {
            return original;
        }

        double aspectRatio = (double) original.getHeight() / (double) original.getWidth();
        int targetHeight = Math.max(1, (int) Math.round(targetWidth * aspectRatio));

        Image scaled = original.getScaledInstance(targetWidth, targetHeight,
                quality == ImageQuality.HIGH ? Image.SCALE_SMOOTH : Image.SCALE_REPLICATE);
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
            graphics.drawImage(scaled, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private static boolean isHttpUrl(String source) {
        String normalized = source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private static boolean isBuiltinResourcePath(String source) {
        String normalized = source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(BUILTIN_SCHEME);
    }

    private static String toBuiltinResourcePath(String source) {
        String normalized = source == null ? "" : source.trim();
        if (!isBuiltinResourcePath(normalized)) {
            return "";
        }
        String resourcePath = normalized.substring(BUILTIN_SCHEME.length()).trim();
        while (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        return resourcePath;
    }

    private static String normalizeInputPath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[8192];
        int read;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static Path getCachedFilePathForUrl(String source) {
        if (!isHttpUrl(source)) {
            return null;
        }
        try {
            String digest = sha1(source);
            return getDiskCacheDir().resolve(digest + ".img");
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to build cache file path for url: {}", source, e);
            return null;
        }
    }

    private static void cacheDownloadedBytes(String source, byte[] bytes) {
        try {
            Path file = getCachedFilePathForUrl(source);
            if (file == null) {
                return;
            }
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            Files.write(tmp, bytes);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.warn("Failed to persist downloaded image cache: {}", source, e);
        }
    }

    private static String sha1(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static String toHexHead(byte[] bytes, int max) {
        if (bytes == null || bytes.length == 0) {
            return "empty";
        }
        int length = Math.min(max, bytes.length);
        StringBuilder builder = new StringBuilder(length * 3);
        for (int i = 0; i < length; i++) {
            builder.append(String.format("%02X", bytes[i] & 0xFF));
            if (i < length - 1) {
                builder.append('-');
            }
        }
        return builder.toString();
    }

    private static void logSourceDiagnostics(String source, String origin, byte[] bytes) {
        String key = origin + "::" + source;
        if (!LOGGED_SOURCE_DIAGNOSTICS.add(key)) {
            return;
        }
        zszlScriptMod.LOGGER.info(
                "Theme image source detected: origin={} source={} bytes={} format={} head={}",
                origin,
                source,
                bytes == null ? 0 : bytes.length,
                describeImageFormat(bytes),
                toHexHead(bytes, 16));
    }

    private static String describeImageFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return "unknown";
        }
        if (hasWebpHeader(bytes)) {
            return "webp";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "jpeg";
        }
        if ((bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "png";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "gif";
        }
        return "unknown";
    }

    private static boolean hasWebpHeader(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private static void maybeEvictCache(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && (now - lastEvictCheckAt) < EVICT_CHECK_INTERVAL_MS) {
            return;
        }
        lastEvictCheckAt = now;
        if (TEXTURE_CACHE.isEmpty()) {
            return;
        }

        List<String> keysToEvict = new ArrayList<>();
        for (Map.Entry<String, ResourceLocation> entry : TEXTURE_CACHE.entrySet()) {
            String key = entry.getKey();
            long lastAccess = LAST_ACCESS_AT.getOrDefault(key, 0L);
            if ((now - lastAccess) > IDLE_EVICT_MS) {
                keysToEvict.add(key);
            }
        }

        int expectedSize = TEXTURE_CACHE.size() - keysToEvict.size();
        if (expectedSize > MAX_CACHE_ENTRIES) {
            int overflow = expectedSize - MAX_CACHE_ENTRIES;
            List<String> candidates = new ArrayList<>(TEXTURE_CACHE.keySet());
            candidates.removeAll(keysToEvict);
            candidates.sort(Comparator.comparingLong(key -> LAST_ACCESS_AT.getOrDefault(key, 0L)));
            for (int i = 0; i < overflow && i < candidates.size(); i++) {
                keysToEvict.add(candidates.get(i));
            }
        }

        if (keysToEvict.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        for (String key : keysToEvict) {
            ResourceLocation location = TEXTURE_CACHE.remove(key);
            LAST_ACCESS_AT.remove(key);
            TEXTURE_SIZE_CACHE.remove(key);
            FAILED_AT.remove(key);
            if (location != null && mc != null) {
                mc.execute(() -> mc.getTextureManager().release(location));
            }
        }
    }
}
