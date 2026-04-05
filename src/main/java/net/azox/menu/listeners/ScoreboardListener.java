package net.azox.menu.listeners;

import net.azox.menu.AzoxMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ScoreboardListener implements Listener {

    private final AzoxMenu plugin;
    private boolean packetEventsAvailable = false;
    private boolean listenerRegistered = false;

    public ScoreboardListener(final AzoxMenu plugin) {
        this.plugin = plugin;
        this.checkPacketEvents();
    }

    private void checkPacketEvents() {
        try {
            final Class<?> packetEventsClass = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            final Object api = packetEventsClass.getMethod("getAPI").invoke(null);
            
            final Class<?> listenerClass = Class.forName("com.github.retrooper.packetevents.event.PacketListener");
            final Object eventManager = api.getClass().getMethod("getEventManager").invoke(api);
            
            final java.lang.reflect.Method registerMethod = eventManager.getClass().getMethod(
                "registerListener", listenerClass
            );
            
            final java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                if ("onPacketSend".equals(method.getName())) {
                    try {
                        final Object event = args[0];
                        final Class<?> eventClass = event.getClass();
                        
                        final Class<?> packetTypeClass = Class.forName("com.github.retrooper.packetevents.protocol.packettype.PacketType");
                        final Object scoreboardType = packetTypeClass.getField("Play.Server.SCOREBOARD_OBJECTIVE").get(null);
                        
                        final Object eventPacketType = eventClass.getMethod("getPacketType").invoke(event);
                        
                        if (scoreboardType.equals(eventPacketType)) {
                            final Class<?> wrapperClass = Class.forName(
                                "com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective"
                            );
                            final Object packet = wrapperClass.getConstructor(eventClass).newInstance(event);
                            
                            final String name = (String) wrapperClass.getMethod("getObjectiveName").invoke(packet);
                            
                            if ("azoxmenu".equals(name)) {
                                final Class<?> scoreFormatClass = Class.forName(
                                    "com.github.retrooper.packetevents.protocol.scoreboard.ScoreFormat"
                                );
                                final Object blankFormat = scoreFormatClass.getMethod("blankScore").invoke(null);
                                
                                wrapperClass.getMethod("setScoreFormat", scoreFormatClass).invoke(packet, blankFormat);
                                eventClass.getMethod("markForReencode", boolean.class).invoke(event, true);
                            }
                        }
                    } catch (final Exception ignored) {
                    }
                }
                return null;
            };
            
            final Object listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[] { listenerClass },
                handler
            );
            
            registerMethod.invoke(eventManager, listener);
            this.packetEventsAvailable = true;
            this.listenerRegistered = true;
            this.plugin.getLogger().info("PacketEvents found - scoreboard numbers will be hidden.");
            
        } catch (final ClassNotFoundException e) {
            this.plugin.getLogger().warning("PacketEvents not found - scoreboard numbers will be visible.");
        } catch (final Exception e) {
            this.plugin.getLogger().warning("Failed to initialize PacketEvents: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (!this.packetEventsAvailable || this.listenerRegistered) {
            return;
        }
        
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            final ScoreboardListener listener = new ScoreboardListener(this.plugin);
        }, 1L);
    }
}
