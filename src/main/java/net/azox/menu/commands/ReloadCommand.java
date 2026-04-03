package net.azox.menu.commands;

import net.azox.menu.AzoxMenu;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public final class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender,
                             @NotNull final Command command,
                             @NotNull final String label,
                             @NotNull final String[] args) {

        if (!commandSender.hasPermission("azox.admin")) {
            commandSender.sendMessage(Component.text("You do not have permission to execute this command.", NamedTextColor.RED));
            return true;
        }

        final AzoxMenu plugin = AzoxMenu.getInstance();

        if (plugin != null) {
            plugin.reloadPluginConfig();
            commandSender.sendMessage(Component.text("Configuration has been reloaded successfully!", NamedTextColor.GREEN));
        } else {
            commandSender.sendMessage(Component.text("Plugin instance is null! Please check logs.", NamedTextColor.RED));
        }

        return true;
    }
}
