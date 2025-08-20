package org.sayandev.MelodyPhone.listener;

import org.sayandev.MelodyPhone.gui.PhoneGUI;
import org.sayandev.MelodyPhone.manager.GuiManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PhoneListener implements Listener {

    private final JavaPlugin plugin;
    private final GuiManager manager;

    public PhoneListener(JavaPlugin plugin, GuiManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack it = event.getItem();
        if (it == null || it.getType() != Material.BLAZE_ROD) return;
        if (it.getItemMeta() == null || !it.getItemMeta().hasDisplayName()) return;
        String stripped = ChatColor.stripColor(it.getItemMeta().getDisplayName());
        if (!"Phone".equalsIgnoreCase(stripped)) return;

        event.setCancelled(true);
        manager.open(event.getPlayer(), new PhoneGUI(plugin, manager));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (manager.isOurTop(p, e.getView().getTopInventory())) {
            manager.handleClick(e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (manager.isOurTop(p, e.getView().getTopInventory())) {
            manager.handleDrag(e);
        }
    }

    @EventHandler public void onClose(InventoryCloseEvent e) { manager.handleClose(e); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { manager.handleQuit(e.getPlayer()); }
}