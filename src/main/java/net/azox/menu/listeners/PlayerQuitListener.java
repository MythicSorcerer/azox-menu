package net.azox.menu.listeners;

import net.azox.menu.AzoxMenu;
import net.azox.menu.managers.SidebarManager;
import net.azox.menu.models.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin != null) {
            final SidebarManager sidebarManager = plugin.getSidebarManager();
            if (sidebarManager != null) {
                sidebarManager.hideSidebar(player);
            }
        }

        if (PlayerDataManager.getInstance() != null) {
            PlayerDataManager.getInstance().remove(player.getUniqueId());
        }
    }
}
