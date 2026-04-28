package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.config.ChatOptimizationConfig;
import com.zszl.zszlScriptMod.handlers.ChatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
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

    @Shadow private void logChatMessage(GuiMessage message) {
        throw new AssertionError();
    }

    @Shadow protected abstract boolean isChatFocused();

    @Shadow private int getWidth() {
        throw new AssertionError();
    }

    @Shadow public abstract int getLinesPerPage();
    @Shadow public abstract double getScale();
    @Shadow public abstract void resetChatScroll();
    @Shadow public abstract void scrollChat(int amount);

    @Shadow private void refreshTrimmedMessages() {
        throw new AssertionError();
    }

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
                this.refreshTrimmedMessages();
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

        int addedTime = this.minecraft.gui.getGuiTicks();
        GuiMessage processedMessage = new GuiMessage(addedTime, displayMessage, signature, tag);
        this.logChatMessage(processedMessage);
        zszl$addProcessedMessage(processedMessage, false);
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

    @Unique
    private void zszl$addProcessedMessage(GuiMessage message, boolean displayOnly) {
        int width = Mth.floor((double) this.getWidth() / this.getScale());
        List<FormattedCharSequence> wrapped = ComponentRenderUtils.wrapComponents(message.content(), width, this.minecraft.font);
        boolean focused = this.isChatFocused();
        zszl$newLines = Math.max(0, wrapped.size() - 1);

        for (int i = 0; i < wrapped.size(); i++) {
            FormattedCharSequence sequence = wrapped.get(i);
            if (focused && this.chatScrollbarPos > 0) {
                this.newMessageSinceScroll = true;
                this.scrollChat(1);
            }
            boolean endOfEntry = i == wrapped.size() - 1;
            this.trimmedMessages.add(0, new GuiMessage.Line(message.addedTime(), sequence, message.tag(), endOfEntry));
        }

        while (this.trimmedMessages.size() > 100) {
            this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
        }

        if (!displayOnly) {
            this.allMessages.add(0, message);
            while (this.allMessages.size() > 100) {
                this.allMessages.remove(this.allMessages.size() - 1);
            }
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

}
