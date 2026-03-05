package com.criztiandev.extractionevent.tasks;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RegionPresenceTask extends BukkitRunnable {

    private final ExtractionEventPlugin plugin;

    public RegionPresenceTask(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            LevRegion region = plugin.getRegionManager().getRegionAt(player.getLocation());
            
            if (region != null && region.isHideNameTags()) {
                plugin.getNameTagManager().hideNameTag(player);
            } else {
                plugin.getNameTagManager().restoreNameTag(player);
            }
        }
    }
}
