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
import org.sayandev.MelodyPhone.manager.SpotifyManager;

import java.util.Collections;

public class SpotifyGUI extends AbstractGUI {

    private static final int[] TRACK_SLOTS = {12, 14, 30, 32};
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;

    private final SpotifyManager spotify;

    public SpotifyGUI(JavaPlugin plugin, GuiManager manager, SpotifyManager spotify) {
        super(plugin, manager);
        this.spotify = spotify;
    }

    @Override
    public void open(Player p) {
        inv = Bukkit.createInventory(null, 54, Component.empty());
        render();
        p.openInventory(inv);
    }

    private void render() {
        inv.setItem(TRACK_SLOTS[0], steveHead("Run On", "by Jamie Bower"));

        inv.setItem(PREV_SLOT, button(Material.RED_STAINED_GLASS_PANE, "Prev", NamedTextColor.RED));
        inv.setItem(NEXT_SLOT, button(Material.LIME_STAINED_GLASS_PANE, "Next", NamedTextColor.GREEN));
    }

    @Override
    public void onTopClick(InventoryClickEvent e) {
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == PREV_SLOT || slot == NEXT_SLOT) return;

        if (slot == TRACK_SLOTS[0]) {
            if (e.getClick().isLeftClick() || e.getClick().isRightClick()) {
                manager.close(p);
                spotify.playOrReject(p, SpotifyManager.DEFAULT_SOUND_ID);
            }
        }
    }

    @Override public void onBottomClick(InventoryClickEvent e) { e.setCancelled(true); }
    @Override public void onDrag(InventoryDragEvent e) { e.setCancelled(true); }
    @Override public void onClose(InventoryCloseEvent e) { }

    private ItemStack steveHead(String title, String subtitle) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.displayName(Component.text(title).color(NamedTextColor.AQUA));
        sm.lore(Collections.singletonList(Component.text(subtitle).color(NamedTextColor.GRAY)));
        sm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        head.setItemMeta(sm);
        return head;
    }

    private ItemStack button(Material mat, String name, NamedTextColor color) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text(name).color(color));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }
}