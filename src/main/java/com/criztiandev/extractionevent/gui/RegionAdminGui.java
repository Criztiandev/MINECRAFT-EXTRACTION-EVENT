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

import java.util.Arrays;
import java.util.List;

/**
 * Per-region admin GUI Category Folder (54-slot / 6 rows).
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────┐
 * │  Row 2 (Categories)                                 │
 * │  [10] Utils     [12] Mechanic                       │
 * │  [14] Catastrophe [16] World                        │
 * ├─────────────────────────────────────────────────────┤
 * │  Row 6 (Actions)            slots 45-53             │
 * │  [46] Teleport  [49] Delete  [53] Back              │
 * └─────────────────────────────────────────────────────┘
 */
public class RegionAdminGui implements Gui {

    private static final int SIZE = 54;

    // ── Category folder slots ─────────────────────────────────────────────────
    private static final int UTILS_SLOT       = 10;
    private static final int MECHANIC_SLOT    = 12;
    private static final int CATASTROPHE_SLOT = 14;
    private static final int WORLD_SLOT       = 16;

    // ── Live Events ───────────────────────────────────────────────────────────
    private static final int SHIFT_SLOT       = 31;

    // ── Row 6: Actions ────────────────────────────────────────────────────────
    private static final int TELEPORT_SLOT    = 46;
    private static final int DELETE_SLOT      = 49;
    private static final int BACK_SLOT        = 53;

    private final ExtractionEventPlugin plugin;
    private final LevRegion region;
    private final Gui parentGui;
    private final Inventory inventory;

    public RegionAdminGui(ExtractionEventPlugin plugin, LevRegion region, Gui parentGui) {
        this.plugin    = plugin;
        this.region    = region;
        this.parentGui = parentGui != null ? parentGui : new RegionListGui(plugin);
        this.inventory = Bukkit.createInventory(this, SIZE, "§8Manage: §d" + region.getId());
        render();
    }

    private void render() {
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, filler());

        // ── Categories ───────────────────────────────────────────────────────
        inventory.setItem(UTILS_SLOT, makeItem(Material.COMMAND_BLOCK,
                "§b§lUtils Event", "§7Configure protection, anti-cheat, and visual effects."));
        
        inventory.setItem(MECHANIC_SLOT, makeItem(Material.REDSTONE,
                "§e§lMechanic Event", "§7Configure custom zone mechanics."));
        
        inventory.setItem(CATASTROPHE_SLOT, makeItem(Material.TNT,
                "§c§lCatastrophe Event", "§7Configure destructive zone events."));
        
        inventory.setItem(WORLD_SLOT, makeItem(Material.GRASS_BLOCK,
                "§a§lWorld Event", "§7Configure environmental effects and behaviors."));

        // ── Live Events ───────────────────────────────────────────────────────
        inventory.setItem(SHIFT_SLOT, makeItem(Material.BEACON,
                "§d§lWarzone Shift (Live)", "§7Manage the active Legendary chest-triggered shift", "§7that temporarily disables custom enchants."));

        // ── Actions ──────────────────────────────────────────────────────────
        inventory.setItem(TELEPORT_SLOT, makeItem(Material.ENDER_PEARL,
                "§bTeleport to Region", "§7Teleports you to the centre of §f" + region.getId() + "§7."));

        inventory.setItem(DELETE_SLOT, makeItem(Material.BARRIER,
                "§cDelete Region", "§7Permanently removes this region. §cThis cannot be undone!"));

        inventory.setItem(BACK_SLOT, makeItem(Material.ARROW,
                "§cBack", "§7Return to the region list."));
    }

    @Override public Inventory getInventory() { return inventory; }
    @Override public void open(Player player) { player.openInventory(inventory); }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot      = event.getSlot();

        switch (slot) {
            case BACK_SLOT -> parentGui.open(player);

            case DELETE_SLOT -> {
                plugin.getRegionManager().deleteRegion(region.getId());
                player.sendMessage("§aRegion §e" + region.getId() + " §adeleted.");
                new RegionListGui(plugin).open(player);
            }

            case TELEPORT_SLOT -> teleportToCenter(player);

            // ── Categories ───────────────────────────────────────────────────
            case UTILS_SLOT       -> new UtilsEventGui(plugin, region).open(player);
            case MECHANIC_SLOT    -> new MechanicEventGui(plugin, region).open(player);
            case CATASTROPHE_SLOT -> new CatastropheEventGui(plugin, region).open(player);
            case WORLD_SLOT       -> new WorldEventGui(plugin, region).open(player);
            case SHIFT_SLOT       -> new ShiftEventGui(plugin, region).open(player);
        }
    }

    private void teleportToCenter(Player player) {
        int centerX = (region.getMinX() + region.getMaxX()) / 2;
        int centerZ = (region.getMinZ() + region.getMaxZ()) / 2;
        org.bukkit.World w = Bukkit.getWorld(region.getWorld());
        if (w == null) {
            player.sendMessage("§cWorld §e" + region.getWorld() + " §cnot found!");
            return;
        }
        int y = w.getHighestBlockYAt(centerX, centerZ) + 1;
        player.teleport(new Location(w, centerX + 0.5, y, centerZ + 0.5));
        player.sendMessage("§aTeleported to §e" + region.getId() + "§a.");
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

    private ItemStack filler() {
        return makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }
}
