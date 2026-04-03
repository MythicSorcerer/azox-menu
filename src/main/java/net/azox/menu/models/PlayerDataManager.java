package net.azox.menu.models;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class PlayerDataManager {

    private static PlayerDataManager instance;
    private final ConcurrentMap<UUID, PlayerData> playerDataMap;

    public PlayerDataManager() {
        instance = this;
        this.playerDataMap = new ConcurrentHashMap<>();
    }

    public static PlayerDataManager getInstance() {
        return instance;
    }

    public PlayerData getOrCreate(final Player player) {
        return this.playerDataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(player));
    }

    public PlayerData get(final UUID uuid) {
        return this.playerDataMap.get(uuid);
    }

    public void remove(final UUID uuid) {
        this.playerDataMap.remove(uuid);
    }

    public void clear() {
        this.playerDataMap.clear();
    }

    @Getter
    public static class PlayerData {
        private final UUID uuid;
        private final String playerName;

        private int kills;
        private int deaths;
        private long playtimeSeconds;
        private long lastActivityTime;
        @Getter @Setter
        private PlayerStatus status;
        @Getter @Setter
        private boolean hasUnreadNews;
        @Getter @Setter
        private boolean tempNewsFlashing;

        public PlayerData(final Player player) {
            this.uuid = player.getUniqueId();
            this.playerName = player.getName();
            this.kills = 0;
            this.deaths = 0;
            this.playtimeSeconds = 0;
            this.lastActivityTime = System.currentTimeMillis();
            this.status = PlayerStatus.ACTIVE;
            this.hasUnreadNews = false;
            this.tempNewsFlashing = false;
        }

        public void updateActivity() {
            this.lastActivityTime = System.currentTimeMillis();
            if (this.status == PlayerStatus.AFK) {
                this.status = PlayerStatus.ACTIVE;
            }
        }

        public boolean isAfk(final long afkTimeoutMillis) {
            return System.currentTimeMillis() - this.lastActivityTime > afkTimeoutMillis;
        }

        public void incrementKills() {
            this.kills++;
        }

        public void incrementDeaths() {
            this.deaths++;
        }

        public void addPlaytime(final long seconds) {
            this.playtimeSeconds += seconds;
        }

        public String getFormattedPlaytime() {
            final long hours = this.playtimeSeconds / 3600;
            final long minutes = (this.playtimeSeconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    public enum PlayerStatus {
        ACTIVE,
        JAIL,
        AFK,
        NEWS
    }
}
