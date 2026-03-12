package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionchest.events.ChestLootedEvent;
import com.criztiandev.extractionchest.models.ParentChestDefinition;
import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class LegendChestListener implements Listener {

    private final ExtractionEventPlugin plugin;

    public LegendChestListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChestLooted(ChestLootedEvent event) {
        Player player = event.getPlayer();
        ParentChestDefinition def = event.getDefinition();
        Location chestLoc = event.getChest().getLocation(player.getWorld());

        // Check if this chest is the legendary trigger
        String targetName = plugin.getConfig().getString("warzone.legend-chest-name", "LegendChest");
        if (!def.getName().equalsIgnoreCase(targetName)) {
            return;
        }

        // Check if the chest is inside an ExtractionEvent region
        LevRegion region = plugin.getRegionManager().getRegionAt(chestLoc);
        if (region == null) {
            return; // Chest is outside any warzone; do nothing
        }

        // Trigger the Warzone Shift for this specific region
        long duration = plugin.getConfig().getLong("warzone.warzone-shift-duration", 3600);
        plugin.getWarzoneShiftManager().startShift(region.getId(), duration);
    }
}
