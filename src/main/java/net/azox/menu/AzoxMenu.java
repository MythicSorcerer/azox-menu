package net.azox.menu;

import net.azox.menu.commands.MusicCommand;
import net.azox.menu.commands.NewsCommand;
import net.azox.menu.commands.ReloadCommand;
import net.azox.menu.listeners.MusicListener;
import net.azox.menu.listeners.PlayerJoinListener;
import net.azox.menu.listeners.PlayerMoveListener;
import net.azox.menu.listeners.PlayerQuitListener;
import net.azox.menu.listeners.ScoreboardListener;
import net.azox.menu.managers.MusicManager;
import net.azox.menu.managers.SidebarManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.logging.Level;

@Getter
public final class AzoxMenu extends JavaPlugin {

    @Getter
    private static AzoxMenu instance;

    private SidebarManager sidebarManager;
    private MusicManager musicManager;

    @Override
    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();

        this.disableDefaultMusic();

        this.sidebarManager = new SidebarManager(this);
        this.musicManager = new MusicManager(this);

        this.registerListeners();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            this.registerCommands();
            this.getLogger().log(Level.INFO, "AzoxMenu has been enabled successfully.");
        }, 1L);
    }

    @Override
    public void onDisable() {
        if (this.sidebarManager != null) {
            this.sidebarManager.cleanup();
        }

        if (this.musicManager != null) {
            this.musicManager.cleanup();
        }

        instance = null;
        this.getLogger().log(Level.INFO, "AzoxMenu has been disabled.");
    }

    public void reloadPluginConfig() {
        this.reloadConfig();
        if (this.sidebarManager != null) {
            this.sidebarManager.reloadConfig();
        }
        if (this.musicManager != null) {
            this.musicManager.reloadConfig();
        }
        this.getLogger().log(Level.INFO, "Configuration has been reloaded.");
    }

    private void disableDefaultMusic() {
        this.getServer().getScheduler().runTaskLater(this, () -> {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                player.stopSound(org.bukkit.Sound.MUSIC_CREATIVE, org.bukkit.SoundCategory.MUSIC);
                player.stopSound(org.bukkit.Sound.MUSIC_GAME, org.bukkit.SoundCategory.MUSIC);
                player.stopSound(org.bukkit.Sound.MUSIC_MENU, org.bukkit.SoundCategory.MUSIC);
            }
        }, 5L);
        
        this.getLogger().log(Level.INFO, "Default Minecraft music disabled.");
    }

    private void registerCommands() {
        this.registerCommand("azoxreload", new ReloadCommand(), "azox.admin");
        this.registerCommand("music", new MusicCommand(this), "azox.menu");
        this.registerCommand("news", new NewsCommand(), "azox.news");
    }

    private void registerCommand(final String name, final CommandExecutor executor, final String permission) {
        try {
            final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            final CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            final Command command = new Command(name) {
                @Override
                public boolean execute(final CommandSender sender, final String label, final String[] args) {
                    if (!sender.hasPermission(permission)) {
                        sender.sendMessage(Component.text("You don't have permission to use this command.").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                        return true;
                    }
                    return executor.onCommand(sender, this, label, args);
                }
            };

            commandMap.register(this.getName(), command);
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to register command " + name + ": " + exception.getMessage());
        }
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerMoveListener(), this);
        this.getServer().getPluginManager().registerEvents(new MusicListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ScoreboardListener(this), this);
    }
}
