package com.criztiandev.extractionevent.managers;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active Warzone Shifts across all regions.
 * A Warzone Shift temporarily disables custom enchantments (e.g., from AdvancedEnchantments)
 * reverting the region back to Vanilla PvP rules.
 */
public class WarzoneShiftManager {

    private final ExtractionEventPlugin plugin;
    
    // Map of Region ID -> Expiration Timestamp in ms
    private final Map<String, Long> activeShifts = new ConcurrentHashMap<>();
    private int taskTaskId = -1;

    public WarzoneShiftManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    /** Start the repeating task to check for expired shifts */
    public void startTask() {
        if (taskTaskId != -1) return;
        // Check every 20 ticks (1 second) to minimize overhead.
        taskTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L).getTaskId();
    }

    /** Stop the repeating task (e.g. on plugin disable) */
    public void stopTask() {
        if (taskTaskId != -1) {
            Bukkit.getScheduler().cancelTask(taskTaskId);
            taskTaskId = -1;
        }
    }

    public void cleanup() {
        stopTask();
        activeShifts.clear();
    }

    /**
     * Start a Warzone Shift for a specific region.
     * @param regionId The ID of the region
     * @param durationSeconds How long the shift should last.
     */
    public void startShift(String regionId, long durationSeconds) {
        long expireTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        activeShifts.put(regionId.toLowerCase(), expireTime);
        broadcastShiftState(regionId, true);
    }

    /**
     * Stop a currently active Warzone Shift in the given region.
     * @param regionId The ID of the region
     * @return true if a shift was actually stopped, false otherwise.
     */
    public boolean stopShift(String regionId) {
        if (activeShifts.remove(regionId.toLowerCase()) != null) {
            broadcastShiftState(regionId, false);
            return true;
        }
        return false;
    }

    /**
     * Checks if a specific region currently has an active Warzone Shift.
     */
    public boolean isShiftActive(String regionId) {
        Long expireTime = activeShifts.get(regionId.toLowerCase());
        if (expireTime == null) return false;
        
        if (System.currentTimeMillis() >= expireTime) {
            // Lazy expiration in case ticker hasn't caught it
            stopShift(regionId);
            return false;
        }
        return true;
    }

    /**
     * Get the remaining duration of a shift in seconds. Returns 0 if not active.
     */
    public long getRemainingSeconds(String regionId) {
        Long expireTime = activeShifts.get(regionId.toLowerCase());
        if (expireTime == null) return 0;
        
        long remaining = expireTime - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000L : 0;
    }

    /**
     * Highly optimized method to short-circuit all mathematical enchant disable checks.
     * @return true if at least one region has an active shift.
     */
    public boolean isAnyShiftActive() {
        return !activeShifts.isEmpty();
    }
    
    /** Returns a set of all region IDs currently experiencing a shift */
    public Set<String> getActiveShiftRegions() {
        return activeShifts.keySet();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : activeShifts.entrySet()) {
            if (now >= entry.getValue()) {
                stopShift(entry.getKey()); // This also broadcasts
            }
        }
    }

    /** Broadcasts to all players currently inside the region. */
    private void broadcastShiftState(String regionId, boolean isStarting) {
        LevRegion region = plugin.getRegionManager().getRegion(regionId);
        if (region == null) return;
        
        org.bukkit.World w = Bukkit.getWorld(region.getWorld());
        if (w == null) return;

        String title = isStarting ? "§c§lWARZONE SHIFT" : "§a§lSHIFT ENDED";
        String subtitle = isStarting ? "§7Custom Enchants Disabled - Vanilla PvP Mode" : "§7Custom Enchants Restored";

        for (Player p : w.getPlayers()) {
            // Check if player is actually inside the boundary
            org.bukkit.Location loc = p.getLocation();
            if (loc.getX() >= region.getMinX() && loc.getX() <= region.getMaxX() &&
                loc.getZ() >= region.getMinZ() && loc.getZ() <= region.getMaxZ() &&
                loc.getY() >= region.getMinY() && loc.getY() <= region.getMaxY()) {
                
                p.sendTitle(title, subtitle, 10, 70, 20);
                p.playSound(p.getLocation(), isStarting ? org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL : org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
            }
        }
    }
}
