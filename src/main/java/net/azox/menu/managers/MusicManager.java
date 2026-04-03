package net.azox.menu.managers;

import net.azox.menu.AzoxMenu;
import net.azox.menu.config.MusicConfig;
import net.azox.menu.utils.MusicExtractor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MusicManager {

    @Getter
    private static MusicManager instance;

    private final AzoxMenu plugin;
    private MusicConfig config;

    private final Set<String> musicFiles = new HashSet<>();
    private String currentTrack;
    private boolean isPlaying;

    private final Set<UUID> playersWithMusicEnabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> pendingRatings = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ratingPromptTimestamps = new ConcurrentHashMap<>();

    private BukkitTask playTask;
    private BukkitTask ratingPromptTask;
    private BukkitTask promptCleanupTask;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final long RATING_PROMPT_DURATION_MS = 15000;

    public MusicManager(final AzoxMenu plugin) {
        this.plugin = plugin;
        this.config = new MusicConfig(plugin);
        instance = this;

        MusicExtractor.extractBundledMusic(plugin);
        this.loadMusicFiles();
        this.startPlayTask();
        this.startRatingPromptCleanupTask();
    }

    public void reloadConfig() {
        this.config.load();
        this.loadMusicFiles();
    }

    private void loadMusicFiles() {
        this.musicFiles.clear();

        final Path musicFolder = this.config.getMusicFolder();
        
        if (Files.exists(musicFolder)) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(musicFolder, "*.mp3")) {
                for (final Path path : stream) {
                    final String fileName = path.getFileName().toString();
                    final String trackName = fileName.replace(".mp3", "").replace("_", " ");
                    this.musicFiles.add(trackName);
                }
            } catch (final IOException e) {
                this.plugin.getLogger().warning("Failed to load music from folder: " + e.getMessage());
            }
        }
        
        final String[] bundledMusic = {
            "Glass_at_the_Edge_of_Morning.mp3",
            "Pale_Horizon_Line.mp3",
            "The_Glass_Conservatory.mp3",
            "The_Glass_Floor.mp3",
            "The_Valley_Floor.mp3"
        };

        for (final String fileName : bundledMusic) {
            if (this.musicFiles.stream().noneMatch(t -> t.equals(fileName.replace(".mp3", "").replace("_", " ")))) {
                final InputStream stream = this.config.getMusicInputStream(fileName);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (final IOException ignored) {
                    }
                    final String trackName = fileName.replace(".mp3", "").replace("_", " ");
                    this.musicFiles.add(trackName);
                }
            }
        }

        this.plugin.getLogger().info("Loaded " + this.musicFiles.size() + " music files.");
    }

    private void startPlayTask() {
        this.playTask = Bukkit.getScheduler().runTaskTimer(
                this.plugin,
                () -> {
                    if (!this.config.isEnabled()) {
                        return;
                    }

                    final int onlineCount = Bukkit.getOnlinePlayers().size();
                    if (onlineCount < this.config.getMinPlayers()) {
                        return;
                    }

                    this.playRandomClip();
                },
                60L,
                this.config.getPlayIntervalTicks()
        );
    }

    private void startRatingPromptCleanupTask() {
        this.promptCleanupTask = Bukkit.getScheduler().runTaskTimer(
                this.plugin,
                () -> {
                    final long now = System.currentTimeMillis();
                    final Iterator<Map.Entry<UUID, Long>> iterator = this.ratingPromptTimestamps.entrySet().iterator();

                    while (iterator.hasNext()) {
                        final Map.Entry<UUID, Long> entry = iterator.next();
                        if (now - entry.getValue() > RATING_PROMPT_DURATION_MS) {
                            final Player player = Bukkit.getPlayer(entry.getKey());
                            if (player != null && player.isOnline()) {
                                this.sendRatingExpiredMessage(player);
                            }
                            iterator.remove();
                            this.pendingRatings.remove(entry.getKey());
                        }
                    }
                },
                20L,
                20L
        );
    }

    private void playRandomClip() {
        if (this.musicFiles.isEmpty()) {
            return;
        }

        final List<Player> eligiblePlayers = this.getEligiblePlayers();
        if (eligiblePlayers.isEmpty()) {
            return;
        }

        final Player targetPlayer = this.selectRandomPlayer(eligiblePlayers);
        if (targetPlayer == null) {
            return;
        }

        final List<String> trackList = new ArrayList<>(this.musicFiles);
        Collections.shuffle(trackList);
        this.currentTrack = trackList.get(0);
        this.isPlaying = true;

        this.playMusicToPlayer(targetPlayer);
        this.promptForRating(targetPlayer);
        this.refreshSidebars();
    }

    private List<Player> getEligiblePlayers() {
        final List<Player> eligible = new ArrayList<>();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (this.playersWithMusicEnabled.contains(player.getUniqueId())) {
                eligible.add(player);
            }
        }
        return eligible;
    }

    private Player selectRandomPlayer(final List<Player> players) {
        if (players.isEmpty()) {
            return null;
        }

        if (Math.random() < this.config.getRandomPlayerChance()) {
            final Random random = new Random();
            return players.get(random.nextInt(players.size()));
        }

        return null;
    }

    public void playMusicToPlayer(final Player player) {
        final String trackFileName = this.currentTrack.replace(" ", "_") + ".mp3";
        
        try (final InputStream musicStream = this.config.getMusicInputStream(trackFileName)) {
            if (musicStream == null) {
                this.plugin.getLogger().warning("Music file not found: " + trackFileName);
                return;
            }
            
            final byte[] audioData = musicStream.readAllBytes();
            final String base64Audio = Base64.getEncoder().encodeToString(audioData);
            
            player.playSound(player.getLocation(), org.bukkit.Sound.MUSIC_DISC_13, 1.0f, 1.0f);
            
        } catch (final IOException exception) {
            this.plugin.getLogger().warning("Failed to play music: " + exception.getMessage());
        }
    }

    public void promptForRating(final Player player) {
        this.pendingRatings.put(player.getUniqueId(), 0);
        this.ratingPromptTimestamps.put(player.getUniqueId(), System.currentTimeMillis());

        final Component message = this.miniMessage.deserialize(
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<newline>" +
                "<gold>Now Playing: <white>" + this.currentTrack + "<newline>" +
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<newline>" +
                "<yellow>How would you rate this track?<newline>" +
                "<green>[1] <gray>Terrible  " +
                "<green>[2] <gray>Bad  " +
                "<green>[3] <gray>Okay  " +
                "<green>[4] <gray>Good  " +
                "<green>[5] <gray>Amazing<newline>" +
                "<red>Type /music off <gray>to disable custom music"
        );

        player.sendMessage(message);
    }

    private void sendRatingExpiredMessage(final Player player) {
        final Component message = this.miniMessage.deserialize(
                "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<newline>" +
                "<red>Rating prompt expired. Type /music off to disable custom music."
        );
        player.sendMessage(message);
    }

    public void submitRating(final Player player, final int rating) {
        if (rating < 1 || rating > 5) {
            return;
        }

        final Integer current = this.pendingRatings.get(player.getUniqueId());
        if (current == null) {
            return;
        }

        this.pendingRatings.put(player.getUniqueId(), rating);
        this.ratingPromptTimestamps.remove(player.getUniqueId());

        final Component thanksMessage = this.miniMessage.deserialize(
                "<green>Thanks for rating! You gave <gold>" + this.currentTrack + " <green>a " + rating + "/5!"
        );
        player.sendMessage(thanksMessage);
    }

    public void enableMusic(final Player player) {
        this.playersWithMusicEnabled.add(player.getUniqueId());

        final Component message = this.miniMessage.deserialize(
                "<green>Custom music enabled! You may hear tracks randomly."
        );
        player.sendMessage(message);
    }

    public void disableMusic(final Player player) {
        this.playersWithMusicEnabled.remove(player.getUniqueId());
        this.pendingRatings.remove(player.getUniqueId());
        this.ratingPromptTimestamps.remove(player.getUniqueId());

        final Component message = this.miniMessage.deserialize(
                "<yellow>Custom music disabled. Type <white>/music on <yellow>to enable again."
        );
        player.sendMessage(message);
    }

    public boolean hasMusicEnabled(final Player player) {
        return this.playersWithMusicEnabled.contains(player.getUniqueId());
    }

    public boolean hasPendingRating(final Player player) {
        return this.pendingRatings.containsKey(player.getUniqueId());
    }

    public void onPlayerJoin(final Player player) {
        if (this.config.isEnabled()) {
            this.enableMusic(player);
        }
    }

    public void onPlayerQuit(final Player player) {
        this.playersWithMusicEnabled.remove(player.getUniqueId());
        this.pendingRatings.remove(player.getUniqueId());
        this.ratingPromptTimestamps.remove(player.getUniqueId());
    }

    private void refreshSidebars() {
        final SidebarManager sidebarManager = this.plugin.getSidebarManager();
        if (sidebarManager != null) {
            sidebarManager.refreshAllPlayers();
        }
    }

    public void playMusicManually(final Player player) {
        if (this.musicFiles.isEmpty()) {
            final Component error = this.miniMessage.deserialize("<red>No music files available.");
            player.sendMessage(error);
            return;
        }

        final List<String> trackList = new ArrayList<>(this.musicFiles);
        Collections.shuffle(trackList);
        this.currentTrack = trackList.get(0);
        this.isPlaying = true;

        this.playMusicToPlayer(player);

        final Component message = this.miniMessage.deserialize(
                "<green>Now playing: <gold>" + this.currentTrack
        );
        player.sendMessage(message);
    }

    public String getCurrentTrackDisplay() {
        if (!this.isPlaying || this.currentTrack == null) {
            return "None";
        }
        return this.currentTrack;
    }

    public void cleanup() {
        if (this.playTask != null) {
            this.playTask.cancel();
        }
        if (this.ratingPromptTask != null) {
            this.ratingPromptTask.cancel();
        }
        if (this.promptCleanupTask != null) {
            this.promptCleanupTask.cancel();
        }

        this.playersWithMusicEnabled.clear();
        this.pendingRatings.clear();
        this.ratingPromptTimestamps.clear();

        instance = null;
    }
}
