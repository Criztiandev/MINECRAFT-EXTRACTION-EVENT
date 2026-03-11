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
 * Per-region admin GUI (54-slot / 6 rows).
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────┐
 * │  Row 1 (Player Protection)  slots 0-8               │
 * │  [10] NameTags  [11] EnderPearl  [12] AntiStasis    │
 * │  [13] EnderChest Restrict                           │
 * ├─────────────────────────────────────────────────────┤
 * │  Row 3 (Anti-Cheat)         slots 18-26             │
 * │  [19] FreeCam   [20] DamageCap                      │
 * ├─────────────────────────────────────────────────────┤
 * │  Row 4 (Visual Effects)     slots 27-35             │
 * │  [28] Lightning [29] Mimic  [30] Kill Effect        │
 * ├─────────────────────────────────────────────────────┤
 * │  Row 6 (Actions)            slots 45-53             │
 * │  [46] Teleport  [49] Delete  [53] Back              │
 * └─────────────────────────────────────────────────────┘
 */
public class RegionAdminGui implements Gui {

    private static final int SIZE = 54;

    // ── Category header slots ─────────────────────────────────────────────────
    private static final int HEADER_PROTECTION = 0;
    private static final int HEADER_ANTICHEAT  = 18;
    private static final int HEADER_VISUAL     = 27;

    // ── Row 1: Player Protection ──────────────────────────────────────────────
    private static final int NAME_TAG_SLOT       = 10;
    private static final int PEARL_SLOT          = 11;
    private static final int ENDER_CHEST_SLOT    = 13;

    // ── Row 3: Anti-Cheat ─────────────────────────────────────────────────────
    private static final int FREECAM_SLOT        = 19;
    private static final int DAMAGE_CAP_SLOT     = 20;

    // ── Row 4: Visual Effects ─────────────────────────────────────────────────
    private static final int LIGHTNING_SLOT      = 28;
    private static final int MIMIC_SLOT          = 29;
    private static final int KILL_EFFECT_SLOT    = 30;

    // ── Row 6: Actions ────────────────────────────────────────────────────────
    private static final int TELEPORT_SLOT       = 46;
    private static final int DELETE_SLOT         = 49;
    private static final int BACK_SLOT           = 53;

    private final ExtractionEventPlugin plugin;
    private final LevRegion region;
    private final Inventory inventory;

    public RegionAdminGui(ExtractionEventPlugin plugin, LevRegion region, Gui parentGui) {
        this.plugin    = plugin;
        this.region    = region;
        this.inventory = Bukkit.createInventory(this, SIZE, "§8Manage: §d" + region.getId());
        render();
    }

    private void render() {
        // Clear first
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, filler());

        // ── Category headers ─────────────────────────────────────────────────
        inventory.setItem(HEADER_PROTECTION, makeItem(Material.SHIELD,
                "§b§lPlayer Protection", "§7Toggle warzone restriction features."));
        inventory.setItem(HEADER_ANTICHEAT, makeItem(Material.IRON_SWORD,
                "§e§lAnti-Cheat", "§7Toggle exploit prevention systems."));
        inventory.setItem(HEADER_VISUAL, makeItem(Material.BLAZE_POWDER,
                "§6§lVisual Effects", "§7Toggle cosmetic kill & spawn effects."));

        // ── Row 1: Player Protection ─────────────────────────────────────────
        inventory.setItem(NAME_TAG_SLOT,    toggle(Material.NAME_TAG,
                "Hide Name Tags",
                "Players appear as §8Anonymous§7 — no name tags overhead.",
                region.isHideNameTags()));

        inventory.setItem(PEARL_SLOT,       toggle(Material.ENDER_EYE,
                "Block Ender Pearl",
                "Prevents pearling across region boundaries.",
                region.isBlockEnderPearl()));

        inventory.setItem(ENDER_CHEST_SLOT, toggle(Material.ENDER_CHEST,
                "Restrict Ender Chest",
                "Players may only TAKE items — no depositing in the warzone.",
                region.isEnderChestRestricted()));

        // ── Row 3: Anti-Cheat ────────────────────────────────────────────────
        inventory.setItem(FREECAM_SLOT,     toggle(Material.SPYGLASS,
                "Block FreeCam",
                "Blocks spectator mode, flight, and extended-reach interactions.",
                region.isFreeCamBlocked()));

        inventory.setItem(DAMAGE_CAP_SLOT,  toggle(Material.IRON_CHESTPLATE,
                "Damage Cap",
                "Caps one-hit damage (max " +
                        plugin.getConfig().getDouble("warzone.max-single-hit", 18) +
                        " HP) and prevents 1-tap kills.",
                region.isDamageCapped()));

        // ── Row 4: Visual Effects ────────────────────────────────────────────
        inventory.setItem(LIGHTNING_SLOT,   toggle(Material.LIGHTNING_ROD,
                "Death Lightning",
                "Strikes cosmetic lightning bolt at every kill location.",
                region.isLightningOnDeath()));

        inventory.setItem(MIMIC_SLOT,       toggle(Material.SCULK_SHRIEKER,
                "Spawn Mimic",
                "Spawns a Mimic Warden when 3+ players group up.",
                region.isSpawnMimic()));

        inventory.setItem(KILL_EFFECT_SLOT, toggle(Material.PAPER,
                "Kill Announcements",
                "Broadcasts an anonymous '§7A player has fallen §8⚡§7' on kills.",
                region.isKillEffectEnabled()));

        // ── Row 6: Actions ───────────────────────────────────────────────────
        inventory.setItem(TELEPORT_SLOT, makeItem(Material.ENDER_PEARL,
                "§bTeleport to Region",
                "§7Teleports you to the centre of §f" + region.getId() + "§7."));

        inventory.setItem(DELETE_SLOT, makeItem(Material.BARRIER,
                "§cDelete Region",
                "§7Permanently removes this region. §cThis cannot be undone!"));

        inventory.setItem(BACK_SLOT, makeItem(Material.ARROW,
                "§cBack", "§7Return to the region list."));
    }

    @Override public Inventory getInventory() { return inventory; }

    @Override public void open(Player player) { player.openInventory(inventory); }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot       = event.getSlot();

        switch (slot) {
            case BACK_SLOT -> new RegionListGui(plugin).open(player);

            case DELETE_SLOT -> {
                plugin.getRegionManager().deleteRegion(region.getId());
                player.sendMessage("§aRegion §e" + region.getId() + " §adeleted.");
                // Open a FRESH list — the old parentGui still holds the deleted region in its snapshot
                new RegionListGui(plugin).open(player);
            }

            case TELEPORT_SLOT -> teleportToCenter(player);

            // ── Protection toggles ────────────────────────────────────────
            case NAME_TAG_SLOT -> {
                region.setHideNameTags(!region.isHideNameTags());
                save(player, "Name tag hiding", region.isHideNameTags());
            }
            case PEARL_SLOT -> {
                region.setBlockEnderPearl(!region.isBlockEnderPearl());
                save(player, "Ender pearl blocking", region.isBlockEnderPearl());
            }
            case ENDER_CHEST_SLOT -> {
                region.setEnderChestRestricted(!region.isEnderChestRestricted());
                save(player, "Ender chest restriction", region.isEnderChestRestricted());
            }

            // ── Anti-cheat toggles ────────────────────────────────────────
            case FREECAM_SLOT -> {
                region.setFreeCamBlocked(!region.isFreeCamBlocked());
                save(player, "FreeCam blocking", region.isFreeCamBlocked());
            }
            case DAMAGE_CAP_SLOT -> {
                region.setDamageCapped(!region.isDamageCapped());
                save(player, "Damage cap", region.isDamageCapped());
            }

            // ── Visual toggles ────────────────────────────────────────────
            case LIGHTNING_SLOT -> {
                region.setLightningOnDeath(!region.isLightningOnDeath());
                save(player, "Death lightning", region.isLightningOnDeath());
            }
            case MIMIC_SLOT -> {
                region.setSpawnMimic(!region.isSpawnMimic());
                save(player, "Mimic spawns", region.isSpawnMimic());
            }
            case KILL_EFFECT_SLOT -> {
                region.setKillEffectEnabled(!region.isKillEffectEnabled());
                save(player, "Kill announcements", region.isKillEffectEnabled());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void save(Player player, String featureName, boolean newState) {
        plugin.getRegionManager().saveRegion(region);
        render(); // refresh GUI
        String stateStr = newState ? "§aEnabled" : "§cDisabled";
        player.sendMessage("§e" + featureName + " §7set to " + stateStr + "§7.");
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

    /** Creates a toggle button — green when enabled, red when disabled. */
    private ItemStack toggle(Material mat, String name, String description, boolean enabled) {
        String status   = enabled ? "§aEnabled" : "§cDisabled";
        Material wool   = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
        return makeItem(wool, (enabled ? "§a" : "§c") + name,
                "§7" + description,
                "",
                "§7Status: " + status,
                "",
                "§eClick to toggle.");
    }

    private ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = lore.length > 0 ? Arrays.asList(lore) : List.of();
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack filler() {
        return makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }
}
