package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles warzone kill effects and kill announcements.
 *
 * Behaviour (preserving anonymity by design):
 *   - On player death inside a warzone with `killEffectEnabled = true`:
 *       1. Strikes a cosmetic lightning bolt at the victim's death location so
 *          every nearby player sees WHERE the kill happened (without knowing WHO).
 *       2. The `lightningOnDeath` flag controls the lightning; `killEffectEnabled`
 *          is the master switch for this entire listener per region.
 *   - Does NOT reveal real names in chat (the user intentionally wants players to
 *     guess — the lightning gives spatial context only).
 *   - The default death message is cleared to prevent identity leakage since
 *     player.getDisplayName() would leak "§8Anonymous" anyway and looks ugly.
 *     A neutral "§8[Warzone] §7A player has died." is shown instead.
 */
public class KillEffectListener implements Listener {

    private final ExtractionEventPlugin plugin;

    public KillEffectListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Location deathLoc = victim.getLocation();

        String regionId = plugin.getRegionPresenceTask().getCachedRegionId(victim.getUniqueId());
        if (regionId == null) return;

        LevRegion region = plugin.getRegionManager().getRegion(regionId);
        if (region == null || !region.isKillEffectEnabled()) return;

        event.setDeathMessage(null);

        if (region.isLightningOnDeath()) {
            deathLoc.getWorld().strikeLightningEffect(deathLoc);
            // Play a global thunder sound so everyone hears it loud and clear (Hunger Games style)
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            }
        }

        Player killer = victim.getKiller();
        String adminMsg;
        if (killer != null) {
            adminMsg = "§8[§cLev Monitor§8] §c" + killer.getName() + " §7killed §c" + victim.getName() + " §7in §e" + region.getId();
        } else {
            adminMsg = "§8[§cLev Monitor§8] §c" + victim.getName() + " §7died in §e" + region.getId();
        }

        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("extractionevent.admin") && !plugin.isTestMode(p.getUniqueId())) {
                p.sendMessage(adminMsg);
            }
        }
    }
}
