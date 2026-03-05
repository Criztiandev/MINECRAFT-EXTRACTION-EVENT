package com.criztiandev.extractionevent.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface Gui extends InventoryHolder {

    void open(Player player);

    void onClick(InventoryClickEvent event);

    default void onClose(InventoryCloseEvent event) {}

    default void onOpen(InventoryOpenEvent event) {}
}
