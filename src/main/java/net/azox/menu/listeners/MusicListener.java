package net.azox.menu.listeners;

import net.azox.menu.AzoxMenu;
import net.azox.menu.managers.MusicManager;
import net.azox.menu.managers.SidebarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MusicListener implements Listener {

    private final AzoxMenu plugin;

    public MusicListener(final AzoxMenu plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final MusicManager musicManager = MusicManager.getInstance();

        if (musicManager != null) {
            musicManager.onPlayerJoin(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final MusicManager musicManager = MusicManager.getInstance();

        if (musicManager != null) {
            musicManager.onPlayerQuit(player);
        }
    }
}
