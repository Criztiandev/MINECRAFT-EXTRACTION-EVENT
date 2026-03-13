package com.criztiandev.extractionevent.tasks;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class RegionPresenceTask extends BukkitRunnable {

    private final ExtractionEventPlugin plugin;

    private Map<UUID, LevRegion> activeCache  = new HashMap<>(128);
    private Map<UUID, LevRegion> writeBuffer  = new HashMap<>(128);
    private Set<Long> hiddenPairs    = new HashSet<>(256);
    private Set<Long> newHiddenPairs = new HashSet<>(256);

    private final double combatRangeSq;
    private int tickCount = 0;
    private static final int VISIBILITY_TICKS = 2;

    public RegionPresenceTask(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        double range = plugin.getConfig().getDouble("warzone.combat-range", 48.0);
        this.combatRangeSq = range * range;
    }

    @Override
    public void run() {
        tickCount++;

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        writeBuffer.clear();
        List<Player> warzonePlayers = new ArrayList<>(80);

        for (Player player : online) {
            org.bukkit.Location loc = player.getLocation();
            LevRegion region = plugin.getRegionManager().getRegionAt(loc);

            UUID      uuid      = player.getUniqueId();
            LevRegion oldRegion = activeCache.get(uuid);

            if (region != null) {
                writeBuffer.put(uuid, region);
                warzonePlayers.add(player);
            }

            boolean wasInHideRegion = oldRegion != null && oldRegion.isHideNameTags();
            boolean isInHideRegion  = region   != null && region.isHideNameTags();

            if (isInHideRegion && !wasInHideRegion) {
                plugin.getNameTagManager().hideNameTag(player);
            } else if (!isInHideRegion && wasInHideRegion) {
                plugin.getNameTagManager().restoreNameTag(player);
            }

            // Minimap / radar hide — only on actual region change
            boolean regionChanged = (region != null) != (oldRegion != null)
                    || (region != null && !region.getId().equals(
                            oldRegion != null ? oldRegion.getId() : null));

            if (regionChanged) {
                if (region != null) {
                    if (plugin.getMinimapHideManager() != null) {
                        plugin.getMinimapHideManager().onPlayerEnterRegion(player, region);
                    }
                    if (plugin.getXaeroFairPlayManager() != null) {
                        plugin.getXaeroFairPlayManager().onPlayerEnterRegion(player);
                    }
                } else {
                    if (plugin.getMinimapHideManager() != null) {
                        plugin.getMinimapHideManager().onPlayerLeaveRegion(player, oldRegion);
                    }
                    if (plugin.getXaeroFairPlayManager() != null) {
                        plugin.getXaeroFairPlayManager().onPlayerLeaveRegion(player);
                    }
                }
            }

            // Restore combat visibility for players who just left the warzone
            if (oldRegion != null && region == null) {
                for (Player wz : warzonePlayers) {
                    wz.showPlayer(plugin, player);
                    player.showPlayer(plugin, wz);
                }
            }
        }

        // ── Throttled O(n²) visibility pass ──────────────────────────────────
        // Only run every VISIBILITY_TICKS calls (~every 2s when scheduled at 1s).
        // At 100 warzone players this saves ~4,900 iterations per second.
        if (tickCount >= VISIBILITY_TICKS) {
            tickCount = 0;
            runVisibilityPass(warzonePlayers);
        }

        // ── Swap double-buffer (zero allocation) ─────────────────────────────
        Map<UUID, LevRegion> tmp = activeCache;
        activeCache  = writeBuffer;
        writeBuffer  = tmp;

        // Keep the MinimapHide packet firewall's warzone set in sync
        if (plugin.getMinimapHideManager() != null) {
            plugin.getMinimapHideManager().setWarzoneSet(activeCache.keySet());
        }
    }

    private void runVisibilityPass(List<Player> warzonePlayers) {
        int count = warzonePlayers.size();
        if (count < 2) return; // Nothing to hide if 0 or 1 player

        // Reuse the pre-allocated buffer instead of allocating a new HashSet each time
        newHiddenPairs.clear();

        for (int i = 0; i < count; i++) {
            Player a = warzonePlayers.get(i);
            org.bukkit.Location la = a.getLocation();

            for (int j = i + 1; j < count; j++) {
                Player b = warzonePlayers.get(j);

                if (!la.getWorld().equals(b.getWorld())) continue;

                double distSq  = la.distanceSquared(b.getLocation());
                long   pairKey = pairKey(a.getUniqueId(), b.getUniqueId());

                if (distSq > combatRangeSq) {
                    newHiddenPairs.add(pairKey);
                    if (!hiddenPairs.contains(pairKey)) {
                        a.hidePlayer(plugin, b);
                        b.hidePlayer(plugin, a);
                    }
                } else {
                    if (hiddenPairs.contains(pairKey)) {
                        a.showPlayer(plugin, b);
                        b.showPlayer(plugin, a);
                    }
                }
            }
        }

        // Swap buffers: hiddenPairs ← newHiddenPairs (no allocation)
        Set<Long> tmp = hiddenPairs;
        hiddenPairs    = newHiddenPairs;
        newHiddenPairs = tmp;
    }

    // ── Public API for listeners ──────────────────────────────────────────────

    /** Returns true if the player is currently inside any warzone region. */
    public boolean isInAnyRegion(UUID uuid) {
        return activeCache.containsKey(uuid);
    }

    /** Returns the cached region the player is in, or null if outside. */
    public LevRegion getCachedRegion(UUID uuid) {
        return activeCache.get(uuid);
    }

    /** Returns the cached region id the player is in, or null. */
    public String getCachedRegionId(UUID uuid) {
        LevRegion r = activeCache.get(uuid);
        return r != null ? r.getId() : null;
    }

    public Set<UUID> getWarzonePlayerUUIDs() {
        return activeCache.keySet();
    }

    /**
     * Returns all players currently in the given region, read from the
     * PREVIOUS tick's stable activeCache snapshot. Using activeCache (not a
     * mid-loop index) guarantees all existing warzone members are present when
     * onPlayerEnterRegion fires during the current tick's iteration.
     */
    public List<Player> getCachedPlayersInRegion(LevRegion region) {
        List<Player> result = new ArrayList<>();
        for (Map.Entry<UUID, LevRegion> entry : activeCache.entrySet()) {
            if (entry.getValue().getId().equals(region.getId())) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) result.add(p);
            }
        }
        return result;
    }

    static long pairKey(UUID a, UUID b) {
        long la = a.getMostSignificantBits() ^ a.getLeastSignificantBits();
        long lb = b.getMostSignificantBits() ^ b.getLeastSignificantBits();
        long lo = Math.min(la, lb);
        long hi = Math.max(la, lb);
        return lo * 0x9e3779b97f4a7c15L ^ hi;
    }

    // ── Package-visible for tests ─────────────────────────────────────────────
    double getCombatRangeSq() { return combatRangeSq; }
    Set<Long> getHiddenPairs() { return hiddenPairs; }
}
