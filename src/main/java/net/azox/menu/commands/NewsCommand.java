package net.azox.menu.commands;

import net.azox.menu.AzoxMenu;
import net.azox.menu.config.SidebarConfig;
import net.azox.menu.managers.SidebarManager;
import net.azox.menu.models.PlayerDataManager;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
public final class NewsCommand implements CommandExecutor, TabExecutor {

    @Override
    public boolean onCommand(@NotNull final CommandSender sender,
                             @NotNull final Command command,
                             @NotNull final String label,
                             @NotNull final String[] args) {

        if (!sender.hasPermission("azox.news")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            this.sendUsage(sender);
            return true;
        }

        final String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "set" -> this.handleSet(sender, args);
            case "clear" -> this.handleClear(sender);
            case "temp" -> this.handleTemp(sender, args);
            case "queue" -> this.handleQueue(sender, args);
            case "list" -> this.handleList(sender);
            case "announce" -> this.handleAnnounce(sender);
            default -> {
                this.sendUsage(sender);
                yield true;
            }
        };
    }

    private void sendUsage(final CommandSender sender) {
        sender.sendMessage(Component.text("=== News Command Usage ===").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.text("/news set <message>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set the main news message").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news clear").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Clear the main news message").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news temp [online|all] [duration] <message>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Create temporary flashing news").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news queue add <message>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Add message to queue").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news queue list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - List queued messages").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news queue remove <index>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Remove from queue").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news queue reorder <from> <to>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Reorder queue").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news queue clear").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Clear queue").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news queue flash <seconds>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set flash duration for queued").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Show current broadcasts status").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/news announce").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Toggle auto-announce queued news").color(NamedTextColor.GRAY)));
    }

    private boolean handleSet(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /news set <message>").color(NamedTextColor.RED));
            return true;
        }

        final String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin == null) {
            sender.sendMessage(Component.text("Plugin error.").color(NamedTextColor.RED));
            return true;
        }

        final SidebarConfig config = plugin.getSidebarManager().getConfig();
        config.setMainNews(message);

        this.broadcastToOnlinePlayers(message, "main");

        sender.sendMessage(Component.text("Main news has been set to: ").color(NamedTextColor.GREEN)
                .append(Component.text(message).color(NamedTextColor.WHITE)));
        return true;
    }

    private boolean handleClear(final CommandSender sender) {
        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin == null) {
            sender.sendMessage(Component.text("Plugin error.").color(NamedTextColor.RED));
            return true;
        }

        final SidebarConfig config = plugin.getSidebarManager().getConfig();
        config.setMainNews("");

        sender.sendMessage(Component.text("Main news has been cleared.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleTemp(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /news temp [online|all] [duration] <message>").color(NamedTextColor.RED));
            return true;
        }

        String targetMode = "online";
        int duration = 10;
        String message;
        int messageStartIndex = 1;

        final String firstArg = args[1].toLowerCase();
        if (firstArg.equals("online") || firstArg.equals("all")) {
            targetMode = firstArg;
            messageStartIndex = 2;
        }

        if (args.length > messageStartIndex) {
            try {
                duration = Integer.parseInt(args[messageStartIndex]);
                messageStartIndex++;
            } catch (final NumberFormatException ignored) {
            }
        }

        if (args.length <= messageStartIndex) {
            sender.sendMessage(Component.text("Usage: /news temp [online|all] [duration] <message>").color(NamedTextColor.RED));
            return true;
        }

        message = String.join(" ", Arrays.copyOfRange(args, messageStartIndex, args.length));

        if (targetMode.equals("all")) {
            this.broadcastToAllPlayers(message, duration);
        } else {
            this.broadcastToOnlinePlayers(message, "temp");
            this.scheduleTempNewsRemoval(message, duration);
        }

        sender.sendMessage(Component.text("Temporary news has been broadcast: ").color(NamedTextColor.GREEN)
                .append(Component.text(message).color(NamedTextColor.WHITE))
                .append(Component.text(" (" + duration + "s)").color(NamedTextColor.GRAY)));
        return true;
    }

    private boolean handleQueue(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /news queue <add|list|remove|reorder|clear|flash>").color(NamedTextColor.RED));
            return true;
        }

        final String queueAction = args[1].toLowerCase();
        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin == null) {
            sender.sendMessage(Component.text("Plugin error.").color(NamedTextColor.RED));
            return true;
        }

        final SidebarConfig config = plugin.getSidebarManager().getConfig();

        return switch (queueAction) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /news queue add <message>").color(NamedTextColor.RED));
                    yield true;
                }
                final String queueMessage = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                config.addToNewsQueue(queueMessage);
                sender.sendMessage(Component.text("Added to queue: ").color(NamedTextColor.GREEN)
                        .append(Component.text(queueMessage).color(NamedTextColor.WHITE)));
                yield true;
            }
            case "list" -> {
                final List<String> queue = config.getTempNewsQueue();
                if (queue.isEmpty()) {
                    sender.sendMessage(Component.text("Queue is empty.").color(NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("=== News Queue ===").color(NamedTextColor.GOLD));
                    for (int i = 0; i < queue.size(); i++) {
                        sender.sendMessage(Component.text((i + 1) + ". " + queue.get(i)).color(NamedTextColor.WHITE));
                    }
                }
                yield true;
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /news queue remove <index>").color(NamedTextColor.RED));
                    yield true;
                }
                try {
                    final int index = Integer.parseInt(args[2]) - 1;
                    config.removeFromNewsQueue(index);
                    sender.sendMessage(Component.text("Removed from queue.").color(NamedTextColor.GREEN));
                } catch (final NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid index.").color(NamedTextColor.RED));
                }
                yield true;
            }
            case "reorder" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /news queue reorder <from> <to>").color(NamedTextColor.RED));
                    yield true;
                }
                try {
                    final int from = Integer.parseInt(args[2]) - 1;
                    final int to = Integer.parseInt(args[3]) - 1;
                    config.reorderNewsQueue(from, to);
                    sender.sendMessage(Component.text("Queue reordered.").color(NamedTextColor.GREEN));
                } catch (final NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid indices.").color(NamedTextColor.RED));
                }
                yield true;
            }
            case "clear" -> {
                config.clearNewsQueue();
                sender.sendMessage(Component.text("Queue cleared.").color(NamedTextColor.GREEN));
                yield true;
            }
            case "flash" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /news queue flash <seconds>").color(NamedTextColor.RED));
                    yield true;
                }
                try {
                    final int seconds = Integer.parseInt(args[2]);
                    sender.sendMessage(Component.text("Queued news flash duration set to " + seconds + " seconds.").color(NamedTextColor.GREEN));
                } catch (final NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number.").color(NamedTextColor.RED));
                }
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown queue action: " + queueAction).color(NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleList(final CommandSender sender) {
        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin == null) {
            sender.sendMessage(Component.text("Plugin error.").color(NamedTextColor.RED));
            return true;
        }

        final SidebarConfig config = plugin.getSidebarManager().getConfig();

        sender.sendMessage(Component.text("=== Current Broadcasts ===").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        sender.sendMessage(Component.text("Main News: ").color(NamedTextColor.YELLOW)
                .append(Component.text(config.getMainNews().isEmpty() ? "(none)" : config.getMainNews()).color(NamedTextColor.WHITE)));

        final List<String> queue = config.getTempNewsQueue();
        sender.sendMessage(Component.text("Queued News (" + queue.size() + "): ").color(NamedTextColor.YELLOW)
                .append(Component.text(queue.isEmpty() ? "(none)" : queue.size() + " messages").color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("Temp Flash Duration: ").color(NamedTextColor.YELLOW)
                .append(Component.text(config.getTempNewsFlashDuration() + "s").color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("Queued Flash Duration: ").color(NamedTextColor.YELLOW)
                .append(Component.text(config.getQueuedNewsFlashDuration() + "s").color(NamedTextColor.WHITE)));

        return true;
    }

    private boolean handleAnnounce(final CommandSender sender) {
        sender.sendMessage(Component.text("Announce toggle is not yet implemented.").color(NamedTextColor.YELLOW));
        return true;
    }

    private void broadcastToOnlinePlayers(final String message, final String type) {
        final Component broadcastComponent = Component.text()
                .color(NamedTextColor.YELLOW)
                .append(Component.text("[NEWS] ").decoration(TextDecoration.BOLD, true))
                .append(Component.text(message).color(NamedTextColor.WHITE))
                .build();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(broadcastComponent);

            final PlayerDataManager dataManager = PlayerDataManager.getInstance();
            if (dataManager != null) {
                final PlayerDataManager.PlayerData data = dataManager.get(player.getUniqueId());
                if (data != null) {
                    data.setHasUnreadNews(true);
                }
            }
        }

        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin != null && type.equals("main")) {
            plugin.getSidebarManager().refreshAllPlayers();
        }
    }

    private void broadcastToAllPlayers(final String message, final int durationSeconds) {
        final Component broadcastComponent = Component.text()
                .color(NamedTextColor.YELLOW)
                .append(Component.text("[NEWS] ").decoration(TextDecoration.BOLD, true))
                .append(Component.text(message).color(NamedTextColor.WHITE))
                .build();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(broadcastComponent);

            final PlayerDataManager dataManager = PlayerDataManager.getInstance();
            if (dataManager != null) {
                final PlayerDataManager.PlayerData data = dataManager.get(player.getUniqueId());
                if (data != null) {
                    data.setHasUnreadNews(true);
                    data.setTempNewsFlashing(true);
                }
            }
        }

        this.scheduleTempNewsRemoval(message, durationSeconds);
    }

    private void scheduleTempNewsRemoval(final String message, final int durationSeconds) {
        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin == null) {
            return;
        }

        final long ticks = durationSeconds * 20L;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                final PlayerDataManager dataManager = PlayerDataManager.getInstance();
                if (dataManager != null) {
                    final PlayerDataManager.PlayerData data = dataManager.get(player.getUniqueId());
                    if (data != null && data.isTempNewsFlashing()) {
                        data.setTempNewsFlashing(false);
                    }
                }
            }
            plugin.getLogger().info("Temporary news expired: " + message);
        }, ticks);
    }

    @Override
    public List<String> onTabComplete(@NotNull final CommandSender sender,
                                      @NotNull final Command command,
                                      @NotNull final String alias,
                                      @NotNull final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("set", "clear", "temp", "queue", "list", "announce"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("temp")) {
                completions.addAll(Arrays.asList("online", "all"));
            } else if (args[0].equalsIgnoreCase("queue")) {
                completions.addAll(Arrays.asList("add", "list", "remove", "reorder", "clear", "flash"));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }
}
