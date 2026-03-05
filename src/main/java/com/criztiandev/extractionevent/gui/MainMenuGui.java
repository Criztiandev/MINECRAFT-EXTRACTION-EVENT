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

import java.util.ArrayList;
import java.util.List;

public class MainMenuGui implements Gui {

    private final ExtractionEventPlugin plugin;
    private final Inventory inventory;
    private final List<LevRegion> regions;

    public MainMenuGui(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        this.regions = new ArrayList<>(plugin.getRegionManager().getRegions());
        
        int size = Math.max(9, ((regions.size() / 9) + 1) * 9);
        if (size > 54) size = 54; // Cap at double chest for now (paging not implemented)
        
        this.inventory = Bukkit.createInventory(this, size, "§dLev Regions");
        initializeItems();
    }

    private void initializeItems() {
        for (int i = 0; i < regions.size() && i < 54; i++) {
            LevRegion region = regions.get(i);
            ItemStack item = createGuiItem(Material.ENDER_EYE, "§a" + region.getId(),
                    "§7World: §f" + region.getWorld(),
                    "§7Pos1: §f(" + region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ() + ")",
                    "§7Pos2: §f(" + region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ() + ")",
                    "",
                    "§eClick to manage region!"
            );
            inventory.setItem(i, item);
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
        int slot = event.getSlot();
        if (slot >= 0 && slot < regions.size()) {
            LevRegion clickedRegion = regions.get(slot);
            Player player = (Player) event.getWhoClicked();
            new RegionAdminGui(plugin, clickedRegion, this).open(player);
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
