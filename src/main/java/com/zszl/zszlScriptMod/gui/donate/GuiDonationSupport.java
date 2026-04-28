package com.zszl.zszlScriptMod.gui.donate;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.DonationLeaderboardManager;
import com.zszl.zszlScriptMod.utils.NativeImageHelper;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class GuiDonationSupport extends ThemedGuiScreen {

    private static final int BTN_BACK = 0;
    private static final int BTN_REFRESH = 1;
    private static final int DESC_TEXT_COLOR = 0xFFFFFFFF;
    private static final int DESC_TEXT_BG = 0x99000000;

    private final GuiScreen parentScreen;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int leftX;
    private int leftY;
    private int leftW;
    private int leftH;
    private int rightX;
    private int rightY;
    private int rightW;
    private int rightH;
    private int qrBoxSize;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private Identifier qrTexture;
    private int qrTexW;
    private int qrTexH;

    public GuiDonationSupport(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    private void returnToMainMenuOverlay() {
        if (parentScreen != null) {
            mc.setScreen(parentScreen);
            return;
        }
        GuiInventory.openOverlayScreen();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        panelWidth = Math.min(680, this.width - 30);
        panelHeight = Math.min(380, this.height - 24);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int contentY = panelY + 34;
        int contentH = panelHeight - 72;
        int gap = 12;

        leftW = Math.max(220, (int) (panelWidth * 0.43f));
        rightW = panelWidth - leftW - gap - 20;
        leftX = panelX + 10;
        rightX = leftX + leftW + gap;
        leftY = contentY;
        rightY = contentY;
        leftH = contentH;
        rightH = contentH;
        qrBoxSize = Math.min(leftW - 20, 170);

        this.buttonList.add(new ThemedButton(BTN_BACK, panelX + panelWidth / 2 - 110, panelY + panelHeight - 28,
                100, 20, I18n.format("gui.common.back")));
        this.buttonList.add(new ThemedButton(BTN_REFRESH, panelX + panelWidth / 2 + 10, panelY + panelHeight - 28,
                100, 20, I18n.format("gui.donate.refresh")));

        DonationLeaderboardManager.fetchContent();
        loadQrTexture();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_BACK) {
            returnToMainMenuOverlay();
            return;
        }
        if (button.id == BTN_REFRESH) {
            DonationLeaderboardManager.forceRefresh();
            loadQrTexture();
        }
    }

    private void loadQrTexture() {
        releaseQrTexture();

        try (InputStream input = DonationLeaderboardManager.class.getClassLoader()
                .getResourceAsStream(DonationLeaderboardManager.PAYMENT_QR_RESOURCE)) {
            if (input == null) {
                zszlScriptMod.LOGGER.warn("[Donation] 未找到内置付款码资源: {}",
                        DonationLeaderboardManager.PAYMENT_QR_RESOURCE);
                return;
            }

            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                zszlScriptMod.LOGGER.warn("[Donation] 内置付款码图片解码失败: {}",
                        DonationLeaderboardManager.PAYMENT_QR_RESOURCE);
                return;
            }

            qrTexW = image.getWidth();
            qrTexH = image.getHeight();
            NativeImage nativeImage = toNativeImage(image);
            if (nativeImage == null) {
                zszlScriptMod.LOGGER.warn("[Donation] 内置付款码转换 NativeImage 失败: {}",
                        DonationLeaderboardManager.PAYMENT_QR_RESOURCE);
                qrTexW = 0;
                qrTexH = 0;
                return;
            }

            try {
                qrTexture = Identifier.fromNamespaceAndPath(zszlScriptMod.MODID, "donation_qr");
                mc.getTextureManager().register(qrTexture, new DynamicTexture(() -> "zszl_donation_qr", nativeImage));
                zszlScriptMod.LOGGER.info("[Donation] 内置付款码加载成功: resource={} size={}x{}",
                        DonationLeaderboardManager.PAYMENT_QR_RESOURCE, qrTexW, qrTexH);
            } catch (Exception e) {
                nativeImage.close();
                qrTexW = 0;
                qrTexH = 0;
                zszlScriptMod.LOGGER.warn("[Donation] 内置付款码动态纹理注册失败: {}",
                        DonationLeaderboardManager.PAYMENT_QR_RESOURCE, e);
            }
        } catch (Exception e) {
            qrTexW = 0;
            qrTexH = 0;
            zszlScriptMod.LOGGER.warn("[Donation] 读取内置付款码图片失败: {}",
                    DonationLeaderboardManager.PAYMENT_QR_RESOURCE, e);
        }
    }

    private static NativeImage toNativeImage(BufferedImage image) {
        return NativeImageHelper.fromBufferedImage(image);
    }

    private void releaseQrTexture() {
        qrTexW = 0;
        qrTexH = 0;
        if (qrTexture != null && mc != null) {
            mc.getTextureManager().release(qrTexture);
        }
        qrTexture = null;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }

        int mouseX = 0;
        int mouseY = 0;
        if (this.mc != null && this.width > 0 && this.height > 0) {
            mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getWidth());
            mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getHeight()) - 1;
        }
        if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= rightY && mouseY <= rightY + rightH) {
            if (dWheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.donate.title"), this.fontRenderer);

        drawRect(leftX, leftY, leftX + leftW, leftY + leftH, 0x66000000);
        drawCenteredString(this.fontRenderer, I18n.format("gui.donate.qr_title"), leftX + leftW / 2, leftY + 6,
                0xFFFFFF);

        int qrX = leftX + (leftW - qrBoxSize) / 2;
        int qrY = leftY + 22;

        drawRect(qrX - 1, qrY - 1, qrX + qrBoxSize + 1, qrY + qrBoxSize + 1, 0xFFB0B0B0);
        drawRect(qrX, qrY, qrX + qrBoxSize, qrY + qrBoxSize, 0xFFFFFFFF);

        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null && qrTexture != null && qrTexW > 0 && qrTexH > 0) {
            int drawWidth = qrBoxSize;
            int drawHeight = qrBoxSize;
            if (qrTexW > 0 && qrTexH > 0) {
                double scale = Math.min(qrBoxSize / (double) qrTexW, qrBoxSize / (double) qrTexH);
                drawWidth = Math.max(1, (int) Math.round(qrTexW * scale));
                drawHeight = Math.max(1, (int) Math.round(qrTexH * scale));
            }
            int drawX = qrX + (qrBoxSize - drawWidth) / 2;
            int drawY = qrY + (qrBoxSize - drawHeight) / 2;
            Gui.drawScaledCustomSizeModalRect(qrTexture, drawX, drawY, 0.0F, 0.0F, qrTexW, qrTexH, drawWidth,
                    drawHeight, qrTexW, qrTexH, 0xFFFFFFFF);
        } else {
            drawCenteredString(this.fontRenderer, I18n.format("gui.donate.qr_placeholder"), leftX + leftW / 2,
                    qrY + qrBoxSize / 2 - 4, 0x666666);
        }

        int textY = qrY + qrBoxSize + 10;
        String[] descLines = {
                I18n.format("gui.donate.desc.free"),
                I18n.format("gui.donate.desc.support"),
                I18n.format("gui.donate.desc.remark"),
                I18n.format("gui.donate.desc.realtime")
        };
        drawRect(leftX + 4, textY - 4, leftX + leftW - 4, textY + descLines.length * 14 + 1, DESC_TEXT_BG);
        for (int i = 0; i < descLines.length; i++) {
            drawCrispString(descLines[i], leftX + 8, textY + i * 14, DESC_TEXT_COLOR);
        }

        drawRect(rightX, rightY, rightX + rightW, rightY + rightH, 0x66000000);
        drawCenteredString(this.fontRenderer, I18n.format("gui.donate.rank_title"), rightX + rightW / 2,
                rightY + 6, 0xFFFFFF);

        int headerY = rightY + 24;
        int rankColW = 42;
        int amountColW = 68;
        int nameColW = rightW - rankColW - amountColW - 10;

        int rowStartX = rightX + 5;
        drawRect(rowStartX, headerY - 2, rowStartX + rightW - 10, headerY + 12, 0x55334455);
        drawString(this.fontRenderer, I18n.format("gui.donate.col.rank"), rowStartX + 4, headerY + 1, 0xFFFFFF);
        drawString(this.fontRenderer, I18n.format("gui.donate.col.name"), rowStartX + rankColW + 4, headerY + 1,
                0xFFFFFF);
        drawString(this.fontRenderer, I18n.format("gui.donate.col.amount"), rowStartX + rankColW + nameColW + 4,
                headerY + 1, 0xFFFFFF);

        List<DonationLeaderboardManager.Entry> entries = DonationLeaderboardManager.leaderboard;
        int listTop = headerY + 16;
        int rowHeight = 14;
        int visibleRows = Math.max(1, (rightH - 44) / rowHeight);
        maxScroll = Math.max(0, entries.size() - visibleRows);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        if (entries.isEmpty()) {
            drawCenteredString(this.fontRenderer, I18n.format("gui.donate.rank_loading"), rightX + rightW / 2,
                    listTop + 30, 0xAAAAAA);
        } else {
            for (int i = 0; i < visibleRows; i++) {
                int idx = i + scrollOffset;
                if (idx >= entries.size()) {
                    break;
                }
                DonationLeaderboardManager.Entry entry = entries.get(idx);
                int y = listTop + i * rowHeight;
                if ((idx & 1) == 0) {
                    drawRect(rowStartX, y - 1, rowStartX + rightW - 10, y + rowHeight - 1, 0x22111111);
                }
                drawString(this.fontRenderer, String.valueOf(entry.rank), rowStartX + 8, y + 2, 0xFFFFFF);
                drawString(this.fontRenderer, entry.name, rowStartX + rankColW + 4, y + 2, 0xFFEFD280);
                drawString(this.fontRenderer, entry.amount, rowStartX + rankColW + nameColW + 4, y + 2, 0xFF98FB98);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCrispString(String text, int x, int y, int color) {
        if (this.fontRenderer == null) {
            return;
        }
        this.fontRenderer.drawStringWithShadow(text == null ? "" : text, (float) x, (float) y, color);
    }

    @Override
    public void onGuiClosed() {
        releaseQrTexture();
        super.onGuiClosed();
    }
}
