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

    /**
     * Primary region cache — UUID → region the player is currently inside (null = outside).
     * Used by listeners as an O(1) alternative to getRegionAt() spatial scans.
     */
    private final Map<UUID, LevRegion> playerRegionCache = new HashMap<>(128);

    /**
     * Pair visibility set — encodes (playerA, playerB) pairs that are currently hidden
     * from each other due to distance. Using long keys instead of String eliminates
     * ~n*(n-1)/2 String allocations per tick (up to 4,950 at 100 warzone players).
     */
    private Set<Long> hiddenPairs = new HashSet<>(256);

    private final double combatRangeSq;

    /**
     * Pair-visibility is O(n²) over warzone players.
     * At 100 players that is 4,950 iterations. We throttle it to every 2 seconds
     * (every other invocation of run()) since sub-2s radar updates are imperceptible.
     */
    private int tickCount = 0;
    private static final int VISIBILITY_TICKS = 2; // run visibility pass every N run() calls

    public RegionPresenceTask(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        double range = plugin.getConfig().getDouble("warzone.combat-range", 48.0);
        this.combatRangeSq = range * range;
    }

    @Override
    public void run() {
        tickCount++;

        // ── Single-pass: locate each player + detect changes ─────────────────
        // Snapshot online players once — avoids two separate Bukkit calls
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        Map<UUID, LevRegion>  newSnapshot     = new HashMap<>(online.size() * 2);
        List<Player>           warzonePlayers  = new ArrayList<>(80);

        for (Player player : online) {
            // Cache location locally — avoids two getLocation() calls per player
            org.bukkit.Location loc = player.getLocation();
            LevRegion region = plugin.getRegionManager().getRegionAt(loc);

            UUID      uuid      = player.getUniqueId();
            LevRegion oldRegion = playerRegionCache.get(uuid);

            if (region != null) {
                newSnapshot.put(uuid, region);
                warzonePlayers.add(player);
            }

            // Detect state change — only when it actually changes
            boolean wasInHideRegion = oldRegion != null && oldRegion.isHideNameTags();
            boolean isInHideRegion  = region   != null && region.isHideNameTags();

            if (isInHideRegion && !wasInHideRegion) {
                plugin.getNameTagManager().hideNameTag(player);
            } else if (!isInHideRegion && wasInHideRegion) {
                plugin.getNameTagManager().restoreNameTag(player);
            }

            // Minimap hide/show — only on actual region change
            boolean regionChanged = (region != null) != (oldRegion != null)
                    || (region != null && !region.getId().equals(
                            oldRegion != null ? oldRegion.getId() : null));

            if (regionChanged && plugin.getMinimapHideManager() != null) {
                if (region != null) {
                    plugin.getMinimapHideManager().onPlayerEnterRegion(player, region);
                } else {
                    plugin.getMinimapHideManager().onPlayerLeaveRegion(player, oldRegion);
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

        // Swap caches
        playerRegionCache.clear();
        playerRegionCache.putAll(newSnapshot);
    }

    private void runVisibilityPass(List<Player> warzonePlayers) {
        int count = warzonePlayers.size();
        if (count < 2) return; // Nothing to hide if 0 or 1 player

        Set<Long> newHiddenPairs = new HashSet<>(hiddenPairs.size() + 16);

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

        hiddenPairs = newHiddenPairs;
    }

    // ── Public API for listeners ──────────────────────────────────────────────

    /** Returns true if the player is currently inside any warzone region. */
    public boolean isInAnyRegion(UUID uuid) {
        return playerRegionCache.containsKey(uuid);
    }

    /** Returns the cached region the player is in, or null if outside. */
    public LevRegion getCachedRegion(UUID uuid) {
        return playerRegionCache.get(uuid);
    }

    /** Returns the cached region id the player is in, or null. */
    public String getCachedRegionId(UUID uuid) {
        LevRegion r = playerRegionCache.get(uuid);
        return r != null ? r.getId() : null;
    }

    /**
     * Returns all players currently cached as being inside a given region.
     * Used by MinimapHideManager to avoid a redundant spatial scan.
     */
    public List<Player> getCachedPlayersInRegion(LevRegion region) {
        List<Player> result = new ArrayList<>();
        for (Map.Entry<UUID, LevRegion> entry : playerRegionCache.entrySet()) {
            if (entry.getValue().getId().equals(region.getId())) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) result.add(p);
            }
        }
        return result;
    }

    // ── Pair-key encoding (zero String allocations) ───────────────────────────

    /**
     * Encodes a canonical, order-independent pair of two UUIDs as a single {@code long}.
     */
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
