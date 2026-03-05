package com.criztiandev.extractionevent.utils;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class WandUtil {

    private static final String WAND_KEY = "lev-wand";

    public static ItemStack getWand(ExtractionEventPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String materialName = config.getString("wand.material", "BLAZE_ROD");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.BLAZE_ROD;

        ItemStack wand = new ItemStack(material);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            String name = config.getString("wand.name", "&dLev Region Selector");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            List<String> lore = config.getStringList("wand.lore");
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                        .collect(Collectors.toList()));
            }

            NamespacedKey key = new NamespacedKey(plugin, WAND_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }

        return wand;
    }

    public static boolean isWand(ExtractionEventPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, WAND_KEY);
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
