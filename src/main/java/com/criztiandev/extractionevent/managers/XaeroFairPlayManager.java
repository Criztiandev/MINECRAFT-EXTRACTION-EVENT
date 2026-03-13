package com.criztiandev.extractionevent.managers;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.managers.AdminMonitorManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class XaeroFairPlayManager {

    private static final String XAERO_ENABLE_FAIRPLAY  = "\u00a7f\u00a7a\u00a7i\u00a7r\u00a7x\u00a7a\u00a7e\u00a7r\u00a7o";
    private static final String XAERO_DISABLE_FAIRPLAY = "\u00a7f\u00a7a\u00a7i\u00a7r\u00a72";

    private static final String JM_CHANNEL = "journeymap:server_admin";
    private static final byte[] JM_DISABLE_MAP = "{\"radar_enabled\":false,\"surface_mapping_enabled\":false,\"cave_mapping_enabled\":false,\"death_waypoints_enabled\":false}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final byte[] JM_ENABLE_MAP  = "{\"radar_enabled\":true,\"surface_mapping_enabled\":true,\"cave_mapping_enabled\":true,\"death_waypoints_enabled\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private final ExtractionEventPlugin plugin;
    private final Object minimapControlPlugin;
    private final Method sendConfigMethod;
    private final Class<?> spigotPlayerClass;
    private final Set<UUID> warzoneSet = ConcurrentHashMap.newKeySet(64);

    public XaeroFairPlayManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, JM_CHANNEL);

        Object mc = Bukkit.getPluginManager().getPlugin("MinimapControl");
        Method sendMethod = null;
        Class<?> spigotClass = null;

        if (mc != null) {
            try {
                spigotClass = Class.forName("com.funniray.minimap.spigot.impl.SpigotPlayer");
                Class<?> minimapPlayerInterface = Class.forName("com.funniray.minimap.common.api.MinimapPlayer");

                // sendConfig is declared on JavaMinimapPlugin (superclass), not SpigotMinimap itself
                Class<?> searchClass = mc.getClass();
                Method found = null;
                while (searchClass != null && found == null) {
                    try { found = searchClass.getDeclaredMethod("sendConfig", minimapPlayerInterface); }
                    catch (NoSuchMethodException ignored) {}
                    searchClass = searchClass.getSuperclass();
                }
                if (found == null) throw new NoSuchMethodException("sendConfig not found in class hierarchy");
                found.setAccessible(true);
                sendMethod = found;
                plugin.getLogger().info("MinimapControl detected — native radar suppression active (Xaero + JourneyMap + VoxelMap).");
            } catch (Exception e) {
                plugin.getLogger().warning("MinimapControl API mismatch — chat-code + JM channel fallback active: " + e.getMessage());
                spigotClass = null;
                sendMethod = null;
            }
        } else {
            plugin.getLogger().info("MinimapControl not found — Xaero chat-code + JourneyMap channel active.");
        }

        this.minimapControlPlugin = sendMethod != null ? mc : null;
        this.sendConfigMethod     = sendMethod;
        this.spigotPlayerClass    = spigotClass;
    }

    public void onPlayerEnterRegion(Player player) {
        warzoneSet.add(player.getUniqueId());
        applyRadarControl(player, true);
    }

    public void onPlayerLeaveRegion(Player player) {
        warzoneSet.remove(player.getUniqueId());
        applyRadarControl(player, false);
    }

    public void cleanup() {
        for (UUID uuid : warzoneSet) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) applyRadarControl(p, false);
        }
        warzoneSet.clear();
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, JM_CHANNEL);
    }

    public boolean isInWarzone(UUID uuid) {
        return warzoneSet.contains(uuid);
    }


    private void applyRadarControl(Player player, boolean disableRadar) {
        // Admins with monitor:map toggled ON keep their full minimap
        if (plugin.getAdminMonitorManager().has(player, AdminMonitorManager.Feature.MAP)) return;

        if (minimapControlPlugin != null) {
            tryNativeSend(player);
        } else {
            sendXaeroFairPlay(player, disableRadar);
        }
        sendJourneyMapControl(player, disableRadar);
    }

    private void tryNativeSend(Player player) {
        try {
            Object spigotPlayer = spigotPlayerClass
                    .getConstructor(Player.class, minimapControlPlugin.getClass().getSuperclass())
                    .newInstance(player, minimapControlPlugin);
            sendConfigMethod.invoke(minimapControlPlugin, spigotPlayer);
        } catch (Exception ignored) {}
    }

    private void sendXaeroFairPlay(Player player, boolean disable) {
        player.spigot().sendMessage(ChatMessageType.SYSTEM,
                new TextComponent(disable ? XAERO_ENABLE_FAIRPLAY : XAERO_DISABLE_FAIRPLAY));
    }

    private void sendJourneyMapControl(Player player, boolean disableMap) {
        try {
            player.sendPluginMessage(plugin, JM_CHANNEL, disableMap ? JM_DISABLE_MAP : JM_ENABLE_MAP);
        } catch (Exception ignored) {}
    }
}
