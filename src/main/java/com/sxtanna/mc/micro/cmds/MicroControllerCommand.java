package com.sxtanna.mc.micro.cmds;

import org.jetbrains.annotations.NotNull;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sxtanna.mc.micro.MicroControllerPlugin;
import com.sxtanna.mc.micro.data.ControllerFlag;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

@CommandAlias("microcontroller|microc|uc")
@CommandPermission("micro.controller.command")
public final class MicroControllerCommand extends BaseCommand {

    @NotNull
    private final MicroControllerPlugin plugin;


    public MicroControllerCommand(@NotNull final MicroControllerPlugin plugin) {
        this.plugin = plugin;
    }


    @HelpCommand
    public void help(@NotNull final CommandHelp help) {
        help.showHelp();
    }


    @Subcommand("clear|reset block")
    @CommandPermission("micro.controller.command.clear.block")
    public void clear(@NotNull final Player sender) {
        final var target = sender.getTargetBlockExact(6);
        if (target == null) {
            reply(sender, Component.text("You must be looking at a block").color(NamedTextColor.RED));
            return;
        }


        final var data = target.getBlockData();
        final var prev = this.plugin.setControllerFlagMask(data, Long.MIN_VALUE);

        if (prev.isEmpty()) {
            reply(sender, Component.text("This block is not controlled!").color(NamedTextColor.GRAY));
        } else {
            reply(sender, Component.text()
                                   .append(Component.text("Successfully removed controlled flags").color(NamedTextColor.GREEN))
                                   .append(Component.text(":").color(NamedTextColor.GRAY))
                                   .append(Component.text(" "))
                                   .append(format(ControllerFlag.read(prev.getAsLong()))));
        }

        this.plugin.saveControlled();
    }

    @Subcommand("toggle block")
    @CommandPermission("micro.controller.command.toggle.block")
    @CommandCompletion("click|break|place|pick|drop true|false")
    public void toggle(@NotNull final Player sender, @NotNull final ControllerFlag flag, final boolean specific) {
        final var target = sender.getTargetBlockExact(6);
        if (target == null) {
            reply(sender, Component.text("You must be looking at a block").color(NamedTextColor.RED));
            return;
        }

        final var data = !specific ? target.getType().key().asString() : target.getBlockData().getAsString(true);
        final var prev = this.plugin.getControllerFlagMask(data);

        final var mask = new HashSet<>(prev.isEmpty() ? Collections.emptySet() : ControllerFlag.read(prev.getAsLong()));

        if (mask.remove(flag)) {
            reply(sender, Component.text()
                                   .append(Component.text("Successfully toggled").color(NamedTextColor.GRAY))
                                   .append(Component.text(" "))
                                   .append(Component.text(flag.disp).color(NamedTextColor.YELLOW))
                                   .append(Component.text(" "))
                                   .append(Component.text("on").color(NamedTextColor.GREEN)));
        } else {
            mask.add(flag);

            reply(sender, Component.text()
                                   .append(Component.text("Successfully toggled").color(NamedTextColor.GRAY))
                                   .append(Component.text(" "))
                                   .append(Component.text(flag.disp).color(NamedTextColor.YELLOW))
                                   .append(Component.text(" "))
                                   .append(Component.text("off").color(NamedTextColor.RED)));
        }

        if (mask.isEmpty()) {
            this.plugin.setControllerFlagMask(data, Long.MIN_VALUE);
        } else {
            this.plugin.setControllerFlagMask(data, ControllerFlag.pack(mask));
        }

        this.plugin.saveControlled();
    }


    private void reply(@NotNull final CommandSender sender, @NotNull final ComponentLike component) {
        sender.sendMessage(Component.text()
                                    .append(Component.text("[").color(NamedTextColor.DARK_GRAY))
                                    .append(Component.text("μ").color(NamedTextColor.WHITE))
                                    .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                                    .append(Component.text(" "))
                                    .append(component));
    }

    private @NotNull ComponentLike format(@NotNull final Collection<ControllerFlag> flags) {
        if (flags.isEmpty()) {
            return Component.text("[]").color(NamedTextColor.DARK_GRAY);
        }

        final var component = Component.text();

        component.append(Component.text("[").color(NamedTextColor.DARK_GRAY));

        var size = flags.size();

        for (final var flag : flags) {
            component.append(Component.text(flag.disp).color(NamedTextColor.YELLOW));

            if (--size <= 0) {
                continue;
            }

            component.append(Component.text(", ").color(NamedTextColor.GRAY));
        }

        component.append(Component.text("]").color(NamedTextColor.DARK_GRAY));

        return component;
    }


}
