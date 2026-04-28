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

package com.zszl.zszlScriptMod.shadowbaritone.command.defaults;

import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.Command;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.ICommand;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.argument.IArgConsumer;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.exception.CommandException;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.exception.CommandNotFoundException;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.helpers.Paginator;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.helpers.TabCompleteHelper;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.ShadowBaritoneI18n;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import static com.zszl.zszlScriptMod.shadowbaritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class HelpCommand extends Command {

    public HelpCommand(IBaritone baritone) {
        super(baritone, "help", "?");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);
        if (!args.hasAny() || args.is(Integer.class)) {
            Paginator.paginate(
                    args, new Paginator<>(
                            this.baritone.getCommandManager().getRegistry().descendingStream()
                                    .filter(command -> !command.hiddenFromHelp())
                                    .collect(Collectors.toList())
                    ),
                    () -> logDirect(ShadowBaritoneI18n.trKey("shadowbaritone.command.help.header.all_commands")),
                    command -> {
                        String translatedShortDesc = ShadowBaritoneI18n.trCommandShortDesc(command);
                        String names = String.join("/", command.getNames());
                        String name = command.getNames().get(0);
                        MutableComponent shortDescComponent = Component.literal(" - " + translatedShortDesc);
                        shortDescComponent.setStyle(shortDescComponent.getStyle().withColor(ChatFormatting.DARK_GRAY));
                        MutableComponent namesComponent = Component.literal(names);
                        namesComponent.setStyle(namesComponent.getStyle().withColor(ChatFormatting.WHITE));
                        MutableComponent hoverComponent = Component.literal("");
                        hoverComponent.setStyle(hoverComponent.getStyle().withColor(ChatFormatting.GRAY));
                        hoverComponent.append(namesComponent);
                        hoverComponent.append("\n" + translatedShortDesc);
                        hoverComponent.append("\n\n");
                        hoverComponent.append(ShadowBaritoneI18n.trKey("shadowbaritone.command.help.hover.click_full_help"));
                        String clickCommand = FORCE_COMMAND_PREFIX + String.format("%s %s", label, command.getNames().get(0));
                        MutableComponent component = Component.literal(name);
                        component.setStyle(component.getStyle().withColor(ChatFormatting.GRAY));
                        component.append(shortDescComponent);
                        component.setStyle(component.getStyle()
                                .withHoverEvent(new HoverEvent.ShowText(hoverComponent))
                                .withClickEvent(new ClickEvent.RunCommand(clickCommand)));
                        return component;
                    },
                    FORCE_COMMAND_PREFIX + label
            );
        } else {
            String commandName = args.getString().toLowerCase();
            ICommand command = this.baritone.getCommandManager().getCommand(commandName);
            if (command == null) {
                throw new CommandNotFoundException(commandName);
            }
            logDirect(String.format("%s - %s", String.join(" / ", command.getNames()),
                    ShadowBaritoneI18n.trCommandShortDesc(command)));
            logDirect("");
            ShadowBaritoneI18n.trCommandLongDesc(command).forEach(this::logDirect);
            logDirect("");
            MutableComponent returnComponent = Component.literal(
                    ShadowBaritoneI18n.trKey("shadowbaritone.command.help.action.return_to_menu"));
            returnComponent.setStyle(returnComponent.getStyle().withClickEvent(new ClickEvent.RunCommand(
                    FORCE_COMMAND_PREFIX + label
            )));
            logDirect(returnComponent);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .addCommands(this.baritone.getCommandManager())
                    .filterPrefix(args.getString())
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return ShadowBaritoneI18n.trKey("shadowbaritone.command.help.short_desc");
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                ShadowBaritoneI18n.trKey("shadowbaritone.command.help.long_desc.1"),
                "",
                ShadowBaritoneI18n.trKey("shadowbaritone.command.help.long_desc.usage"),
                ShadowBaritoneI18n.trKey("shadowbaritone.command.help.long_desc.example.list"),
                ShadowBaritoneI18n.trKey("shadowbaritone.command.help.long_desc.example.command")
        );
    }
}

