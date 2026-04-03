package net.azox.menu.listeners;

import net.azox.menu.AzoxMenu;
import net.azox.menu.managers.SidebarManager;
import net.azox.menu.models.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerMoveListener implements Listener {

    @EventHandler
    public void onPlayerMove(@NotNull final PlayerMoveEvent event) {
        final Player player = event.getPlayer();

        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin == null) {
            return;
        }

        final SidebarManager sidebarManager = plugin.getSidebarManager();
        if (sidebarManager == null) {
            return;
        }

        final PlayerDataManager dataManager = PlayerDataManager.getInstance();
        if (dataManager == null) {
            return;
        }

        final PlayerDataManager.PlayerData data = dataManager.get(player.getUniqueId());
        if (data == null) {
            return;
        }

        final long afkTimeoutMillis = sidebarManager.getConfig().getAfkTimeoutMillis();

        if (data.isAfk(afkTimeoutMillis) && data.getStatus() == PlayerDataManager.PlayerStatus.ACTIVE) {
            data.setStatus(PlayerDataManager.PlayerStatus.AFK);
            sidebarManager.refreshPlayerSidebar(player);
        }

        data.updateActivity();
    }
}
