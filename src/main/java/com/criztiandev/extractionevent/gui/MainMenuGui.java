package com.criztiandev.extractionevent.gui;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.utils.WandUtil;
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
 * Main hub GUI — 54 slots (6 rows).
 *
 * Layout:
 *   Rows 1, 6        : full BLACK_STAINED_GLASS_PANE border
 *   Rows 2-5 sides   : BLACK_STAINED_GLASS_PANE (col 0, col 8)
 *   Row 3 (centered) : [Slot 20] Select  [Slot 22] Manage  [Slot 24] Create
 *   Row 4 (centered) : [Slot 31] Get Wand
 *
 *   Region count shown on the Select button.
 */
public class MainMenuGui implements Gui {

    static final String TITLE = "§8✦ §dLev Regions §8✦";

    private static final int SIZE         = 54;
    private static final int SLOT_SELECT  = 20;
    private static final int SLOT_MANAGE  = 22;
    private static final int SLOT_CREATE   = 24;
    private static final int SLOT_WAND     = 31;
    private static final int SLOT_SETTINGS = 40;

    private final ExtractionEventPlugin plugin;
    private final Inventory inventory;

    public MainMenuGui(ExtractionEventPlugin plugin) {
        this.plugin    = plugin;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
        render();
    }

    private void render() {
        // Clear
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, null);

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack inner  = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        // Row 1 + Row 6: full border
        for (int i = 0; i <  9; i++) inventory.setItem(i,      border);
        for (int i = 45; i < 54; i++) inventory.setItem(i,      border);

        // Rows 2-5: side pillars (columns 0 and 8)
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9,     border);
            inventory.setItem(row * 9 + 8, border);
        }

        // Fill remaining interior with gray filler
        for (int i = 10; i < 45; i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, inner);
        }

        int regionCount = plugin.getRegionManager().getRegions().size();

        // ── Row 3 — main actions ─────────────────────────────────────────────

        // [20] Select / Browse regions
        inventory.setItem(SLOT_SELECT, makeItem(Material.COMPASS,
                "§b§lBrowse Regions",
                "§7View and manage your",
                "§7" + regionCount + " configured region" + (regionCount == 1 ? "" : "s") + ".",
                "",
                "§eClick to browse!"));

        // [22] Quick manage — opens list directly
        inventory.setItem(SLOT_MANAGE, makeItem(Material.WRITABLE_BOOK,
                "§e§lManage Regions",
                "§7Select a region and",
                "§7configure its features.",
                "",
                "§eClick to manage!"));

        // [24] Create new region
        inventory.setItem(SLOT_CREATE, makeItem(Material.NETHER_STAR,
                "§a§lCreate Region",
                "§71. Grab the wand below.",
                "§72. Left-click §fPos1§7, Right-click §fPos2§7.",
                "§73. Run §e/lrev create <name>§7.",
                "",
                "§eClick for a usage reminder."));

        // ── Row 4 — utility ──────────────────────────────────────────────────

        // [31] Region wand
        inventory.setItem(SLOT_WAND, makeItem(Material.BLAZE_ROD,
                "§d§lGet Region Wand",
                "§7Left-click a block  → §fSet Pos1§7.",
                "§7Right-click a block → §fSet Pos2§7.",
                "",
                "§eClick to receive the wand."));

        // [40] Global Settings
        inventory.setItem(SLOT_SETTINGS, makeItem(Material.COMMAND_BLOCK,
                "§c§lGlobal Settings",
                "§7Toggle test mode and",
                "§7other server-wide configs.",
                "",
                "§eClick to configure!"));
    }

    @Override public Inventory getInventory() { return inventory; }

    @Override public void open(Player player) { player.openInventory(inventory); }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        switch (event.getSlot()) {
            case SLOT_SELECT, SLOT_MANAGE -> new RegionListGui(plugin).open(player);
            case SLOT_CREATE -> {
                player.closeInventory();
                player.sendMessage("§d§l[Lev] §r§7How to create a region:");
                player.sendMessage("§e1. §7Get the wand: §e/lrev wand");
                player.sendMessage("§e2. §7Left-click a block to set §fPos1§7, right-click to set §fPos2§7.");
                player.sendMessage("§e3. §7Run §e/lrev create <name>§7 to save.");
            }
            case SLOT_WAND -> {
                player.closeInventory();
                player.getInventory().addItem(WandUtil.getWand(plugin));
                player.sendMessage("§a§l[Lev] §r§7You received the Region Wand.");
                player.sendMessage("§7Left-click → Pos1  |  Right-click → Pos2");
            }
            case SLOT_SETTINGS -> new GlobalSettingsGui(plugin).open(player);
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
