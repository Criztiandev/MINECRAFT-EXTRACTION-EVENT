package com.criztiandev.extractionevent.gui;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RegionAdminGui implements Gui {

    private final ExtractionEventPlugin plugin;
    private final LevRegion region;
    private final MainMenuGui parentGui;
    private final Inventory inventory;

    private static final int DELETE_SLOT = 11;
    private static final int PEARL_TOGGLE_SLOT = 13;
    private static final int NAME_TAG_TOGGLE_SLOT = 14;
    private static final int LIGHTNING_TOGGLE_SLOT = 15;
    private static final int TELEPORT_SLOT = 22;
    private static final int BACK_SLOT = 26;

    public RegionAdminGui(ExtractionEventPlugin plugin, LevRegion region, MainMenuGui parentGui) {
        this.plugin = plugin;
        this.region = region;
        this.parentGui = parentGui;
        this.inventory = Bukkit.createInventory(this, 27, "§8Manage: §d" + region.getId());
        initializeItems();
    }

    private void initializeItems() {
        // Delete Item
        inventory.setItem(DELETE_SLOT, createGuiItem(Material.BARRIER, "§cDelete Region", "§7Click to permanently delete this region."));
        
        // Teleport Item
        inventory.setItem(TELEPORT_SLOT, createGuiItem(Material.ENDER_PEARL, "§bTeleport", "§7Click to teleport to the center of this region."));

        // Pearl Toggle Item
        inventory.setItem(PEARL_TOGGLE_SLOT, createGuiItem(Material.ENDER_EYE, "§aToggle Pearl Block", "§7Currently: " + (region.isBlockEnderPearl() ? "§aEnabled" : "§cDisabled"), "§7Click to toggle blocking pearling out of region."));

        // Name Tag Toggle Item
        inventory.setItem(NAME_TAG_TOGGLE_SLOT, createGuiItem(Material.NAME_TAG, "§aToggle Name Tags", "§7Currently Hidden: " + (region.isHideNameTags() ? "§aYes" : "§cNo"), "§7Click to toggle hiding player name tags in region."));

        // Lightning Toggle Item
        inventory.setItem(LIGHTNING_TOGGLE_SLOT, createGuiItem(Material.LIGHTNING_ROD, "§aToggle Death Lightning", "§7Currently: " + (region.isLightningOnDeath() ? "§aEnabled" : "§cDisabled"), "§7Click to toggle visual lightning strikes on death."));

        // Back Item
        inventory.setItem(BACK_SLOT, createGuiItem(Material.ARROW, "§cGo Back", "§7Return to main menu."));

        // Filler
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
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
            if (parentGui != null) {
                // Refresh main menu just in case things deleted
                new MainMenuGui(plugin).open(player);
            } else {
                player.closeInventory();
            }
        } else if (slot == DELETE_SLOT) {
            plugin.getRegionManager().deleteRegion(region.getId());
            player.sendMessage("§aRegion deleted.");
            if (parentGui != null) {
                 new MainMenuGui(plugin).open(player);
            } else {
                 player.closeInventory();
            }
        } else if (slot == TELEPORT_SLOT) {
            // Find center
            int centerX = (region.getMinX() + region.getMaxX()) / 2;
            int centerZ = (region.getMinZ() + region.getMaxZ()) / 2;
            org.bukkit.World w = Bukkit.getWorld(region.getWorld());
            if (w != null) {
                // Find highest block to safely TP to
                int highestY = w.getHighestBlockYAt(centerX, centerZ);
                Location loc = new Location(w, centerX + 0.5, highestY + 1, centerZ + 0.5);
                player.teleport(loc);
                player.sendMessage("§aTeleported to region " + region.getId() + ".");
            } else {
                player.sendMessage("§cCannot find world " + region.getWorld() + "!");
            }
        } else if (slot == PEARL_TOGGLE_SLOT) {
            region.setBlockEnderPearl(!region.isBlockEnderPearl());
            plugin.getRegionManager().saveRegion(region);
            initializeItems();
            player.sendMessage("§aPearl blocking set to: " + (region.isBlockEnderPearl() ? "Enabled" : "Disabled"));
        } else if (slot == NAME_TAG_TOGGLE_SLOT) {
            region.setHideNameTags(!region.isHideNameTags());
            plugin.getRegionManager().saveRegion(region);
            initializeItems();
            player.sendMessage("§aName tag hiding set to: " + (region.isHideNameTags() ? "Yes" : "No"));
        } else if (slot == LIGHTNING_TOGGLE_SLOT) {
            region.setLightningOnDeath(!region.isLightningOnDeath());
            plugin.getRegionManager().saveRegion(region);
            initializeItems();
            player.sendMessage("§aDeath lightning set to: " + (region.isLightningOnDeath() ? "Enabled" : "Disabled"));
        }
    }
    
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String l : lore) {
                loreList.add(l);
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
