package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class LevEventListener implements Listener {

    private final ExtractionEventPlugin plugin;

    public LevEventListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Ender Pearl restrictions ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != TeleportCause.ENDER_PEARL) return;

        org.bukkit.entity.Player player = event.getPlayer();
        boolean isAdmin = player.hasPermission("extractionevent.admin");
        boolean isTestMode = plugin.isTestMode(player.getUniqueId());

        // Admins bypass restrictions unless they opted in to test mode
        if (isAdmin && !isTestMode) {
            return;
        }

        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;

        LevRegion regionFrom = plugin.getRegionManager().getRegionAt(from);
        LevRegion regionTo   = plugin.getRegionManager().getRegionAt(to);

        // Pearl thrown FROM inside → must land inside the same region
        if (regionFrom != null && regionFrom.isBlockEnderPearl()) {
            if (regionTo == null || !regionTo.getId().equals(regionFrom.getId())) {
                event.setCancelled(true);
                player.sendMessage("§c✖ §7You cannot ender pearl out of the warzone!");
                return;
            }
        }

        // Pearl landing INTO a restricted region from outside → block it
        if (regionTo != null && regionTo.isBlockEnderPearl()) {
            if (regionFrom == null || !regionFrom.getId().equals(regionTo.getId())) {
                event.setCancelled(true);
                player.sendMessage("§c✖ §7You cannot ender pearl into the warzone from outside!");
            }
        }
    }

    // ── Mimic death cleanup ───────────────────────────────────────────────────

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Warden warden)) return;

        if (warden.getCustomName() != null && warden.getCustomName().equals("§cMimic")) {
            LevRegion region = plugin.getRegionManager().getRegionAt(warden.getLocation());
            if (region != null) {
                plugin.getMimicManager().unregisterMimic(region.getId());
            }
        }
    }
}
