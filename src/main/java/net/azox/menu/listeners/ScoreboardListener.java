package net.azox.menu.listeners;

import net.azox.menu.AzoxMenu;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.entity.Player;

public class ScoreboardListener implements Listener {

    private final AzoxMenu plugin;
    private boolean packetEventsAvailable = false;
    private boolean checked = false;

    public ScoreboardListener(final AzoxMenu plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.checkPacketEvents();
        }, 20L);
    }

    private void checkPacketEvents() {
        if (this.checked) {
            return;
        }
        this.checked = true;
        
        try {
            final Class<?> packetEventsClass = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            final Object api = packetEventsClass.getMethod("getAPI").invoke(null);
            
            final Class<?> listenerClass = Class.forName("com.github.retrooper.packetevents.event.PacketListener");
            final Object eventManager = api.getClass().getMethod("getEventManager").invoke(api);
            
            final java.lang.reflect.Method registerMethod = eventManager.getClass().getMethod(
                "registerListener", listenerClass
            );
            
            final Class<?> wrapperClass = Class.forName(
                "com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective"
            );
            final Class<?> scoreFormatClass = Class.forName(
                "com.github.retrooper.packetevents.protocol.scoreboard.ScoreFormat"
            );
            final Class<?> eventClass = Class.forName("com.github.retrooper.packetevents.event.PacketSendEvent");
            
            final java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                if ("onPacketSend".equals(method.getName())) {
                    try {
                        final Object event = args[0];
                        
                        final Class<?> packetTypeClass = Class.forName("com.github.retrooper.packetevents.protocol.packettype.PacketType");
                        final Object scoreboardType = packetTypeClass.getField("Play.Server.SCOREBOARD_OBJECTIVE").get(null);
                        
                        final Object eventPacketType = eventClass.getMethod("getPacketType").invoke(event);
                        
                        if (scoreboardType.equals(eventPacketType)) {
                            final Object packet = wrapperClass.getConstructor(eventClass).newInstance(event);
                            
                            final String name = (String) wrapperClass.getMethod("getObjectiveName").invoke(packet);
                            
                            if ("azoxmenu".equals(name)) {
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
            this.plugin.getLogger().info("PacketEvents found - scoreboard numbers will be hidden.");
            
        } catch (final ClassNotFoundException e) {
            this.plugin.getLogger().warning("PacketEvents not found - scoreboard numbers will be visible.");
        } catch (final Exception e) {
            this.plugin.getLogger().warning("Failed to initialize PacketEvents: " + e.getClass().getSimpleName());
        }
    }
}
