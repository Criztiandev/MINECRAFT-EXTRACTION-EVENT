package com.criztiandev.extractionevent.gui;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
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
 * Global Settings GUI for managing server-wide plugin configurations
 * like Test Mode for admins.
 */
public class GlobalSettingsGui implements Gui {

    static final String TITLE = "§8✦ §cGlobal Settings §8✦";

    private static final int SIZE          = 36;
    private static final int TEST_MODE_SLOT = 12;
    private static final int LOCKDOWN_SLOT  = 14;
    private static final int BACK_SLOT      = 27;

    private final ExtractionEventPlugin plugin;
    private final Inventory inventory;

    public GlobalSettingsGui(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
    }

    private void render(Player player) {
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, null);

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)  inventory.setItem(i, border);
        for (int i = 27; i < 36; i++) inventory.setItem(i, border);

        // Fill middle rows with inner filler
        ItemStack inner = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 9; i < 27; i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, inner);
        }

        boolean isTestMode = plugin.isTestMode(player.getUniqueId());
        
        Material mat = isTestMode ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = isTestMode ? "§aEnabled" : "§cDisabled";

        inventory.setItem(TEST_MODE_SLOT, makeItem(mat,
                "§e§lTest Mode",
                "§7When enabled, you temporarily",
                "§7bypass your admin privileges.",
                "§7This allows you to test",
                "§7warzone restrictions like",
                "§7Ender Pearls and Ender Chests.",
                "",
                "§7Status: " + status,
                "",
                "§eClick to toggle!"));

        // Lockdown & KOTH button — reflects live state
        var lm = plugin.getLockdownManager();
        Material lockMat;
        String lockName;
        String lockStatus;
        if (lm.isLockdownActive()) {
            lockMat    = Material.RED_CONCRETE;
            lockName   = "§4§l🔒 Lockdown & KOTH";
            lockStatus = "§4LOCKDOWN ACTIVE";
        } else if (lm.isKothActive()) {
            lockMat    = Material.ORANGE_CONCRETE;
            lockName   = "§6§l⚔ Lockdown & KOTH";
            lockStatus = "§6KOTH: §a" + lm.getKothRegionId();
        } else {
            lockMat    = Material.LIME_CONCRETE;
            lockName   = "§a§l🔓 Lockdown & KOTH";
            lockStatus = "§aAll Clear";
        }
        inventory.setItem(LOCKDOWN_SLOT, makeItem(lockMat,
                lockName,
                "§7Instantly lock extraction points",
                "§7or run a King of the Hill event.",
                "",
                "§7Status: " + lockStatus,
                "",
                "§eClick to manage!"));

        inventory.setItem(BACK_SLOT, makeItem(Material.ARROW, "§cBack", "§7Return to main menu."));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void open(Player player) {
        render(player);
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        
        switch (event.getSlot()) {
            case TEST_MODE_SLOT -> {
                plugin.toggleTestMode(player.getUniqueId());
                render(player);
            }
            case LOCKDOWN_SLOT -> new LockdownGui(plugin).open(player);
            case BACK_SLOT     -> new MainMenuGui(plugin).open(player);
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
