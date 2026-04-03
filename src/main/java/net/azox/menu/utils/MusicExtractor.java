package net.azox.menu.utils;

import net.azox.menu.AzoxMenu;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;

public class MusicExtractor {

    private static boolean extracted = false;

    public static void extractBundledMusic(final AzoxMenu plugin) {
        if (extracted) {
            return;
        }

        final Path musicFolder = plugin.getDataFolder().toPath().resolve("music");
        
        if (!Files.exists(musicFolder)) {
            try {
                Files.createDirectories(musicFolder);
            } catch (final IOException e) {
                plugin.getLogger().warning("Could not create music folder: " + e.getMessage());
                return;
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
            final Path targetFile = musicFolder.resolve(fileName);
            
            if (Files.exists(targetFile)) {
                continue;
            }

            try (final InputStream stream = plugin.getResource("assets/" + fileName)) {
                if (stream != null) {
                    Files.copy(stream, targetFile);
                    plugin.getLogger().info("Extracted music: " + fileName);
                }
            } catch (final IOException e) {
                plugin.getLogger().warning("Failed to extract " + fileName + ": " + e.getMessage());
            }
        }

        extracted = true;
        plugin.getLogger().info("Music files ready in: " + musicFolder);
    }

    public static Path getMusicFolder(final JavaPlugin plugin) {
        return plugin.getDataFolder().toPath().resolve("music");
    }
}
