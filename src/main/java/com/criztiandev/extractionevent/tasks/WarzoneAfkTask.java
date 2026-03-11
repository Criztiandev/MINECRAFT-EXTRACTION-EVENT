package com.criztiandev.extractionevent.tasks;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
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

/**
 * Detects AFK players inside warzone regions and teleports them to world spawn.
 *
 * "AFK" is defined as no position change (pitch/yaw rotation alone is ignored)
 * for longer than {@code warzone.afk-timeout-seconds} (default: 60s).
 *
 * This class is both a {@link Listener} (to track movement) and a
 * {@link BukkitRunnable} (to perform the periodic AFK check).
 * Register it as a listener AND schedule it as a timer in {@code ExtractionEventPlugin}.
 */
public class WarzoneAfkTask extends BukkitRunnable implements Listener {

    private final ExtractionEventPlugin plugin;
    private final long                  timeoutMillis;

    /** UUID → timestamp of last real (position-changing) movement. */
    private final Map<UUID, Long> lastMoved = new HashMap<>(64);

    public WarzoneAfkTask(ExtractionEventPlugin plugin) {
        this.plugin        = plugin;
        int timeoutSeconds = plugin.getConfig().getInt("warzone.afk-timeout-seconds", 60);
        this.timeoutMillis = timeoutSeconds * 1000L;
    }

    // ── Movement tracking ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Ignore pure head rotations — only real position changes count
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

    // ── Periodic AFK check ────────────────────────────────────────────────────

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            // Only check players currently inside a warzone region
            LevRegion region = plugin.getRegionPresenceTask().getCachedRegion(uuid);
            if (region == null) {
                // Player left the warzone — reset their AFK timer so they start fresh if they return
                lastMoved.remove(uuid);
                continue;
            }

            long moved = lastMoved.getOrDefault(uuid, now);
            long idleMs = now - moved;

            if (idleMs >= timeoutMillis) {
                teleportToSpawn(player);
                lastMoved.put(uuid, now); // reset so they aren't immediately kicked again on re-entry
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void teleportToSpawn(Player player) {
        Location spawn = player.getWorld().getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage("§c§l[Warzone] §r§7You were teleported to spawn for being §cAFK§7.");
        plugin.getLogger().info("[WarzoneAFK] " + player.getName() + " was teleported to spawn (AFK in warzone).");
    }
}
