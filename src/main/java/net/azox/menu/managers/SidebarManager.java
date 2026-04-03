package net.azox.menu.managers;

import net.azox.menu.AzoxMenu;
import net.azox.menu.config.SidebarConfig;
import net.azox.menu.models.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SidebarManager {

    private final AzoxMenu plugin;
    private SidebarConfig config;
    private BukkitTask updateTask;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final Map<UUID, Scoreboard> playerScoreboards;
    private final Set<UUID> flashingPlayers;

    public SidebarManager(final AzoxMenu plugin) {
        this.plugin = plugin;
        this.config = new SidebarConfig(plugin);
        this.playerScoreboards = new ConcurrentHashMap<>();
        this.flashingPlayers = ConcurrentHashMap.newKeySet();
        this.startUpdateTask();
    }

    public void reloadConfig() {
        this.config.load();
    }

    public SidebarConfig getConfig() {
        return this.config;
    }

    private String miniToLegacy(final String mini) {
        try {
            final Component component = this.miniMessage.deserialize(mini);
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
        } catch (final Exception e) {
            return mini;
        }
    }

    public void showSidebar(final Player player) {
        if (!this.config.isEnabled()) {
            return;
        }

        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        final String objectiveName = "azoxmenu";

        final String title = this.miniToLegacy(this.config.getServerName());

        final Objective objective = scoreboard.registerNewObjective(
                objectiveName,
                Criteria.DUMMY,
                title
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.updateSidebarContent(scoreboard);

        player.setScoreboard(scoreboard);
        this.playerScoreboards.put(player.getUniqueId(), scoreboard);
    }

    public void hideSidebar(final Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        this.playerScoreboards.remove(player.getUniqueId());
        this.flashingPlayers.remove(player.getUniqueId());
    }

    private void updateSidebarContent(final Scoreboard scoreboard) {
        final List<String> order = this.config.getEntryOrder();
        int score = order.size();

        for (final String entryKey : order) {
            final String entry = this.buildEntry(entryKey);
            if (entry != null) {
                final String teamName = "entry_" + score;
                final Team team = scoreboard.registerNewTeam(teamName);

                team.addEntry(entry);
                team.setPrefix(this.getPrefixForEntry(entryKey));
                team.setSuffix(this.getSuffixForEntry(entryKey));

                final Objective objective = scoreboard.getObjective("azoxmenu");
                if (objective != null) {
                    objective.getScore(entry).setScore(score);
                }
            }
            score--;
        }
    }

    private String buildEntry(final String key) {
        return switch (key.toLowerCase()) {
            case "status" -> this.config.isShowStatus() ? " " : null;
            case "playtime" -> this.config.isShowPlaytime() ? " " : null;
            case "kills" -> this.config.isShowKills() ? " " : null;
            case "deaths" -> this.config.isShowDeaths() ? " " : null;
            case "money" -> this.config.isShowMoney() ? " " : null;
            case "rank" -> this.config.isShowRank() ? " " : null;
            case "store" -> this.config.isShowStore() ? " " : null;
            case "networth" -> this.config.isShowNetworth() ? " " : null;
            case "music" -> this.config.isShowMusic() ? "  " : null;
            default -> null;
        };
    }

    private String getPrefixForEntry(final String key) {
        final PlayerDataManager.PlayerStatus status = PlayerDataManager.getInstance() != null ?
                PlayerDataManager.PlayerStatus.ACTIVE : null;

        return switch (key.toLowerCase()) {
            case "status" -> this.config.isShowStatus() ? "§7Status: §f" : "";
            case "playtime" -> this.config.isShowPlaytime() ? "§7Playtime: §f" : "";
            case "kills" -> this.config.isShowKills() ? "§7Kills: §f" : "";
            case "deaths" -> this.config.isShowDeaths() ? "§7Deaths: §f" : "";
            case "money" -> this.config.isShowMoney() ? "§7Money: §a$" : "";
            case "rank" -> this.config.isShowRank() ? "§7Rank: §6" : "";
            case "store" -> this.config.isShowStore() ? "§7Store: §n§f" : "";
            case "networth" -> this.config.isShowNetworth() ? "§7Net Worth: §a$" : "";
            case "music" -> this.config.isShowMusic() ? "§7Music: §e" : "";
            default -> "";
        };
    }

    private String getSuffixForEntry(final String key) {
        return switch (key.toLowerCase()) {
            case "status" -> this.config.isShowStatus() ? this.getStatusSuffix() : "";
            case "playtime" -> this.config.isShowPlaytime() ? this.getPlaytimeSuffix() : "";
            case "kills" -> this.config.isShowKills() ? this.getKillsSuffix() : "";
            case "deaths" -> this.config.isShowDeaths() ? this.getDeathsSuffix() : "";
            case "money" -> this.config.isShowMoney() ? "0" : "";
            case "rank" -> this.config.isShowRank() ? "Default" : "";
            case "store" -> this.config.isShowStore() ? "§r" : "";
            case "networth" -> this.config.isShowNetworth() ? "0" : "";
            case "music" -> this.config.isShowMusic() ? this.getMusicSuffix() : "";
            default -> "";
        };
    }

    private String getStatusSuffix() {
        final UUID currentPlayerUuid = this.getCurrentPlayerUuid();
        if (currentPlayerUuid != null) {
            final PlayerDataManager.PlayerData data = PlayerDataManager.getInstance().get(currentPlayerUuid);
            if (data != null) {
                return switch (data.getStatus()) {
                    case ACTIVE -> "§aActive";
                    case JAIL -> "§cJail";
                    case AFK -> "§eAFK";
                    case NEWS -> "§6News!";
                };
            }
        }
        return "§aActive";
    }

    private UUID getCurrentPlayerUuid() {
        for (final UUID uuid : this.playerScoreboards.keySet()) {
            return uuid;
        }
        return null;
    }

    private String getPlaytimeSuffix() {
        final UUID uuid = this.getCurrentPlayerUuid();
        if (uuid != null) {
            final PlayerDataManager.PlayerData data = PlayerDataManager.getInstance().get(uuid);
            if (data != null) {
                return data.getFormattedPlaytime();
            }
        }
        return "0h 0m";
    }

    private String getKillsSuffix() {
        final UUID uuid = this.getCurrentPlayerUuid();
        if (uuid != null) {
            final PlayerDataManager.PlayerData data = PlayerDataManager.getInstance().get(uuid);
            if (data != null) {
                return String.valueOf(data.getKills());
            }
        }
        return "0";
    }

    private String getDeathsSuffix() {
        final UUID uuid = this.getCurrentPlayerUuid();
        if (uuid != null) {
            final PlayerDataManager.PlayerData data = PlayerDataManager.getInstance().get(uuid);
            if (data != null) {
                return String.valueOf(data.getDeaths());
            }
        }
        return "0";
    }

    private String getMusicSuffix() {
        final MusicManager musicManager = MusicManager.getInstance();
        if (musicManager != null) {
            return musicManager.getCurrentTrackDisplay();
        }
        return "None";
    }

    private String getMusicSuffixForPlayer(final Player player) {
        final MusicManager musicManager = MusicManager.getInstance();
        if (musicManager != null) {
            return musicManager.getCurrentTrackDisplay();
        }
        return "None";
    }

    private void startUpdateTask() {
        this.updateTask = Bukkit.getScheduler().runTaskTimer(
                this.plugin,
                () -> this.updateAllSidebars(),
                20L,
                this.config.getUpdateInterval()
        );
    }

    private void updateAllSidebars() {
        for (final Map.Entry<UUID, Scoreboard> entry : this.playerScoreboards.entrySet()) {
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                this.refreshPlayerSidebar(player);
            }
        }
    }

    public void refreshPlayerSidebar(final Player player) {
        final Scoreboard scoreboard = this.playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            this.showSidebar(player);
            return;
        }

        final Objective objective = scoreboard.getObjective("azoxmenu");
        if (objective != null) {
            final String title = this.miniToLegacy(this.config.getServerName());
            objective.setDisplayName(title);
        }

        final List<String> order = this.config.getEntryOrder();
        int score = order.size();

        for (final String entryKey : order) {
            final String entry = this.buildEntry(entryKey);
            if (entry != null) {
                final String teamName = "entry_" + score;
                final Team team = scoreboard.getTeam(teamName);
                if (team != null) {
                    team.setPrefix(this.getPrefixForEntryWithPlayer(entryKey, player));
                    team.setSuffix(this.getSuffixForEntryWithPlayer(entryKey, player));
                }
            }
            score--;
        }
    }

    private String getPrefixForEntryWithPlayer(final String key, final Player player) {
        return switch (key.toLowerCase()) {
            case "status" -> this.config.isShowStatus() ? "§7Status: §f" : "";
            case "playtime" -> this.config.isShowPlaytime() ? "§7Playtime: §f" : "";
            case "kills" -> this.config.isShowKills() ? "§7Kills: §f" : "";
            case "deaths" -> this.config.isShowDeaths() ? "§7Deaths: §f" : "";
            case "money" -> this.config.isShowMoney() ? "§7Money: §a$" : "";
            case "rank" -> this.config.isShowRank() ? "§7Rank: §6" : "";
            case "store" -> this.config.isShowStore() ? "§7Store: §n§f" : "";
            case "networth" -> this.config.isShowNetworth() ? "§7Net Worth: §a$" : "";
            case "music" -> this.config.isShowMusic() ? "§7Music: §e" : "";
            default -> "";
        };
    }

    private String getSuffixForEntryWithPlayer(final String key, final Player player) {
        final PlayerDataManager.PlayerData data = PlayerDataManager.getInstance().get(player.getUniqueId());

        return switch (key.toLowerCase()) {
            case "status" -> this.config.isShowStatus() ? this.getStatusSuffixForPlayer(data) : "";
            case "playtime" -> this.config.isShowPlaytime() ? (data != null ? data.getFormattedPlaytime() : "0h 0m") : "";
            case "kills" -> this.config.isShowKills() ? (data != null ? String.valueOf(data.getKills()) : "0") : "";
            case "deaths" -> this.config.isShowDeaths() ? (data != null ? String.valueOf(data.getDeaths()) : "0") : "";
            case "money" -> this.config.isShowMoney() ? "0" : "";
            case "rank" -> this.config.isShowRank() ? "Default" : "";
            case "store" -> this.config.isShowStore() ? "§r" : "";
            case "networth" -> this.config.isShowNetworth() ? "0" : "";
            case "music" -> this.config.isShowMusic() ? this.getMusicSuffixForPlayer(player) : "";
            default -> "";
        };
    }

    private String getStatusSuffixForPlayer(final PlayerDataManager.PlayerData data) {
        if (data != null) {
            return switch (data.getStatus()) {
                case ACTIVE -> "§aActive";
                case JAIL -> "§cJail";
                case AFK -> "§eAFK";
                case NEWS -> "§6News!";
            };
        }
        return "§aActive";
    }

    public void setTempNewsFlashing(final UUID uuid, final boolean flashing) {
        if (flashing) {
            this.flashingPlayers.add(uuid);
        } else {
            this.flashingPlayers.remove(uuid);
        }
    }

    public boolean isTempNewsFlashing(final UUID uuid) {
        return this.flashingPlayers.contains(uuid);
    }

    public void refreshAllPlayers() {
        for (final UUID uuid : this.playerScoreboards.keySet()) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                this.refreshPlayerSidebar(player);
            }
        }
    }

    public void cleanup() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
        }
        this.playerScoreboards.clear();
        this.flashingPlayers.clear();
    }
}
