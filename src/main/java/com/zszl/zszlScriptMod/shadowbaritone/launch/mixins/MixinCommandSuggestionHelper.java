/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.shadowbaritone.launch.util.ReflectionHelper;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 10/9/2019
 */
@Mixin(CommandSuggestions.class)
public class MixinCommandSuggestionHelper {

    private static final String TAB_COMPLETE_EVENT_CLASS =
            "com.zszl.zszlScriptMod.shadowbaritone.api.event.events.TabCompleteEvent";
    private static final String BARITONE_API_CLASS =
            "com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI";

    @Inject(
            method = "updateCommandInfo",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preUpdateSuggestion(CallbackInfo ci) {
        EditBox input = ReflectionHelper.getField(this, "input", "f_93853_");
        // Anything that is present in the input text before the cursor position
        String prefix = input.getValue().substring(0, Math.min(input.getValue().length(), input.getCursorPosition()));

        Object event = createTabCompleteEvent(prefix);
        if (event == null || !dispatchTabCompleteEvent(event)) {
            return;
        }

        if (isTabCompleteCancelled(event)) {
            ci.cancel();
            return;
        }

        String[] completions = getTabCompletions(event);
        if (completions != null) {
            ci.cancel();

            ReflectionHelper.setField(this, null, "currentParse", "f_93864_"); // stop coloring

            boolean keepSuggestions = ReflectionHelper.getField(this, "keepSuggestions", "f_93868_");
            if (keepSuggestions) { // Supress suggestions update when cycling suggestions.
                return;
            }

            input.setSuggestion(null); // clear old suggestions
            ReflectionHelper.setField(this, null, "suggestions", "f_93866_");
            // TODO: Support populating the command usage
            List<String> commandUsage = ReflectionHelper.getField(this, "commandUsage", "f_93861_");
            commandUsage.clear();

            if (completions.length == 0) {
                ReflectionHelper.setField(this, Suggestions.empty(), "pendingSuggestions", "f_93865_");
            } else {
                StringRange range = StringRange.between(prefix.lastIndexOf(" ") + 1, prefix.length()); // if there is no space this starts at 0

                List<Suggestion> suggestionList = Stream.of(completions)
                        .map(s -> new Suggestion(range, s))
                        .collect(Collectors.toList());

                Suggestions suggestions = new Suggestions(range, suggestionList);

                CompletableFuture<Suggestions> pendingSuggestions = new CompletableFuture<>();
                pendingSuggestions.complete(suggestions);
                ReflectionHelper.setField(this, pendingSuggestions, "pendingSuggestions", "f_93865_");
            }
            ((CommandSuggestions) (Object) this).showSuggestions(true); // actually populate the suggestions list from the suggestions future
        }
    }

    private static Object createTabCompleteEvent(String prefix) {
        try {
            Class<?> eventClass = Class.forName(TAB_COMPLETE_EVENT_CLASS);
            return eventClass.getConstructor(String.class).newInstance(prefix);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean dispatchTabCompleteEvent(Object event) {
        try {
            Class<?> apiClass = Class.forName(BARITONE_API_CLASS);
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object handler = baritone.getClass().getMethod("getGameEventHandler").invoke(baritone);
            handler.getClass().getMethod("onPreTabComplete", event.getClass()).invoke(handler, event);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isTabCompleteCancelled(Object event) {
        try {
            Object result = event.getClass().getMethod("isCancelled").invoke(event);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String[] getTabCompletions(Object event) {
        try {
            Object value = event.getClass().getField("completions").get(event);
            return value instanceof String[] ? (String[]) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}

