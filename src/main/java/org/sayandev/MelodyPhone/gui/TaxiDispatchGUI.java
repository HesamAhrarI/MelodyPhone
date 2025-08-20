package org.sayandev.MelodyPhone.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.sayandev.MelodyPhone.manager.GuiManager;
import org.sayandev.MelodyPhone.manager.TaxiManager;

import java.util.List;

public class TaxiDispatchGUI extends AbstractGUI {

    private static final int[] REQUEST_SLOTS = {12, 14, 30, 32};
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;
    private static final int PAGE_SIZE = 4;

    private final TaxiManager taxi;
    private int page = 0;

    public TaxiDispatchGUI(JavaPlugin plugin, GuiManager manager, TaxiManager taxi) {
        super(plugin, manager);
        this.taxi = taxi;
    }

    @Override
    public void open(Player p) {
        inv = Bukkit.createInventory(null, 54, Component.empty());
        render(p);
        p.openInventory(inv);
    }

    private void render(Player p) {
        List<Player> req = taxi.onlineRequestsSnapshot();
        int start = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= req.size()) break;
            Player target = req.get(idx);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(target);
            sm.displayName(Component.text(target.getName()).color(NamedTextColor.AQUA));
            sm.lore(java.util.Collections.singletonList(
                    Component.text("Click to accept").color(NamedTextColor.YELLOW)
            ));
            sm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            head.setItemMeta(sm);

            inv.setItem(REQUEST_SLOTS[i], head);
        }

        inv.setItem(PREV_SLOT, button(Material.RED_STAINED_GLASS_PANE, "Prev", NamedTextColor.RED));
        inv.setItem(NEXT_SLOT, button(Material.LIME_STAINED_GLASS_PANE, "Next", NamedTextColor.GREEN));
    }

    @Override
    public void onTopClick(InventoryClickEvent e) {
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == PREV_SLOT) {
            if (page > 0) { page--; render(p); }
            return;
        }
        if (slot == NEXT_SLOT) {
            List<Player> req = taxi.onlineRequestsSnapshot();
            if ((page + 1) * PAGE_SIZE < req.size()) { page++; render(p); }
            return;
        }

        for (int i = 0; i < REQUEST_SLOTS.length; i++) {
            if (slot == REQUEST_SLOTS[i]) {
                List<Player> req = taxi.onlineRequestsSnapshot();
                int idx = page * PAGE_SIZE + i;
                if (idx >= 0 && idx < req.size()) {
                    Player target = req.get(idx);
                    manager.close(p);
                    taxi.accept(p, target);
                }
                return;
            }
        }
    }

    @Override public void onBottomClick(InventoryClickEvent e) { e.setCancelled(true); }
    @Override public void onDrag(InventoryDragEvent e) { e.setCancelled(true); }
    @Override public void onClose(InventoryCloseEvent e) { }

    private ItemStack button(Material mat, String name, NamedTextColor color) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text(name).color(color));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }
}