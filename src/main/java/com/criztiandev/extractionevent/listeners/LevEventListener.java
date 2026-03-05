package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LevEventListener implements Listener {

    private final ExtractionEventPlugin plugin;

    public LevEventListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (to == null) return;

            LevRegion regionTo = plugin.getRegionManager().getRegionAt(to);
            LevRegion regionFrom = plugin.getRegionManager().getRegionAt(from);

            // If escaping a region
            if (regionFrom != null && regionFrom.isBlockEnderPearl()) {
                if (regionTo == null || !regionTo.getId().equals(regionFrom.getId())) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cYou cannot ender pearl out of a lev region!");
                    return;
                }
            }

            // If entering a region from outside
            if (regionTo != null && regionTo.isBlockEnderPearl()) {
                if (regionFrom == null || !regionFrom.getId().equals(regionTo.getId())) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cYou cannot ender pearl into a lev region from the outside!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Location loc = event.getEntity().getLocation();
        LevRegion region = plugin.getRegionManager().getRegionAt(loc);
        
        if (region != null && region.isLightningOnDeath()) {
            // Strike visual lightning effect (no damage/fire)
            if (loc.getWorld() != null) {
                loc.getWorld().strikeLightningEffect(loc);
            }
        }
    }
}
