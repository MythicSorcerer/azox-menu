package net.azox.menu.listeners;

import net.azox.menu.AzoxMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ScoreboardListener implements Listener {

    private final AzoxMenu plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private boolean packetEventsAvailable = false;
    
    private Object packetEventsAPI;
    private Class<?> wrapperClass;
    private Class<?> scoreFormatClass;

    public ScoreboardListener(final AzoxMenu plugin) {
        this.plugin = plugin;
        this.checkPacketEvents();
    }

    private void checkPacketEvents() {
        try {
            final Class<?> packetEventsClass = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            final Object api = packetEventsClass.getMethod("getAPI").invoke(null);
            
            this.packetEventsAPI = api;
            this.wrapperClass = Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective");
            this.scoreFormatClass = Class.forName("com.github.retrooper.packetevents.protocol.scoreboard.ScoreFormat");
            
            this.packetEventsAvailable = true;
            this.plugin.getLogger().info("PacketEvents found - scoreboard numbers will be hidden.");
        } catch (final ClassNotFoundException e) {
            this.plugin.getLogger().warning("PacketEvents not found - scoreboard numbers will be visible.");
            this.plugin.getLogger().warning("Install PacketEvents to hide scoreboard numbers.");
        } catch (final Exception e) {
            this.plugin.getLogger().warning("Failed to initialize PacketEvents integration: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (!this.packetEventsAvailable) {
            return;
        }

        final Player player = event.getPlayer();
        
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.hideScoreboardNumbers(player);
        }, 3L);
    }

    private void hideScoreboardNumbers(final Player player) {
        if (!this.packetEventsAvailable) {
            return;
        }

        try {
            final Class<?> eventManagerClass = this.packetEventsAPI.getClass();
            final java.util.List<?> listeners = (java.util.List<?>) eventManagerClass.getMethod("getListeners").invoke(this.packetEventsAPI);
            
            for (final Object listener : listeners) {
                if (listener.getClass().getName().contains("AzoxMenu")) {
                    return;
                }
            }
            
            final Object myListener = java.lang.reflect.Proxy.newProxyInstance(
                ClassLoader.getSystemClassLoader(),
                new Class<?>[] { Class.forName("com.github.retrooper.packetevents.event.PacketListener") },
                (proxy, method, args) -> {
                    if ("onPacketSend".equals(method.getName())) {
                        final Object event = args[0];
                        final Class<?> eventClass = event.getClass();
                        
                        final Class<?> packetTypeClass = Class.forName("com.github.retrooper.packetevents.protocol.packettype.PacketType");
                        final Object packetType = packetTypeClass.getField("Play.Server.SCOREBOARD_OBJECTIVE").get(null);
                        
                        final Object packetTypeFromEvent = eventClass.getMethod("getPacketType").invoke(event);
                        
                        if (packetType.equals(packetTypeFromEvent)) {
                            final Object packet = this.wrapperClass.getConstructor(eventClass).newInstance(event);
                            final String name = (String) this.wrapperClass.getMethod("getObjectiveName").invoke(packet);
                            
                            if ("azoxmenu".equals(name)) {
                                final Object blankScore = this.scoreFormatClass.getMethod("blankScore").invoke(null);
                                this.wrapperClass.getMethod("setScoreFormat", this.scoreFormatClass).invoke(packet, blankScore);
                                eventClass.getMethod("markForReencode", boolean.class).invoke(event, true);
                            }
                        }
                    }
                    return null;
                }
            );
            
            eventManagerClass.getMethod("registerListener", Class.forName("com.github.retrooper.packetevents.event.PacketListener")).invoke(this.packetEventsAPI, myListener);
            
        } catch (final Exception e) {
            this.plugin.getLogger().warning("Failed to hide scoreboard numbers: " + e.getMessage());
        }
    }
}
