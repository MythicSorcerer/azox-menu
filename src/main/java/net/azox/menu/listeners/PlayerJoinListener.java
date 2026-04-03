package net.azox.menu.listeners;

import net.azox.menu.AzoxMenu;
import net.azox.menu.managers.SidebarManager;
import net.azox.menu.models.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerJoinListener implements Listener {

    public PlayerJoinListener() {
        if (PlayerDataManager.getInstance() == null) {
            new PlayerDataManager();
        }
    }

    @EventHandler
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        final PlayerDataManager.PlayerData data = PlayerDataManager.getInstance().getOrCreate(player);

        final AzoxMenu plugin = AzoxMenu.getInstance();
        if (plugin == null) {
            return;
        }

        final SidebarManager sidebarManager = plugin.getSidebarManager();
        if (sidebarManager != null && plugin.getConfig().getBoolean("settings.enabled", true)) {
            sidebarManager.showSidebar(player);

            if (data.isHasUnreadNews()) {
                player.sendMessage(Component.text("You have unread news! Use /news to read it.").color(NamedTextColor.YELLOW));
            }
        }

        player.sendMessage(Component.text("Welcome back, ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(player.getName()).color(NamedTextColor.YELLOW))
                .append(Component.text("!").color(NamedTextColor.GRAY)));
    }
}
