package com.criztiandev.extractionevent.gui;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.managers.LockdownManager;
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
 * Lockdown & KOTH control GUI.
 *
 * Row 1 — info / status header
 * Row 2 — [Lockdown ON/OFF]  [KOTH status]
 * Row 3 — KOTH region selector (up to 7 extraction regions shown)
 * Row 4 — Back
 */
public class LockdownGui implements Gui {

    static final String TITLE = "§8✦ §4Lockdown §8/ §6KOTH §8✦";

    private static final int SIZE         = 54;
    private static final int LOCKDOWN_SLOT = 11;
    private static final int KOTH_STOP_SLOT = 15;
    private static final int BACK_SLOT    = 49;
    // KOTH region buttons fill row 4 (slots 28-34)
    private static final int KOTH_ROW_START = 28;
    private static final int KOTH_ROW_END   = 34;

    private final ExtractionEventPlugin plugin;
    private final Inventory inventory;

    public LockdownGui(ExtractionEventPlugin plugin) {
        this.plugin    = plugin;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
        render();
    }

    private void render() {
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, filler());

        LockdownManager lm = plugin.getLockdownManager();

        // ── Status banner row 1 ─────────────────────────────────────────────
        Material bannerMat;
        String bannerName;
        String[] bannerLore;
        if (lm.isLockdownActive()) {
            bannerMat  = Material.RED_CONCRETE;
            bannerName = "§4§l⚠ LOCKDOWN ACTIVE";
            bannerLore = new String[]{"§7All extraction regions are §clocked§7.", "", "§cClick the button below to end lockdown."};
        } else if (lm.isKothActive()) {
            bannerMat  = Material.ORANGE_CONCRETE;
            bannerName = "§6§l⚔ KOTH ACTIVE";
            bannerLore = new String[]{"§7King region: §a" + lm.getKothRegionId(), "§7All other regions are §clocked§7.", "", "§eClick §6KOTH Stop §eto end."};
        } else {
            bannerMat  = Material.GREEN_CONCRETE;
            bannerName = "§a§l✔ All Clear";
            bannerLore = new String[]{"§7All extraction regions are §aopen§7.", "", "§7Use the buttons below to start a lockdown or KOTH event."};
        }
        for (int i = 0; i < 9; i++) inventory.setItem(i, makeItem(bannerMat, bannerName, bannerLore));

        // ── Lockdown button ─────────────────────────────────────────────────
        if (lm.isLockdownActive()) {
            inventory.setItem(LOCKDOWN_SLOT, makeItem(Material.RED_CONCRETE,
                    "§4§l🔒 End Lockdown",
                    "§7Click to §aopen§7 all extraction regions.",
                    "",
                    "§cCurrently: §4LOCKED"));
        } else {
            inventory.setItem(LOCKDOWN_SLOT, makeItem(Material.LIME_CONCRETE,
                    "§a§l🔓 Start Lockdown",
                    "§7Instantly §clock §7all extraction regions.",
                    "§7Cancels all active extractions.",
                    "",
                    "§cCurrently: §aOpen"));
        }

        // ── KOTH Stop button ────────────────────────────────────────────────
        if (lm.isKothActive()) {
            inventory.setItem(KOTH_STOP_SLOT, makeItem(Material.ORANGE_CONCRETE,
                    "§6§lEnd KOTH",
                    "§7End the KOTH event and open",
                    "§7all extraction regions.",
                    "",
                    "§eKing region: §a" + lm.getKothRegionId()));
        } else {
            inventory.setItem(KOTH_STOP_SLOT, makeItem(Material.GRAY_CONCRETE,
                    "§7§lKOTH — Not Active",
                    "§7To start KOTH, click a region",
                    "§7in the selector below."));
        }

        // ── KOTH region selector (row 4 only if no lockdown/KOTH active) ────
        if (!lm.isLockdownActive()) {
            List<String> regionIds = lm.getExtractionRegionIds();
            int slotIdx = KOTH_ROW_START;
            for (int i = 0; i < regionIds.size() && slotIdx <= KOTH_ROW_END; i++, slotIdx++) {
                String id = regionIds.get(i);
                boolean isKing = lm.isKothActive() && id.equalsIgnoreCase(lm.getKothRegionId());
                Material mat = isKing ? Material.GOLD_BLOCK : Material.YELLOW_STAINED_GLASS_PANE;
                String name = isKing ? "§6§l" + id + " §8(KOTH King)" : "§e§l" + id;
                inventory.setItem(slotIdx, makeItem(mat, name,
                        "§7Click to start KOTH with",
                        "§7§e" + id + " §7as the only open extraction point."));
            }
            if (regionIds.isEmpty()) {
                inventory.setItem(31, makeItem(Material.BARRIER,
                        "§cNo extraction regions found",
                        "§7ExtractionRegionEditor has no",
                        "§7configured EXTRACTION regions."));
            }
        }

        // ── Row label for KOTH selector ──────────────────────────────────────
        inventory.setItem(27, makeItem(Material.GOLD_INGOT, "§6§lKOTH — Pick King Region",
                "§7Click a region below to make it",
                "§7the only open extraction point."));

        // ── Back ─────────────────────────────────────────────────────────────
        inventory.setItem(BACK_SLOT, makeItem(Material.ARROW, "§cBack", "§7Return to Global Settings."));
    }

    @Override public Inventory getInventory() { return inventory; }
    @Override public void open(Player player)  { render(); player.openInventory(inventory); }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        LockdownManager lm = plugin.getLockdownManager();
        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            new GlobalSettingsGui(plugin).open(player);
            return;
        }

        if (slot == LOCKDOWN_SLOT) {
            if (lm.isLockdownActive()) {
                lm.stop(player);
            } else {
                if (lm.isKothActive()) {
                    player.sendMessage("§c✘ KOTH is active. Use the KOTH Stop button or §e/lev koth stop §cfirst.");
                } else {
                    lm.startLockdown(player);
                }
            }
            render();
            return;
        }

        if (slot == KOTH_STOP_SLOT && lm.isKothActive()) {
            lm.stop(player);
            render();
            return;
        }

        // KOTH region selector
        if (slot >= KOTH_ROW_START && slot <= KOTH_ROW_END && !lm.isLockdownActive()) {
            List<String> regionIds = lm.getExtractionRegionIds();
            int idx = slot - KOTH_ROW_START;
            if (idx < regionIds.size()) {
                String regionId = regionIds.get(idx);
                if (lm.isKothActive()) {
                    if (regionId.equalsIgnoreCase(lm.getKothRegionId())) {
                        player.sendMessage("§7§e" + regionId + " §7is already the KOTH king region.");
                    } else {
                        player.sendMessage("§c✘ KOTH is already active. Stop it first with §e/lev koth stop§c.");
                    }
                } else {
                    lm.startKoth(player, regionId);
                    render();
                }
            }
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
