package com.criztiandev.extractionevent.listeners;

import com.criztiandev.extractionevent.ExtractionEventPlugin;
import com.criztiandev.extractionevent.models.LevRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces "take-only" access to Ender Chests inside restricted warzone regions.
 * Players CAN open the ender chest and take items out, but CANNOT deposit any.
 */
public class EnderChestListener implements Listener {

    /** Actions that put items INTO the ender chest. */
    private static final Set<InventoryAction> DEPOSIT_ACTIONS = EnumSet.of(
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.HOTBAR_MOVE_AND_READD
    );

    private final ExtractionEventPlugin plugin;
    public EnderChestListener(ExtractionEventPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isRestricted(Player player) {
        if (!plugin.getRegionPresenceTask().isInAnyRegion(player.getUniqueId())) return false;
        LevRegion region = plugin.getRegionPresenceTask().getCachedRegion(player.getUniqueId());
        if (region == null || !region.isEnderChestRestricted()) return false;

        if (player.hasPermission("extractionevent.admin") && !plugin.isTestMode(player.getUniqueId())) {
            return false;
        }

        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        if (event.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST) return;
        
        if (!isRestricted(player)){
             return;
        }

        if (event.getClickedInventory() == null){
            return;
        }

        InventoryAction action = event.getAction();
        boolean clickedEnderChest = event.getClickedInventory().getType() == InventoryType.ENDER_CHEST;

        // Placing directly INTO the ender chest
        boolean isDeposit = clickedEnderChest && DEPOSIT_ACTIONS.contains(action);

        // Shift-click from player inventory → ender chest (MOVE_TO_OTHER_INVENTORY goes to ender chest)
        boolean isShiftDeposit = !clickedEnderChest && action == InventoryAction.MOVE_TO_OTHER_INVENTORY;

        if (isDeposit || isShiftDeposit) {
            event.setCancelled(true);
            player.sendMessage("§c✖ §7You cannot store items in the Ender Chest inside the warzone!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        if (event.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST) return;
        if (!isRestricted(player)) return;

        // If any raw slot id being dragged into belongs to the TOP inventory (Ender Chest slots are 0-26)
        boolean draggingIntoEnderChest = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < event.getView().getTopInventory().getSize()) {
                draggingIntoEnderChest = true;
                break;
            }
        }

        if (draggingIntoEnderChest) {
            event.setCancelled(true);
            player.sendMessage("§c✖ §7You cannot store items in the Ender Chest inside the warzone!");
        }
    }
}
