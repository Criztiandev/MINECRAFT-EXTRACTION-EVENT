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


    private Player findGroupPlayer(List<Player> players) {
        int n = players.size();

        org.bukkit.Location[] locs = new org.bukkit.Location[n];
        for (int i = 0; i < n; i++) {
            locs[i] = players.get(i).getLocation();
        }

        for (int i = 0; i < n; i++) {
            int nearbyCount = 1;
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                if (!locs[i].getWorld().equals(locs[j].getWorld())) continue;
                if (locs[i].distanceSquared(locs[j]) <= GROUP_DISTANCE_SQ) {
                    nearbyCount++;
                }
            }
            if (nearbyCount >= GROUP_SIZE_REQUIREMENT) return players.get(i);
        }
        return null;
    }
}
