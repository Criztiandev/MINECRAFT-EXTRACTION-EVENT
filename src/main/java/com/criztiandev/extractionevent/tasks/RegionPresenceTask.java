package com.criztiandev.extractionevent.tasks;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
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
     * ~n*(n-1)/2 String allocations per tick (up to 3,160 at 80 warzone players).
     */
    private Set<Long> hiddenPairs = new HashSet<>(256);

    private final double combatRangeSq;

    public RegionPresenceTask(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        double range = plugin.getConfig().getDouble("warzone.combat-range", 48.0);
        this.combatRangeSq = range * range;
    }

    @Override
    public void run() {
        // ── Pass 1: locate each player in a region, build warzone list ────────
        Map<UUID, LevRegion> newSnapshot = new HashMap<>(Bukkit.getOnlinePlayers().size() * 2);
        List<Player> warzonePlayers = new ArrayList<>(80);

        for (Player player : Bukkit.getOnlinePlayers()) {
            LevRegion region = plugin.getRegionManager().getRegionAt(player.getLocation());
            if (region != null) {
                newSnapshot.put(player.getUniqueId(), region);
                warzonePlayers.add(player);
            }
        }

        // ── Pass 2: detect region changes and apply name-tag / visibility rules ─
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID      uuid      = player.getUniqueId();
            LevRegion newRegion = newSnapshot.get(uuid);
            LevRegion oldRegion = playerRegionCache.get(uuid);

            boolean wasInHideRegion = oldRegion != null && oldRegion.isHideNameTags();
            boolean isInHideRegion  = newRegion != null && newRegion.isHideNameTags();

            // Apply / remove name-tag concealment only when state actually changes
            if (isInHideRegion && !wasInHideRegion) {
                // Entered a hide-nametags region → conceal
                plugin.getNameTagManager().hideNameTag(player);
            } else if (!isInHideRegion && wasInHideRegion) {
                // Left a hide-nametags region → restore
                plugin.getNameTagManager().restoreNameTag(player);
            }

            // Minimap hide/show — fire on any region change (enter / leave / switch)
            boolean regionChanged = (newRegion != null) != (oldRegion != null)
                    || (newRegion != null && !newRegion.getId().equals(
                            oldRegion != null ? oldRegion.getId() : null));

            if (regionChanged && plugin.getMinimapHideManager() != null) {
                if (newRegion != null) {
                    plugin.getMinimapHideManager().onPlayerEnterRegion(player, newRegion);
                } else {
                    plugin.getMinimapHideManager().onPlayerLeaveRegion(player, oldRegion);
                }
            }

            // Restore combat visibility for players who just left the warzone entirely
            if (oldRegion != null && newRegion == null) {
                for (Player wz : warzonePlayers) {
                    wz.showPlayer(plugin, player);
                    player.showPlayer(plugin, wz);
                }
            }
        }

        // ── Pass 3: combat-range visibility for warzone players (O(n²) over wz) ─
        Set<Long> newHiddenPairs = new HashSet<>(hiddenPairs.size() + 16);
        int count = warzonePlayers.size();

        for (int i = 0; i < count; i++) {
            Player a = warzonePlayers.get(i);
            for (int j = i + 1; j < count; j++) {
                Player b = warzonePlayers.get(j);
                if (!a.getWorld().equals(b.getWorld())) continue;

                double distSq  = a.getLocation().distanceSquared(b.getLocation());
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
        playerRegionCache.clear();
        playerRegionCache.putAll(newSnapshot);
    }

    // ── Public API for listeners ──────────────────────────────────────────────

    /** Returns true if the player is currently inside any warzone region. */
    public boolean isInAnyRegion(UUID uuid) {
        return playerRegionCache.containsKey(uuid);
    }

    /** Returns the cached region id the player is in, or null. */
    public String getCachedRegionId(UUID uuid) {
        LevRegion r = playerRegionCache.get(uuid);
        return r != null ? r.getId() : null;
    }

    /** Returns the cached LevRegion for the player, or null if outside all regions. */
    public LevRegion getCachedRegion(UUID uuid) {
        return playerRegionCache.get(uuid);
    }

    // ── Pair-key encoding (zero String allocations) ───────────────────────────

    /**
     * Encodes a canonical, order-independent pair of two UUIDs as a single {@code long}.
     *
     * <p>Each UUID is folded to a {@code long} by XOR-ing its two 64-bit halves.
     * The two resulting longs are then combined so that {@code pairKey(a,b) == pairKey(b,a)}.
     *
     * <p><b>Collision risk:</b> extremely low for the expected scale (≤ 100 players,
     * at most 4,950 pairs). The construction is not cryptographically collision-free,
     * but is safe for a warzone visibility cache where a false positive only causes a
     * benign extra {@code showPlayer/hidePlayer} call.
     */
    static long pairKey(UUID a, UUID b) {
        long la = a.getMostSignificantBits() ^ a.getLeastSignificantBits();
        long lb = b.getMostSignificantBits() ^ b.getLeastSignificantBits();
        // Canonical order: put the smaller value "first" so pairKey(a,b) == pairKey(b,a)
        long lo = Math.min(la, lb);
        long hi = Math.max(la, lb);
        // Mix with a Fibonacci hashing constant keep distribution uniform
        return lo * 0x9e3779b97f4a7c15L ^ hi;
    }

    // ── Package-visible for tests ─────────────────────────────────────────────

    double getCombatRangeSq() { return combatRangeSq; }
    Set<Long> getHiddenPairs() { return hiddenPairs; }
}
