package net.azox.menu.config;

import net.azox.menu.AzoxMenu;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
public class MusicConfig {

    private final AzoxMenu plugin;
    private FileConfiguration config;

    private boolean enabled;
    private int minPlayers;
    private int clipDurationSeconds;
    private int playIntervalSeconds;
    private double randomPlayerChance;
    private boolean bundleMusic;

    private boolean showMusic;
    private List<String> entryOrder;

    public MusicConfig(final AzoxMenu plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();

        this.enabled = this.config.getBoolean("music.enabled", true);
        this.minPlayers = this.config.getInt("music.min-players", 3);
        this.clipDurationSeconds = this.config.getInt("music.clip-duration", 30);
        this.playIntervalSeconds = this.config.getInt("music.play-interval", 60);
        this.randomPlayerChance = this.config.getDouble("music.random-player-chance", 0.3);
        this.bundleMusic = this.config.getBoolean("music.bundle-music", true);

        this.showMusic = this.config.getBoolean("sidebar.entries.show-music", true);
        
        final List<String> defaultOrder = new ArrayList<>();
        defaultOrder.add("music");
        defaultOrder.add("status");
        defaultOrder.add("playtime");
        this.entryOrder = this.config.getStringList("sidebar.entries.order");
        if (this.entryOrder.isEmpty()) {
            this.entryOrder = defaultOrder;
        }
    }

    public int getClipDurationTicks() {
        return this.clipDurationSeconds * 20;
    }

    public long getPlayIntervalTicks() {
        return this.playIntervalSeconds * 20L;
    }

    public Path getMusicFolder() {
        return this.plugin.getDataFolder().toPath().resolve("music");
    }
    
    public InputStream getMusicInputStream(final String fileName) {
        final Path externalFile = this.getMusicFolder().resolve(fileName);
        
        if (this.bundleMusic && Files.exists(externalFile)) {
            try {
                return Files.newInputStream(externalFile);
            } catch (final IOException e) {
                this.plugin.getLogger().warning("Failed to read music file: " + fileName);
            }
        }
        
        return this.plugin.getResource("assets/" + fileName);
    }
    
    public boolean hasExternalMusic(final String fileName) {
        return Files.exists(this.getMusicFolder().resolve(fileName));
    }
}
