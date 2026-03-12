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

public class WorldEventGui implements Gui {
    private static final int SIZE = 54;
    private static final int BACK_SLOT = 53;

    private final ExtractionEventPlugin plugin;
    private final LevRegion region;
    private final Inventory inventory;

    public WorldEventGui(ExtractionEventPlugin plugin, LevRegion region) {
        this.plugin = plugin;
        this.region = region;
        this.inventory = Bukkit.createInventory(this, SIZE, "§8World: §d" + region.getId());
        render();
    }

    private void render() {
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, filler());
        inventory.setItem(22, makeItem(Material.BARRIER, "§cComing Soon", "§7World features are under development."));
        inventory.setItem(BACK_SLOT, makeItem(Material.ARROW, "§cBack", "§7Return to Category Menu."));
    }

    @Override public Inventory getInventory() { return inventory; }
    @Override public void open(Player player) { player.openInventory(inventory); }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getSlot() == BACK_SLOT) {
            new RegionAdminGui(plugin, region, null).open((Player) event.getWhoClicked());
        }
    }

    private ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore.length > 0 ? Arrays.asList(lore) : List.of());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack filler() { return makeItem(Material.GRAY_STAINED_GLASS_PANE, " "); }
}
