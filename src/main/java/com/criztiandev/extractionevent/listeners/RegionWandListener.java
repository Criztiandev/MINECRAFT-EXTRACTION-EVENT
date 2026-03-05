package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.RegionSelection;
import com.criztiandev.extractionevent.utils.WandUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class RegionWandListener implements Listener {

    private final ExtractionEventPlugin plugin;

    public RegionWandListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !WandUtil.isWand(plugin, item)) return;

        if (!player.hasPermission("extractionevent.admin")) return;

        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (block == null) return;

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            RegionSelection sel = plugin.getRegionManager().getOrCreateSelection(player.getUniqueId());
            sel.setPos1(block.getLocation());
            player.sendMessage("§dPos 1 set to (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")");
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            RegionSelection sel = plugin.getRegionManager().getOrCreateSelection(player.getUniqueId());
            sel.setPos2(block.getLocation());
            player.sendMessage("§dPos 2 set to (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")");
        }
    }
}
