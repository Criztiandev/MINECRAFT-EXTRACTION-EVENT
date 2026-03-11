package com.criztiandev.extractionevent.tasks;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MimicSpawnTask extends BukkitRunnable {

    private final ExtractionEventPlugin plugin;
    private static final double GROUP_DISTANCE_SQ = 15.0 * 15.0; // pre-squared
    private static final int GROUP_SIZE_REQUIREMENT = 3;

    public MimicSpawnTask(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        RegionPresenceTask presenceTask = plugin.getRegionPresenceTask();

        // Reuse RegionPresenceTask's already-computed cache instead of calling
        // getRegionAt(player.getLocation()) for every online player again.
        Map<LevRegion, List<Player>> regionPlayers = new HashMap<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            LevRegion region = presenceTask.getCachedRegion(p.getUniqueId());
            if (region != null && region.isSpawnMimic()) {
                regionPlayers.computeIfAbsent(region, k -> new ArrayList<>()).add(p);
            }
        }

        for (Map.Entry<LevRegion, List<Player>> entry : regionPlayers.entrySet()) {
            LevRegion region = entry.getKey();
            List<Player> players = entry.getValue();

            if (players.size() < GROUP_SIZE_REQUIREMENT) continue;
            if (!plugin.getMimicManager().canSpawn(region)) continue;

            Player spawnAt = findGroupPlayer(players);
            if (spawnAt != null) {
                plugin.getMimicManager().spawnMimic(region, spawnAt.getLocation());
            }
        }
    }

    /**
     * Finds the first player that has at least {@code GROUP_SIZE_REQUIREMENT} players
     * clustered within {@code GROUP_DISTANCE_SQ} of them (including themselves).
     * Uses squared distance to avoid sqrt per pair.
     */
    private Player findGroupPlayer(List<Player> players) {
        int n = players.size();
        for (int i = 0; i < n; i++) {
            Player p1 = players.get(i);
            int nearbyCount = 1;
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                Player p2 = players.get(j);
                if (!p1.getWorld().equals(p2.getWorld())) continue;
                if (p1.getLocation().distanceSquared(p2.getLocation()) <= GROUP_DISTANCE_SQ) {
                    nearbyCount++;
                }
            }
            if (nearbyCount >= GROUP_SIZE_REQUIREMENT) return p1;
        }
        return null;
    }
}
