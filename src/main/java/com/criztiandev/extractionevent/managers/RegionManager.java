package com.criztiandev.extractionevent.managers;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import com.criztiandev.extractionevent.models.RegionSelection;
import org.bukkit.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegionManager {

    private final ExtractionEventPlugin plugin;
    private final Map<UUID, RegionSelection> selections = new HashMap<>();
    private final Map<String, LevRegion> regions = new ConcurrentHashMap<>();
    private final Map<String, List<LevRegion>> regionsByWorld = new ConcurrentHashMap<>();
    private final AtomicBoolean anyEnvoyEnabled = new AtomicBoolean(false);

    public RegionManager(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getStorageProvider().loadAllRegions().thenAccept(list -> {
            regions.clear();
            regionsByWorld.clear();

            for (LevRegion r : list) {
                regions.put(r.getId().toLowerCase(), r);
                regionsByWorld.computeIfAbsent(r.getWorld().toLowerCase(), k -> new CopyOnWriteArrayList<>()).add(r);
            }
            recomputeEnvoyFlag();
            plugin.getLogger().info("Loaded " + regions.size() + " lev regions.");
        });
    }

    public Collection<LevRegion> getRegions() {
        return regions.values();
    }

    public LevRegion getRegion(String id) {
        return regions.get(id.toLowerCase());
    }

    public LevRegion getRegionAt(Location location) {
        if (location.getWorld() == null) return null;

        // Avoid allocating a new COWAL on every world miss — return null immediately
        List<LevRegion> worldRegions = regionsByWorld.get(location.getWorld().getName().toLowerCase());
        if (worldRegions == null) return null;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        for (LevRegion region : worldRegions) {
            if (x >= region.getMinX() && x <= region.getMaxX() &&
                y >= region.getMinY() && y <= region.getMaxY() &&
                z >= region.getMinZ() && z <= region.getMaxZ()) {
                return region;
            }
        }
        return null;
    }

    public void saveRegion(LevRegion region) {
        LevRegion old = regions.put(region.getId().toLowerCase(), region);
        if (old != null && !old.getWorld().equalsIgnoreCase(region.getWorld())) {
            List<LevRegion> oldList = regionsByWorld.get(old.getWorld().toLowerCase());
            if (oldList != null) oldList.remove(old);
        }

        List<LevRegion> worldList = regionsByWorld.computeIfAbsent(region.getWorld().toLowerCase(), k -> new CopyOnWriteArrayList<>());
        if (!worldList.contains(region)) {
            worldList.add(region);
        }

        recomputeEnvoyFlag();
        plugin.getStorageProvider().saveRegion(region);
    }

    public void deleteRegion(String id) {
        LevRegion region = regions.remove(id.toLowerCase());
        if (region != null) {
            List<LevRegion> worldList = regionsByWorld.get(region.getWorld().toLowerCase());
            if (worldList != null) {
                worldList.remove(region);
            }
        }
        recomputeEnvoyFlag();
        plugin.getStorageProvider().deleteRegion(id);
    }

    /**
     * Returns true if at least one loaded region has envoyEventEnabled = true.
     * O(1) — reads a pre-computed flag rather than streaming all regions.
     */
    public boolean isAnyEnvoyEnabled() {
        return anyEnvoyEnabled.get();
    }

    private void recomputeEnvoyFlag() {
        anyEnvoyEnabled.set(regions.values().stream().anyMatch(LevRegion::isEnvoyEventEnabled));
    }

    public RegionSelection getOrCreateSelection(UUID uuid) {
        return selections.computeIfAbsent(uuid, k -> new RegionSelection());
    }

    public RegionSelection getSelection(UUID uuid) {
        return selections.get(uuid);
    }

    public void removeSelection(UUID uuid) {
        selections.remove(uuid);
    }
}
