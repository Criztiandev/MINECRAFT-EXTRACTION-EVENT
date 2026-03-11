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
import java.util.Arrays;
import java.util.List;

/**
 * Region list GUI (54 slots / 6 rows).
 *
 * Rows 1-5 (slots 0-44): one ENDER_EYE item per region, click → RegionAdminGui.
 * Row 6   (slots 45-53): glass pane border + Back button at slot 49.
 * Pagination: Prev arrow at 45, Next arrow at 53 when needed.
 */
public class RegionListGui implements Gui {

    private static final int SIZE            = 54;
    private static final int ITEMS_PER_PAGE  = 45;
    private static final int BACK_SLOT       = 49;
    private static final int PREV_SLOT       = 45;
    private static final int NEXT_SLOT       = 53;

    private final ExtractionEventPlugin plugin;
    private final List<LevRegion>       regions;
    private final int                   page;
    private final Inventory             inventory;

    public RegionListGui(ExtractionEventPlugin plugin) {
        this(plugin, 0);
    }

    public RegionListGui(ExtractionEventPlugin plugin, int page) {
        this.plugin    = plugin;
        this.regions   = new ArrayList<>(plugin.getRegionManager().getRegions());
        this.page      = page;
        this.inventory = Bukkit.createInventory(this, SIZE, "§8Select a Region...");
        render();
    }

    private void render() {
        // Fill bottom row with border
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = ITEMS_PER_PAGE; i < SIZE; i++) inventory.setItem(i, border);

        // Region items
        int start = page * ITEMS_PER_PAGE;
        int end   = Math.min(start + ITEMS_PER_PAGE, regions.size());
        for (int i = start; i < end; i++) {
            LevRegion region = regions.get(i);
            inventory.setItem(i - start, makeItem(Material.ENDER_EYE,
                    "§a" + region.getId(),
                    "§7World: §f" + region.getWorld(),
                    "§7Pos1: §f(" + region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ() + ")",
                    "§7Pos2: §f(" + region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ() + ")",
                    "",
                    "§eClick to manage this region."));
        }

        // Back
        inventory.setItem(BACK_SLOT, makeItem(Material.OAK_DOOR, "§cGo Back", "§7Return to the main menu."));

        // Pagination
        if (page > 0) {
            inventory.setItem(PREV_SLOT, makeItem(Material.ARROW, "§cPrevious Page"));
        }
        if (end < regions.size()) {
            inventory.setItem(NEXT_SLOT, makeItem(Material.ARROW, "§aNext Page"));
        }
    }

    @Override public Inventory getInventory() { return inventory; }

    @Override public void open(Player player) { player.openInventory(inventory); }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            new MainMenuGui(plugin).open(player);
            return;
        }
        if (slot == PREV_SLOT && page > 0) {
            new RegionListGui(plugin, page - 1).open(player);
            return;
        }
        if (slot == NEXT_SLOT) {
            int nextStart = (page + 1) * ITEMS_PER_PAGE;
            if (nextStart < regions.size()) {
                new RegionListGui(plugin, page + 1).open(player);
            }
            return;
        }

        // Region slot
        int regionIndex = page * ITEMS_PER_PAGE + slot;
        if (slot < ITEMS_PER_PAGE && regionIndex < regions.size()) {
            LevRegion region = regions.get(regionIndex);
            new RegionAdminGui(plugin, region, this).open(player);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
}
