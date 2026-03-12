package com.criztiandev.extractionevent.gui;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * GUI for manually managing the live Warzone Shift for a region.
 * Allows starting, stopping, and viewing the remaining time.
 */
public class ShiftEventGui implements Gui {

    private static final int SIZE = 27;

    private static final int STATUS_SLOT = 13;
    private static final int TOGGLE_SLOT = 15;
    private static final int BACK_SLOT = 26;

    private final ExtractionEventPlugin plugin;
    private final LevRegion region;
    private final Inventory inventory;

    public ShiftEventGui(ExtractionEventPlugin plugin, LevRegion region) {
        this.plugin = plugin;
        this.region = region;
        this.inventory = Bukkit.createInventory(this, SIZE, "§8Shift: §d" + region.getId());
        render();
    }

    private void render() {
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        boolean isActive = plugin.getWarzoneShiftManager().isShiftActive(region.getId());
        long remaining = plugin.getWarzoneShiftManager().getRemainingSeconds(region.getId());

        String statusName = isActive ? "§a§lShift Active" : "§c§lShift Inactive";
        String timeStr = isActive ? String.format("%02d:%02d", remaining / 60, remaining % 60) : "N/A";
        
        inventory.setItem(STATUS_SLOT, makeItem(Material.CLOCK,
                statusName,
                "§7Remaining Time: §e" + timeStr,
                "",
                "§7Triggered by Legend Chests."));

        if (isActive) {
            inventory.setItem(TOGGLE_SLOT, makeItem(Material.REDSTONE_BLOCK,
                    "§c§lStop Shift",
                    "§7Click to immediately end",
                    "§7the active Warzone Shift."));
        } else {
            long defaultDuration = plugin.getConfig().getLong("warzone.warzone-shift-duration", 3600);
            inventory.setItem(TOGGLE_SLOT, makeItem(Material.EMERALD_BLOCK,
                    "§a§lStart Shift",
                    "§7Click to manually start",
                    "§7a Warzone Shift for §e" + defaultDuration + "s§7."));
        }

        inventory.setItem(BACK_SLOT, makeItem(Material.ARROW, "§cBack", "§7Return to Region Admin."));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            new RegionAdminGui(plugin, region, null).open(player);
            return;
        }

        if (slot == TOGGLE_SLOT) {
            boolean active = plugin.getWarzoneShiftManager().isShiftActive(region.getId());
            if (active) {
                plugin.getWarzoneShiftManager().stopShift(region.getId());
                player.sendMessage("§cStopped Warzone Shift in §e" + region.getId() + "§c.");
            } else {
                long duration = plugin.getConfig().getLong("warzone.warzone-shift-duration", 3600);
                plugin.getWarzoneShiftManager().startShift(region.getId(), duration);
                player.sendMessage("§aStarted Warzone Shift in §e" + region.getId() + " §afor " + duration + "s.");
            }
            render(); // Refresh GUI
        }
    }

    private ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore.length > 0 ? Arrays.asList(lore) : List.of());
            item.setItemMeta(meta);
        }
        return item;
    }
}
