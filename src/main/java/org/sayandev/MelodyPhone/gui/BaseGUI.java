package org.sayandev.MelodyPhone.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public interface BaseGUI {
    Inventory getInventory();
    void open(Player player);

    default void onTopClick(InventoryClickEvent e) {}
    default void onBottomClick(InventoryClickEvent e) {}
    default void onDrag(InventoryDragEvent e) {}
    default void onClose(InventoryCloseEvent e) {}
}