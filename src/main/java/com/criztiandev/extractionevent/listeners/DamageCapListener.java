package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Warzone damage normalization — two independent caps applied in order:
 *
 * 1. MAX_SINGLE_HIT cap   — prevents custom-enchant / aura-skill burst damage from
 *                           exceeding a configurable ceiling per hit (default 18 HP = 9 hearts).
 * 2. Survival floor       — ensures no single hit kills instantly; player always
 *                           survives with at least MIN_SURVIVAL_HP (default 0.5 HP = ¼ heart).
 *
 * Both thresholds are read from config.yml for easy server-admin tuning.
 * Processing order: HIGH (after most plugins set damage, before MONITOR).
 *
 * Region membership is resolved via the {@code RegionPresenceTask} cache (O(1) map
 * lookup) instead of a spatial scan — avoids two full region scans per damage event.
 */
public class DamageCapListener implements Listener {

    private final ExtractionEventPlugin plugin;
    private final double maxSingleHit;
    private final double minSurvivalHp;

    public DamageCapListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        this.maxSingleHit  = plugin.getConfig().getDouble("warzone.max-single-hit",  18.0);
        this.minSurvivalHp = plugin.getConfig().getDouble("warzone.min-survival-hp",  0.5);
    }

    // ── Burst cap ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        LevRegion region = getCachedRegion(victim);
        if (region == null || !region.isDamageCapped()) return;

        double damage = event.getFinalDamage();
        if (damage > maxSingleHit) {
            event.setDamage(maxSingleHit);
        }
    }

    // ── Survival floor ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        LevRegion region = getCachedRegion(victim);
        if (region == null || !region.isDamageCapped()) return;

        double finalDamage = event.getFinalDamage();
        double currentHp   = victim.getHealth();

        if (finalDamage >= currentHp) {
            double cappedRaw = currentHp - minSurvivalHp;
            if (cappedRaw <= 0) {
                event.setCancelled(true);
                return;
            }
            event.setDamage(event.getDamage() - (finalDamage - cappedRaw));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves the warzone region the player is currently in via the
     * {@code RegionPresenceTask} cache — O(1), no spatial scan.
     */
    private LevRegion getCachedRegion(Player player) {
        return plugin.getRegionPresenceTask().getCachedRegion(player.getUniqueId());
    }
}
