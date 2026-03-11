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

    private static final int SIZE = 27;
    private static final int TEST_MODE_SLOT = 13;
    private static final int BACK_SLOT = 18;

    private final ExtractionEventPlugin plugin;
    private final Inventory inventory;

    public GlobalSettingsGui(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
    }

    private void render(Player player) {
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, null);

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 18; i < 27; i++) inventory.setItem(i, border);

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
            case BACK_SLOT -> new MainMenuGui(plugin).open(player);
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
