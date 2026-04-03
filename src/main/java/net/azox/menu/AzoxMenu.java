package net.azox.menu;

import net.azox.menu.commands.MusicCommand;
import net.azox.menu.commands.NewsCommand;
import net.azox.menu.commands.ReloadCommand;
import net.azox.menu.listeners.MusicListener;
import net.azox.menu.listeners.PlayerJoinListener;
import net.azox.menu.listeners.PlayerMoveListener;
import net.azox.menu.listeners.PlayerQuitListener;
import net.azox.menu.managers.MusicManager;
import net.azox.menu.managers.SidebarManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

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

        this.registerCommands();
        this.registerListeners();

        this.getLogger().log(Level.INFO, "AzoxMenu has been enabled successfully.");
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
        Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(org.bukkit.GameRule.DO_INSOMNIA, true);
        });
        this.getLogger().log(Level.INFO, "Default Minecraft music disabled.");
    }

    private void registerCommands() {
        final PluginCommand reloadCommand = this.getCommand("azoxreload");
        if (reloadCommand != null) {
            reloadCommand.setExecutor(new ReloadCommand());
        }

        final PluginCommand newsCommand = this.getCommand("news");
        if (newsCommand != null) {
            newsCommand.setExecutor(new NewsCommand());
        }
        
        final PluginCommand musicCommand = this.getCommand("music");
        if (musicCommand != null) {
            musicCommand.setExecutor(new MusicCommand(this));
        }
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerMoveListener(), this);
        this.getServer().getPluginManager().registerEvents(new MusicListener(this), this);
    }
}
