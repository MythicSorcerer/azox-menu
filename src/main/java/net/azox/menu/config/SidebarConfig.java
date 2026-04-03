package net.azox.menu.config;

import net.azox.menu.AzoxMenu;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SidebarConfig {

    private final AzoxMenu plugin;
    private FileConfiguration config;

    private boolean enabled;
    private int updateInterval;
    private int afkTimeoutSeconds;

    private String serverName;
    private List<String> entryOrder;

    private boolean showMoney;
    private boolean showPlaytime;
    private boolean showKills;
    private boolean showDeaths;
    private boolean showRank;
    private boolean showStatus;
    private boolean showStore;
    private boolean showNetworth;
    private boolean showMusic;

    private String storeUrl;

    private String mainNews;
    private int tempNewsFlashDuration;
    private int queuedNewsFlashDuration;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public SidebarConfig(final AzoxMenu plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();

        this.enabled = this.config.getBoolean("settings.enabled", true);
        this.updateInterval = this.config.getInt("settings.update-interval", 20);
        this.afkTimeoutSeconds = this.config.getInt("settings.afk-timeout", 300);

        this.serverName = this.config.getString("sidebar.title.text", "<#FF6B6B>Server</#6BFF6B>Name");

        this.entryOrder = this.config.getStringList("sidebar.entries.order");
        if (this.entryOrder.isEmpty()) {
            this.entryOrder = List.of("status", "playtime", "kills", "deaths", "store", "networth");
        }

        this.showMoney = this.config.getBoolean("sidebar.entries.show-money", false);
        this.showPlaytime = this.config.getBoolean("sidebar.entries.show-playtime", true);
        this.showKills = this.config.getBoolean("sidebar.entries.show-kills", false);
        this.showDeaths = this.config.getBoolean("sidebar.entries.show-deaths", false);
        this.showRank = this.config.getBoolean("sidebar.entries.show-rank", false);
        this.showStatus = this.config.getBoolean("sidebar.entries.show-status", true);
        this.showStore = this.config.getBoolean("sidebar.entries.show-store", true);
        this.showNetworth = this.config.getBoolean("sidebar.entries.show-networth", false);
        this.showMusic = this.config.getBoolean("sidebar.entries.show-music", true);

        this.storeUrl = this.config.getString("sidebar.store-url", "store.example.com");

        this.mainNews = this.config.getString("news.main", "");
        this.tempNewsFlashDuration = this.config.getInt("news.temp-flash-duration", 10);
        this.queuedNewsFlashDuration = this.config.getInt("news.queued-flash-duration", 5);
    }

    public Component parseComponent(final String text) {
        try {
            return this.miniMessage.deserialize(text);
        } catch (final Exception exception) {
            return Component.text(text);
        }
    }

    public long getAfkTimeoutMillis() {
        return this.afkTimeoutSeconds * 1000L;
    }

    public void setMainNews(final String news) {
        this.mainNews = news;
        this.config.set("news.main", news);
        this.plugin.saveConfig();
    }

    public List<String> getTempNewsQueue() {
        return this.config.getStringList("news.queued");
    }

    public void addToNewsQueue(final String news) {
        final List<String> queue = new ArrayList<>(this.getTempNewsQueue());
        queue.add(news);
        this.config.set("news.queued", queue);
        this.plugin.saveConfig();
    }

    public void removeFromNewsQueue(final int index) {
        final List<String> queue = new ArrayList<>(this.getTempNewsQueue());
        if (index >= 0 && index < queue.size()) {
            queue.remove(index);
            this.config.set("news.queued", queue);
            this.plugin.saveConfig();
        }
    }

    public void reorderNewsQueue(final int from, final int to) {
        final List<String> queue = new ArrayList<>(this.getTempNewsQueue());
        if (from >= 0 && from < queue.size() && to >= 0 && to < queue.size()) {
            final String item = queue.remove(from);
            queue.add(to, item);
            this.config.set("news.queued", queue);
            this.plugin.saveConfig();
        }
    }

    public void clearNewsQueue() {
        this.config.set("news.queued", new ArrayList<>());
        this.plugin.saveConfig();
    }
}
