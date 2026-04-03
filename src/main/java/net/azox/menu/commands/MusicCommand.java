package net.azox.menu.commands;

import net.azox.menu.AzoxMenu;
import net.azox.menu.managers.MusicManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MusicCommand implements CommandExecutor, TabCompleter {

    private final AzoxMenu plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MusicCommand(final AzoxMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command,
                            @NotNull final String label, @NotNull final String[] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.miniMessage.deserialize("<red>Only players can use this command."));
            return true;
        }

        if (args.length == 0) {
            this.sendHelp(player);
            return true;
        }

        final String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "on" -> this.enableMusic(player);
            case "off" -> this.disableMusic(player);
            case "play" -> this.playMusic(player);
            case "status" -> this.sendStatus(player);
            case "1", "2", "3", "4", "5" -> this.handleRating(player, Integer.parseInt(subCommand));
            default -> this.sendHelp(player);
        }

        return true;
    }

    private void sendHelp(final Player player) {
        final Component help = this.miniMessage.deserialize(
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<newline>" +
                "<gold>Music Commands<newline>" +
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<newline>" +
                "<white>/music on <gray>- Enable custom music<newline>" +
                "<white>/music off <gray>- Disable custom music<newline>" +
                "<white>/music play <gray>- Play a random track manually<newline>" +
                "<white>/music status <gray>- Check current music status<newline>" +
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        player.sendMessage(help);
    }

    private void enableMusic(final Player player) {
        final MusicManager musicManager = MusicManager.getInstance();
        if (musicManager != null) {
            musicManager.enableMusic(player);
        }
    }

    private void disableMusic(final Player player) {
        final MusicManager musicManager = MusicManager.getInstance();
        if (musicManager != null) {
            musicManager.disableMusic(player);
        }
    }

    private void playMusic(final Player player) {
        final MusicManager musicManager = MusicManager.getInstance();
        if (musicManager != null) {
            musicManager.playMusicManually(player);
        }
    }

    private void sendStatus(final Player player) {
        final MusicManager musicManager = MusicManager.getInstance();
        if (musicManager == null) {
            final Component error = this.miniMessage.deserialize("<red>Music system is not available.");
            player.sendMessage(error);
            return;
        }

        final boolean enabled = musicManager.hasMusicEnabled(player);
        final String track = musicManager.getCurrentTrackDisplay();

        final Component status = this.miniMessage.deserialize(
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<newline>" +
                "<gold>Music Status<newline>" +
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<newline>" +
                "<gray>Your Music: <" + (enabled ? "<green>Enabled" : "<red>Disabled") + "<newline>" +
                "<gray>Now Playing: <white>" + track + "<newline>" +
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        player.sendMessage(status);
    }

    private void handleRating(final Player player, final int rating) {
        final MusicManager musicManager = MusicManager.getInstance();
        if (musicManager == null) {
            return;
        }

        if (musicManager.hasPendingRating(player)) {
            musicManager.submitRating(player, rating);
        } else {
            final Component message = this.miniMessage.deserialize(
                    "<yellow>No music currently playing to rate. Use <white>/music play <yellow>to start!"
            );
            player.sendMessage(message);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command,
                                                  @NotNull final String alias, @NotNull final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("on");
            completions.add("off");
            completions.add("play");
            completions.add("status");
            return completions;
        }

        return completions;
    }
}
