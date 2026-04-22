package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public final class UpdateManager {

    private static final String PRIMARY_URL_KEY = "update_primary.url";
    private static final String PASSWORD = "fenda";
    private static final String MOBILE_USER_AGENT = SharechainPageParser.MOBILE_USER_AGENT;

    private UpdateManager() {
    }

    public static void fetchUpdateLinkAndOpen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        mc.player.sendSystemMessage(new TextComponentString(ChatFormatting.AQUA + I18n.format("msg.update.fetching")));

        Thread thread = new Thread(() -> {
            try {
                String primaryUrl = SharechainLinkConfig.getRequiredUrl(PRIMARY_URL_KEY);
                String downloadLink = SharechainPageParser.fetchBestFirstUrl(primaryUrl, MOBILE_USER_AGENT, 15000);
                zszlScriptMod.LOGGER.info("[UpdateManager] Extracted download link: {}", downloadLink);

                if (downloadLink == null || !downloadLink.startsWith("http")) {
                    throw new Exception(I18n.format("msg.update.error.no_valid_link") + downloadLink);
                }

                final String finalDownloadLink = downloadLink;
                mc.execute(() -> {
                    try {
                        ExternalLinkOpener.OpenResult openResult =
                                ExternalLinkOpener.copyAndOpen(finalDownloadLink, "update-download");
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(new TextComponentString(
                                    ChatFormatting.AQUA + I18n.format("msg.update.password_copied", PASSWORD)));
                        }
                        if (openResult.opened()) {
                            if (mc.player != null) {
                                mc.player.sendSystemMessage(new TextComponentString(
                                        ChatFormatting.AQUA + I18n.format("msg.update.opening_browser")));
                            }
                        } else if (mc.player != null) {
                            mc.player.sendSystemMessage(new TextComponentString(
                                    ChatFormatting.RED + I18n.format("msg.update.browser_unsupported")));
                        }
                    } catch (Exception e) {
                        zszlScriptMod.LOGGER.error("Failed to open link or copy update URL on main thread", e);
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(new TextComponentString(
                                    ChatFormatting.RED + I18n.format("msg.update.operation_failed")));
                        }
                    }
                });
            } catch (Throwable t) {
                zszlScriptMod.LOGGER.error("Failed to fetch update link", t);
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(new TextComponentString(
                                ChatFormatting.RED + I18n.format("msg.update.fetch_failed")));
                    }
                });
            }
        }, "UpdateManager-Fetch");
        thread.setDaemon(true);
        thread.start();
    }
}
