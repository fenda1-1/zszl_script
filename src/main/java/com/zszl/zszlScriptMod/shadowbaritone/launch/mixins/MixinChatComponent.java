package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.config.ChatOptimizationConfig;
import com.zszl.zszlScriptMod.handlers.ChatEventHandler;
import com.zszl.zszlScriptMod.utils.TextureManagerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow private boolean newMessageSinceScroll;

    @Shadow protected abstract void logChatMessage(Component message, @Nullable GuiMessageTag tag);
    @Shadow protected abstract boolean isChatHidden();
    @Shadow protected abstract int getTagIconLeft(GuiMessage.Line line);
    @Shadow protected abstract void drawTagIcon(GuiGraphics graphics, int x, int y, GuiMessageTag.Icon icon);
    @Shadow protected abstract boolean isChatFocused();
    @Shadow public abstract int getWidth();
    @Shadow public abstract int getLinesPerPage();
    @Shadow public abstract double getScale();
    @Shadow public abstract void resetChatScroll();
    @Shadow public abstract void scrollChat(int amount);
    @Shadow protected abstract int getLineHeight();
    @Shadow protected abstract void refreshTrimmedMessage();

    @Unique private static final Map<String, Integer> zszl$spamCounts = new ConcurrentHashMap<>();
    @Unique private static final Map<String, Long> zszl$spamTimes = new ConcurrentHashMap<>();
    @Unique private static final Map<String, GuiMessage> zszl$spamMessages = new ConcurrentHashMap<>();
    @Unique private static final Map<String, Pattern> zszl$patternCache = new ConcurrentHashMap<>();
    @Unique private static final SimpleDateFormat zszl$timeFormat = new SimpleDateFormat("HH:mm:ss");
    @Unique private static float zszl$percentComplete = 1.0F;
    @Unique private static int zszl$newLines = 0;
    @Unique private static long zszl$prevMillis = -1L;

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zszl$handleAddMessage(Component message, @Nullable MessageSignature signature,
            @Nullable GuiMessageTag tag, CallbackInfo ci) {
        ChatOptimizationConfig settings = ChatOptimizationConfig.INSTANCE;
        Component originalMessage = message == null ? Component.empty() : message;
        String rawText = originalMessage.getString();
        long now = System.currentTimeMillis();

        zszl$cleanupSpamTracker(now, settings);
        if (zszl$shouldFilter(rawText, settings)) {
            ci.cancel();
            return;
        }

        Component displayMessage = originalMessage;
        if (settings.enableAntiSpam && !rawText.isEmpty()) {
            Long lastTime = zszl$spamTimes.get(rawText);
            GuiMessage previousMessage = zszl$spamMessages.get(rawText);
            if (lastTime != null
                    && (now - lastTime) < Math.max(1, settings.antiSpamThresholdSeconds) * 1000L
                    && previousMessage != null
                    && this.allMessages.contains(previousMessage)) {
                int count = zszl$spamCounts.getOrDefault(rawText, 1) + 1;
                zszl$spamCounts.put(rawText, count);
                this.allMessages.remove(previousMessage);
                this.refreshTrimmedMessage();
                if (settings.antiSpamScrollToBottom) {
                    this.resetChatScroll();
                }
                MutableComponent merged = originalMessage.copy();
                merged.append(Component.literal(" [x" + count + "]").withStyle(ChatFormatting.GRAY));
                displayMessage = merged;
            } else {
                zszl$spamCounts.put(rawText, 1);
            }
        }

        if (settings.enableTimestamp) {
            MutableComponent timestamped = Component.literal("[" + zszl$timeFormat.format(new Date()) + "] ")
                    .withStyle(ChatFormatting.GRAY);
            timestamped.append(displayMessage);
            displayMessage = timestamped;
        }

        this.logChatMessage(displayMessage, tag);
        zszl$addProcessedMessage(displayMessage, signature, this.minecraft.gui.getGuiTicks(), tag, false);
        zszl$percentComplete = 0.0F;

        if (settings.enableAntiSpam && !rawText.isEmpty()) {
            zszl$spamTimes.put(rawText, now);
            if (this.allMessages.isEmpty()) {
                zszl$spamMessages.remove(rawText);
            } else {
                zszl$spamMessages.put(rawText, this.allMessages.get(0));
            }
        }

        boolean system = tag != null && "System".equalsIgnoreCase(Optionull.map(tag, GuiMessageTag::logTag));
        ChatEventHandler.triggerDisplayedChatMessage(originalMessage, displayMessage, system);
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void zszl$render(GuiGraphics graphics, int guiTicks, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.isChatHidden()) {
            ci.cancel();
            return;
        }

        int lineCount = this.getLinesPerPage();
        int trimmedCount = this.trimmedMessages.size();
        if (trimmedCount <= 0) {
            ci.cancel();
            return;
        }

        ChatOptimizationConfig settings = ChatOptimizationConfig.INSTANCE;
        boolean chatFocused = this.isChatFocused();
        float scale = (float) this.getScale();
        int chatWidth = Mth.ceil((float) this.getWidth() / scale);
        int guiHeight = graphics.guiHeight();
        double chatOpacity = this.minecraft.options.chatOpacity().get() * 0.9F + 0.1F;
        double backgroundOpacity = this.minecraft.options.textBackgroundOpacity().get();
        double lineSpacing = this.minecraft.options.chatLineSpacing().get();
        int lineHeight = this.getLineHeight();
        int textOffset = (int) Math.round(-8.0D * (lineSpacing + 1.0D) + 4.0D * lineSpacing);
        int visibleLines = 0;

        zszl$updateAnimation();

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(4.0F + (float) (settings.xOffset / Math.max(scale, 0.001F)),
                (float) (settings.yOffset / Math.max(scale, 0.001F)), 0.0F);

        int baseY = Mth.floor((float) (guiHeight - 40) / scale);
        int hoveredEndIndex = this.zszl$getMessageEndIndexAt(zszl$screenToChatX(mouseX), zszl$screenToChatY(mouseY));

        for (int visibleIndex = 0; visibleIndex + this.chatScrollbarPos < trimmedCount && visibleIndex < lineCount; visibleIndex++) {
            int lineIndex = visibleIndex + this.chatScrollbarPos;
            GuiMessage.Line line = this.trimmedMessages.get(lineIndex);
            if (line == null) {
                continue;
            }

            int age = guiTicks - line.addedTime();
            if (age >= 200 && !chatFocused) {
                continue;
            }

            double timeFactor = chatFocused ? 1.0D : zszl$getTimeFactor(age);
            int textAlpha = (int) (255.0D * timeFactor * chatOpacity);
            if (textAlpha <= 3) {
                continue;
            }

            int drawBottom = baseY - visibleIndex * lineHeight;
            int backgroundTop = drawBottom - lineHeight;
            int backgroundBottom = drawBottom;
            int backgroundWidth = chatWidth + 8;

            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 50.0F);
            zszl$drawBackground(graphics, settings, -4, backgroundTop, backgroundWidth, lineHeight,
                    textAlpha, backgroundOpacity);

            GuiMessageTag messageTag = line.tag();
            if (messageTag != null) {
                int indicatorColor = messageTag.indicatorColor() | (textAlpha << 24);
                graphics.fill(-4, backgroundTop, -2, backgroundBottom, indicatorColor);
                if (lineIndex == hoveredEndIndex && messageTag.icon() != null) {
                    int iconLeft = this.getTagIconLeft(line);
                    int iconBottom = drawBottom + textOffset + 9;
                    this.drawTagIcon(graphics, iconLeft, iconBottom, messageTag.icon());
                }
            }

            int finalY = drawBottom + textOffset;
            int finalAlpha = textAlpha;
            if (settings.smooth && !chatFocused && visibleIndex <= zszl$newLines) {
                float percent = zszl$percentComplete;
                finalY = drawBottom + textOffset + Math.round((1.0F - percent) * lineHeight);
                finalAlpha = Math.max(0, Math.min(255, Math.round(textAlpha * percent)));
            }
            graphics.drawString(this.minecraft.font, line.content(), 0, finalY, 0xFFFFFF | (finalAlpha << 24));
            graphics.pose().popPose();
            visibleLines++;
        }

        long queueSize = this.minecraft.getChatListener().queueSize();
        if (queueSize > 0L) {
            int queueAlpha = (int) (128.0D * chatOpacity);
            int queueBackgroundAlpha = (int) (255.0D * backgroundOpacity);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, (float) baseY, 50.0F);
            graphics.fill(-2, 0, chatWidth + 4, 9, queueBackgroundAlpha << 24);
            graphics.pose().translate(0.0F, 0.0F, 50.0F);
            graphics.drawString(this.minecraft.font, Component.translatable("chat.queue", queueSize), 0, 1,
                    0xFFFFFF | (queueAlpha << 24));
            graphics.pose().popPose();
        }

        if (chatFocused) {
            int visibleHeight = trimmedCount * lineHeight;
            int currentHeight = visibleLines * lineHeight;
            int scrollOffset = this.chatScrollbarPos * currentHeight / Math.max(1, trimmedCount) - baseY;
            int thumbHeight = currentHeight * currentHeight / Math.max(1, visibleHeight);
            if (visibleHeight != currentHeight) {
                int alpha = scrollOffset > 0 ? 170 : 96;
                int color = this.newMessageSinceScroll ? 13382451 : 3355562;
                int scrollX = chatWidth + 4;
                graphics.fill(scrollX, -scrollOffset, scrollX + 2, -scrollOffset - thumbHeight, color + (alpha << 24));
                graphics.fill(scrollX + 2, -scrollOffset, scrollX + 1, -scrollOffset - thumbHeight,
                        13421772 + (alpha << 24));
            }
        }

        graphics.pose().popPose();
        ci.cancel();
    }

    @Inject(method = "screenToChatX", at = @At("HEAD"), cancellable = true, remap = true)
    private void zszl$overrideScreenToChatX(double mouseX, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Double> cir) {
        ChatOptimizationConfig settings = ChatOptimizationConfig.INSTANCE;
        cir.setReturnValue(mouseX / this.getScale() - 4.0D - settings.xOffset / this.getScale());
    }

    @Inject(method = "screenToChatY", at = @At("HEAD"), cancellable = true, remap = true)
    private void zszl$overrideScreenToChatY(double mouseY,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Double> cir) {
        ChatOptimizationConfig settings = ChatOptimizationConfig.INSTANCE;
        double translated = (double) this.minecraft.getWindow().getGuiScaledHeight() - mouseY - 40.0D + settings.yOffset;
        cir.setReturnValue(translated / (this.getScale() * (double) this.getLineHeight()));
    }

    @Inject(method = "handleChatQueueClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void zszl$handleChatQueueClicked(double mouseX, double mouseY,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!this.isChatFocused() || this.minecraft.options.hideGui || this.isChatHidden()) {
            return;
        }
        long queueSize = this.minecraft.getChatListener().queueSize();
        if (queueSize == 0L) {
            return;
        }
        ChatOptimizationConfig settings = ChatOptimizationConfig.INSTANCE;
        double translatedX = mouseX - 2.0D - settings.xOffset;
        double translatedY = (double) this.minecraft.getWindow().getGuiScaledHeight() - mouseY - 40.0D + settings.yOffset;
        if (translatedX <= (double) Mth.floor((double) this.getWidth() / this.getScale())
                && translatedY < 0.0D
                && translatedY > (double) Mth.floor(-9.0D * this.getScale())) {
            this.minecraft.getChatListener().acceptNextDelayedMessage();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void zszl$addProcessedMessage(Component message, @Nullable MessageSignature signature, int addedTime,
            @Nullable GuiMessageTag tag, boolean displayOnly) {
        int width = Mth.floor((double) this.getWidth() / this.getScale());
        if (tag != null && tag.icon() != null) {
            width -= tag.icon().width + 6;
        }

        List<FormattedCharSequence> wrapped = ComponentRenderUtils.wrapComponents(message, width, this.minecraft.font);
        boolean focused = this.isChatFocused();
        zszl$newLines = Math.max(0, wrapped.size() - 1);

        for (int i = 0; i < wrapped.size(); i++) {
            FormattedCharSequence sequence = wrapped.get(i);
            if (focused && this.chatScrollbarPos > 0) {
                this.newMessageSinceScroll = true;
                this.scrollChat(1);
            }
            boolean endOfEntry = i == wrapped.size() - 1;
            this.trimmedMessages.add(0, new GuiMessage.Line(addedTime, sequence, tag, endOfEntry));
        }

        while (this.trimmedMessages.size() > 100) {
            this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
        }

        if (!displayOnly) {
            this.allMessages.add(0, new GuiMessage(addedTime, message, signature, tag));
            while (this.allMessages.size() > 100) {
                this.allMessages.remove(this.allMessages.size() - 1);
            }
        }
    }

    @Unique
    private void zszl$drawBackground(GuiGraphics graphics, ChatOptimizationConfig settings, int x, int y, int width,
            int height, int maxAlpha, double vanillaBackgroundOpacity) {
        if (width <= 0 || height <= 0) {
            return;
        }
        var background = TextureManagerHelper.getResourceLocationForPath(settings.backgroundImagePath, settings.imageQuality);
        float transparencyRatio = Mth.clamp(settings.backgroundTransparencyPercent / 100.0F, 0.0F, 1.0F);
        float opacityRatio = 1.0F - transparencyRatio;

        if (background != null) {
            int[] textureSize = TextureManagerHelper.getTextureSizeForPath(settings.backgroundImagePath, settings.imageQuality);
            int textureWidth = textureSize != null ? textureSize[0] : width;
            int textureHeight = textureSize != null ? textureSize[1] : height;
            int safeScale = Math.max(10, Math.min(300, settings.backgroundImageScale));
            float scaleFactor = 100.0F / safeScale;
            int sampleWidth = Math.max(1, Math.min(textureWidth, Math.round(width * scaleFactor)));
            int sampleHeight = Math.max(1, Math.min(textureHeight, Math.round(height * scaleFactor)));
            int u = Math.max(0, settings.backgroundCropX);
            int v = Math.max(0, settings.backgroundCropY);
            if (u + sampleWidth > textureWidth) {
                u = Math.max(0, textureWidth - sampleWidth);
            }
            if (v + sampleHeight > textureHeight) {
                v = Math.max(0, textureHeight - sampleHeight);
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (maxAlpha / 255.0F) * opacityRatio);
            graphics.blit(background, x, y, 0, (float) u, (float) v, width, height, textureWidth, textureHeight);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else if (settings.backgroundTransparencyPercent < 100) {
            int backgroundAlpha = (int) (((maxAlpha / 255.0D) * vanillaBackgroundOpacity * 255.0D) * opacityRatio);
            graphics.fill(x, y, x + width, y + height, backgroundAlpha << 24);
        }
    }

    @Unique
    private void zszl$cleanupSpamTracker(long now, ChatOptimizationConfig settings) {
        long ttl = Math.max(1, settings.antiSpamThresholdSeconds) * 2000L;
        List<String> staleKeys = new java.util.ArrayList<>();
        for (Map.Entry<String, Long> entry : zszl$spamTimes.entrySet()) {
            String key = entry.getKey();
            Long lastTime = entry.getValue();
            GuiMessage message = zszl$spamMessages.get(key);
            boolean expired = lastTime == null || (now - lastTime) > ttl;
            boolean staleMessage = message != null && !this.allMessages.contains(message);
            if (expired || staleMessage) {
                staleKeys.add(key);
            }
        }
        for (String key : staleKeys) {
            zszl$spamCounts.remove(key);
            zszl$spamTimes.remove(key);
            zszl$spamMessages.remove(key);
        }
    }

    @Unique
    private boolean zszl$shouldFilter(String rawText, ChatOptimizationConfig settings) {
        if (settings.enableBlacklist && settings.blacklist != null && !settings.blacklist.isEmpty()) {
            boolean blacklisted = settings.blacklist.stream()
                    .filter(filter -> filter != null && !filter.trim().isEmpty())
                    .anyMatch(filter -> zszl$matches(rawText, filter.trim(), settings.regexFilter));
            if (blacklisted) {
                return true;
            }
        }

        if (settings.enableWhitelist && settings.whitelist != null && !settings.whitelist.isEmpty()) {
            boolean whitelisted = settings.whitelist.stream()
                    .filter(filter -> filter != null && !filter.trim().isEmpty())
                    .anyMatch(filter -> zszl$matches(rawText, filter.trim(), settings.regexFilter));
            return !whitelisted;
        }

        return false;
    }

    @Unique
    private boolean zszl$matches(String text, String filter, boolean regex) {
        if (text == null) {
            return false;
        }
        if (!regex) {
            return text.contains(filter);
        }
        try {
            Pattern pattern = zszl$patternCache.computeIfAbsent(filter, Pattern::compile);
            return pattern.matcher(text).find();
        } catch (PatternSyntaxException ignored) {
            return text.contains(filter);
        }
    }

    @Unique
    private static double zszl$getTimeFactor(int age) {
        double value = (double) age / 200.0D;
        value = 1.0D - value;
        value *= 10.0D;
        value = Mth.clamp(value, 0.0D, 1.0D);
        return value * value;
    }

    @Unique
    private static void zszl$updateAnimation() {
        if (zszl$prevMillis == -1L) {
            zszl$prevMillis = System.currentTimeMillis();
            zszl$percentComplete = 1.0F;
            return;
        }
        long now = System.currentTimeMillis();
        long diff = now - zszl$prevMillis;
        zszl$prevMillis = now;
        if (zszl$percentComplete < 1.0F) {
            zszl$percentComplete += 0.004F * diff;
        }
        zszl$percentComplete = Mth.clamp(zszl$percentComplete, 0.0F, 1.0F);
    }

    @Unique
    private double zszl$screenToChatX(double mouseX) {
        ChatOptimizationConfig settings = ChatOptimizationConfig.INSTANCE;
        return mouseX / this.getScale() - 4.0D - settings.xOffset / this.getScale();
    }

    @Unique
    private double zszl$screenToChatY(double mouseY) {
        ChatOptimizationConfig settings = ChatOptimizationConfig.INSTANCE;
        double translated = (double) this.minecraft.getWindow().getGuiScaledHeight() - mouseY - 40.0D + settings.yOffset;
        return translated / (this.getScale() * (double) this.getLineHeight());
    }

    @Unique
    private int zszl$getMessageEndIndexAt(double chatX, double chatY) {
        int lineIndex = zszl$getMessageLineIndexAt(chatX, chatY);
        if (lineIndex == -1) {
            return -1;
        }

        while (lineIndex >= 0) {
            if (this.trimmedMessages.get(lineIndex).endOfEntry()) {
                return lineIndex;
            }
            lineIndex--;
        }
        return lineIndex;
    }

    @Unique
    private int zszl$getMessageLineIndexAt(double chatX, double chatY) {
        if (!this.isChatFocused() || this.minecraft.options.hideGui || this.isChatHidden()) {
            return -1;
        }
        if (chatX < -4.0D || chatX > (double) Mth.floor((double) this.getWidth() / this.getScale())) {
            return -1;
        }
        int visibleCount = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
        if (chatY < 0.0D || chatY >= (double) visibleCount) {
            return -1;
        }
        int lineIndex = Mth.floor(chatY + (double) this.chatScrollbarPos);
        return lineIndex >= 0 && lineIndex < this.trimmedMessages.size() ? lineIndex : -1;
    }
}
