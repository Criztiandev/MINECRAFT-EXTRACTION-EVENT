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
 * Sub-GUI for "Utils Event" toggles.
 * Houses all the core protection, anti-cheat, and visual effect toggles.
 */
public class UtilsEventGui implements Gui {

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

    // ── Row 5: Integrations ───────────────────────────────────────────────────
    private static final int ENVOY_SLOT          = 37;

    // ── Row 6: Actions ────────────────────────────────────────────────────────
    private static final int BACK_SLOT           = 53;

    private final ExtractionEventPlugin plugin;
    private final LevRegion region;
    private final Inventory inventory;

    public UtilsEventGui(ExtractionEventPlugin plugin, LevRegion region) {
        this.plugin    = plugin;
        this.region    = region;
        this.inventory = Bukkit.createInventory(this, SIZE, "§8Utils: §d" + region.getId());
        render();
    }

    private void render() {
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, filler());

        inventory.setItem(HEADER_PROTECTION, makeItem(Material.SHIELD,
                "§b§lPlayer Protection", "§7Toggle warzone restriction features."));
        inventory.setItem(HEADER_ANTICHEAT, makeItem(Material.IRON_SWORD,
                "§e§lAnti-Cheat", "§7Toggle exploit prevention systems."));
        inventory.setItem(HEADER_VISUAL, makeItem(Material.BLAZE_POWDER,
                "§6§lVisual Effects", "§7Toggle cosmetic kill & spawn effects."));

        inventory.setItem(NAME_TAG_SLOT, toggle(Material.NAME_TAG,
                "Hide Name Tags", "Players appear as §8Anonymous§7 — no name tags overhead.", region.isHideNameTags()));
        inventory.setItem(PEARL_SLOT, toggle(Material.ENDER_EYE,
                "Block Ender Pearl", "Prevents pearling across region boundaries.", region.isBlockEnderPearl()));
        inventory.setItem(ENDER_CHEST_SLOT, toggle(Material.ENDER_CHEST,
                "Restrict Ender Chest", "Players may only TAKE items — no depositing in the warzone.", region.isEnderChestRestricted()));

        inventory.setItem(FREECAM_SLOT, toggle(Material.SPYGLASS,
                "Block FreeCam", "Blocks spectator mode, flight, and extended-reach interactions.", region.isFreeCamBlocked()));
        inventory.setItem(DAMAGE_CAP_SLOT, toggle(Material.IRON_CHESTPLATE,
                "Damage Cap", "Caps one-hit damage (max " + plugin.getConfig().getDouble("warzone.max-single-hit", 18) + " HP).", region.isDamageCapped()));

        inventory.setItem(LIGHTNING_SLOT, toggle(Material.LIGHTNING_ROD,
                "Death Lightning", "Strikes cosmetic lightning bolt at every kill location.", region.isLightningOnDeath()));
        inventory.setItem(MIMIC_SLOT, toggle(Material.SCULK_SHRIEKER,
                "Spawn Mimic", "Spawns a Mimic Warden when 3+ players group up.", region.isSpawnMimic()));
        inventory.setItem(KILL_EFFECT_SLOT, toggle(Material.PAPER,
                "Kill Announcements", "Broadcasts an anonymous '§7A player has fallen §8⚡§7' on kills.", region.isKillEffectEnabled()));

        inventory.setItem(ENVOY_SLOT, toggle(Material.ENCHANTING_TABLE,
                "Disable Custom Enchants", "Permanently suppress AdvancedEnchantments (Envoy).", region.isEnvoyEventEnabled()));

        inventory.setItem(BACK_SLOT, makeItem(Material.ARROW, "§cBack", "§7Return to Category Menu."));
    }

    @Override public Inventory getInventory() { return inventory; }
    @Override public void open(Player player) { player.openInventory(inventory); }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        switch (slot) {
            case BACK_SLOT -> new RegionAdminGui(plugin, region, null).open(player);

            case NAME_TAG_SLOT -> { region.setHideNameTags(!region.isHideNameTags()); save(player, "Name tag hiding", region.isHideNameTags()); }
            case PEARL_SLOT -> { region.setBlockEnderPearl(!region.isBlockEnderPearl()); save(player, "Ender pearl blocking", region.isBlockEnderPearl()); }
            case ENDER_CHEST_SLOT -> { region.setEnderChestRestricted(!region.isEnderChestRestricted()); save(player, "Ender chest restriction", region.isEnderChestRestricted()); }
            case FREECAM_SLOT -> { region.setFreeCamBlocked(!region.isFreeCamBlocked()); save(player, "FreeCam blocking", region.isFreeCamBlocked()); }
            case DAMAGE_CAP_SLOT -> { region.setDamageCapped(!region.isDamageCapped()); save(player, "Damage cap", region.isDamageCapped()); }
            case LIGHTNING_SLOT -> { region.setLightningOnDeath(!region.isLightningOnDeath()); save(player, "Death lightning", region.isLightningOnDeath()); }
            case MIMIC_SLOT -> { region.setSpawnMimic(!region.isSpawnMimic()); save(player, "Mimic spawns", region.isSpawnMimic()); }
            case KILL_EFFECT_SLOT -> { region.setKillEffectEnabled(!region.isKillEffectEnabled()); save(player, "Kill announcements", region.isKillEffectEnabled()); }
            case ENVOY_SLOT -> { region.setEnvoyEventEnabled(!region.isEnvoyEventEnabled()); save(player, "Custom enchants block", region.isEnvoyEventEnabled()); }
        }
    }

    private void save(Player player, String featureName, boolean newState) {
        plugin.getRegionManager().saveRegion(region);
        render(); // refresh GUI
        player.sendMessage("§e" + featureName + " §7set to " + (newState ? "§aEnabled" : "§cDisabled") + "§7.");
    }

    private ItemStack toggle(Material mat, String name, String description, boolean enabled) {
        String status = enabled ? "§aEnabled" : "§cDisabled";
        Material wool = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
        return makeItem(wool, (enabled ? "§a" : "§c") + name, "§7" + description, "", "§7Status: " + status, "", "§eClick to toggle.");
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
