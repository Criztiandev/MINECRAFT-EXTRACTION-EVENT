package com.criztiandev.extractionevent.tasks;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarzoneAfkTask extends BukkitRunnable implements Listener {

    private final ExtractionEventPlugin plugin;
    private final long                  timeoutMillis;

    private final Map<UUID, Long> lastMoved = new HashMap<>(64);

    public WarzoneAfkTask(ExtractionEventPlugin plugin) {
        this.plugin        = plugin;
        int timeoutSeconds = plugin.getConfig().getInt("warzone.afk-timeout-seconds", 60);
        this.timeoutMillis = timeoutSeconds * 1000L;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        lastMoved.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastMoved.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (UUID uuid : plugin.getRegionPresenceTask().getWarzonePlayerUUIDs()) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player == null) continue;

            long moved  = lastMoved.getOrDefault(uuid, now);
            long idleMs = now - moved;

            if (idleMs >= timeoutMillis) {
                teleportToSpawn(player);
                lastMoved.put(uuid, now);
            }
        }
    }

    private void teleportToSpawn(Player player) {
        Location spawn = player.getWorld().getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage("§c§l[Warzone] §r§7You were teleported to spawn for being §cAFK§7.");
        plugin.getLogger().info("[WarzoneAFK] " + player.getName() + " was teleported to spawn (AFK in warzone).");
    }
}
