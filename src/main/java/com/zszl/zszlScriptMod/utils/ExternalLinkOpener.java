package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.shadowbaritone.utils.accessor.IGuiScreen;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;

public final class ExternalLinkOpener {

    public enum OpenMethod {
        MINECRAFT_SCREEN,
        DESKTOP,
        SYSTEM_COMMAND,
        FAILED
    }

    public static final class OpenResult {
        public final URI uri;
        public final boolean clipboardCopied;
        public final OpenMethod method;
        public final String detail;

        private OpenResult(URI uri, boolean clipboardCopied, OpenMethod method, String detail) {
            this.uri = uri;
            this.clipboardCopied = clipboardCopied;
            this.method = method;
            this.detail = detail == null ? "" : detail;
        }

        public boolean opened() {
            return method != OpenMethod.FAILED;
        }
    }

    private ExternalLinkOpener() {
    }

    public static OpenResult copyAndOpen(String url, String context) {
        return open(url, true, context);
    }

    public static OpenResult open(String url, String context) {
        return open(url, false, context);
    }

    private static OpenResult open(String url, boolean copyToClipboard, String context) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.warn("[LinkOpener:{}] Invalid URI: {}", context, url, e);
            return new OpenResult(null, false, OpenMethod.FAILED, "invalid-uri");
        }

        boolean copied = false;
        if (copyToClipboard) {
            try {
                GuiScreen.setClipboardString(url);
                copied = true;
                zszlScriptMod.LOGGER.info("[LinkOpener:{}] Copied link to clipboard: {}", context, url);
            } catch (Throwable t) {
                zszlScriptMod.LOGGER.warn("[LinkOpener:{}] Failed to copy link to clipboard: {}", context, url, t);
            }
        }

        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof IGuiScreen guiScreen) {
            try {
                guiScreen.openLinkInvoker(uri);
                zszlScriptMod.LOGGER.info("[LinkOpener:{}] Opened via Minecraft screen: {}", context, url);
                return new OpenResult(uri, copied, OpenMethod.MINECRAFT_SCREEN, "minecraft-screen");
            } catch (Throwable t) {
                zszlScriptMod.LOGGER.warn("[LinkOpener:{}] Minecraft screen open failed: {}", context, url, t);
            }
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(uri);
                    zszlScriptMod.LOGGER.info("[LinkOpener:{}] Opened via Desktop browse: {}", context, url);
                    return new OpenResult(uri, copied, OpenMethod.DESKTOP, "desktop-browse");
                }
            } catch (Throwable t) {
                zszlScriptMod.LOGGER.warn("[LinkOpener:{}] Desktop browse failed: {}", context, url, t);
            }
        } else {
            zszlScriptMod.LOGGER.info("[LinkOpener:{}] Desktop browse unsupported for {}", context, url);
        }

        OpenResult systemResult = openViaSystemCommand(uri, copied, context);
        if (systemResult.opened()) {
            return systemResult;
        }

        zszlScriptMod.LOGGER.warn("[LinkOpener:{}] All open strategies failed for {}", context, url);
        return new OpenResult(uri, copied, OpenMethod.FAILED, systemResult.detail);
    }

    private static OpenResult openViaSystemCommand(URI uri, boolean copied, String context) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String url = uri.toString();

        if (osName.contains("win")) {
            if (startProcess(context, url, "cmd-start", "cmd", "/c", buildWindowsStartCommand(url))) {
                return new OpenResult(uri, copied, OpenMethod.SYSTEM_COMMAND, "cmd-start");
            }
            if (startProcess(context, url, "rundll32", "rundll32", "url.dll,FileProtocolHandler", url)) {
                return new OpenResult(uri, copied, OpenMethod.SYSTEM_COMMAND, "rundll32");
            }
            return new OpenResult(uri, copied, OpenMethod.FAILED, "windows-command-failed");
        }

        if (osName.contains("mac")) {
            if (startProcess(context, url, "open", "open", url)) {
                return new OpenResult(uri, copied, OpenMethod.SYSTEM_COMMAND, "open");
            }
            return new OpenResult(uri, copied, OpenMethod.FAILED, "mac-open-failed");
        }

        if (startProcess(context, url, "xdg-open", "xdg-open", url)) {
            return new OpenResult(uri, copied, OpenMethod.SYSTEM_COMMAND, "xdg-open");
        }
        return new OpenResult(uri, copied, OpenMethod.FAILED, "linux-open-failed");
    }

    private static String buildWindowsStartCommand(String url) {
        return "start \"\" \"" + url + "\"";
    }

    private static boolean startProcess(String context, String url, String label, String... command) {
        try {
            new ProcessBuilder(command).start();
            zszlScriptMod.LOGGER.info("[LinkOpener:{}] Opened via {}: {}", context, label, url);
            return true;
        } catch (Throwable t) {
            zszlScriptMod.LOGGER.warn("[LinkOpener:{}] {} failed for {}", context, label, url, t);
            return false;
        }
    }
}
