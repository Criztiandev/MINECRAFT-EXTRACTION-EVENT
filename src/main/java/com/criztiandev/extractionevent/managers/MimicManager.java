package com.criztiandev.extractionevent.managers;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Warden;

import java.util.HashMap;
import java.util.Map;

public class MimicManager {

    private final ExtractionEventPlugin plugin;
    private final Map<String, Long> cooldowns = new HashMap<>(); // Region ID -> Last Spawn Time (ms)
    private final Map<String, Integer> activeMimics = new HashMap<>(); // Region ID -> Count

    private static final long COOLDOWN_TIME_MS = 5 * 60 * 1000L; // 5 minutes
    private static final int MAX_MIMICS_PER_REGION = 12;

    public MimicManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canSpawn(LevRegion region) {
        if (!region.isSpawnMimic()) return false;
        
        String id = region.getId().toLowerCase();
        
        // Check limits
        int currentCount = activeMimics.getOrDefault(id, 0);
        if (currentCount >= MAX_MIMICS_PER_REGION) {
            return false;
        }

        // Check cooldown
        if (cooldowns.containsKey(id)) {
            long lastSpawn = cooldowns.get(id);
            if (System.currentTimeMillis() - lastSpawn < COOLDOWN_TIME_MS) {
                return false;
            }
        }
        
        return true;
    }

    public void spawnMimic(LevRegion region, Location location) {
        if (!canSpawn(region)) return;
        
        if (location.getWorld() == null) return;
        
        String id = region.getId().toLowerCase();
        
        Warden warden = (Warden) location.getWorld().spawnEntity(location, EntityType.WARDEN);
        warden.setCustomName("§cMimic");
        warden.setCustomNameVisible(true);
        warden.setRemoveWhenFarAway(false);
        
        // Make it a tank
        if (warden.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            warden.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1000.0);
            warden.setHealth(1000.0);
        }

        // Apply Cooldown
        cooldowns.put(id, System.currentTimeMillis());
        
        // Increase count (This assumes count is tracked indefinitely, ideally we'd also track entity death to decrement)
        activeMimics.put(id, activeMimics.getOrDefault(id, 0) + 1);
        
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("Spawned Mimic in region " + region.getId() + ". Total: " + activeMimics.get(id));
        }
    }
    
    public void unregisterMimic(String regionId) {
        String id = regionId.toLowerCase();
        int count = activeMimics.getOrDefault(id, 0);
        if (count > 0) {
            activeMimics.put(id, count - 1);
        }
    }
}
