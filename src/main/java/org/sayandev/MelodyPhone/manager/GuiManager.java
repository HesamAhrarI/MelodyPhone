package org.sayandev.MelodyPhone.manager;

import org.sayandev.MelodyPhone.gui.BaseGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager {

    public static final int HOTBAR_MID = 4;

    private final JavaPlugin plugin;
    public GuiManager(JavaPlugin plugin) { this.plugin = plugin; }

    private static class SavedInv {
        final ItemStack[] contents;
        final ItemStack[] armor;
        final ItemStack   offhand;
        SavedInv(Player p) {
            PlayerInventory inv = p.getInventory();
            contents = clone(inv.getContents());
            armor    = clone(inv.getArmorContents());
            offhand  = inv.getItemInOffHand() == null ? null : inv.getItemInOffHand().clone();
        }
        private static ItemStack[] clone(ItemStack[] a) {
            ItemStack[] o = new ItemStack[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i] == null ? null : a[i].clone();
            return o;
        }
    }

    private static class Session {
        final SavedInv saved;
        final BaseGUI gui;
        final Inventory top;
        Session(SavedInv saved, BaseGUI gui) {
            this.saved = saved;
            this.gui   = gui;
            this.top   = gui.getInventory();
        }
    }

    private final Map<UUID, Session> sessions = new HashMap<>();

    public void open(Player p, BaseGUI gui) {
        UUID id = p.getUniqueId();

        if (sessions.containsKey(id)) return;

        SavedInv snap = new SavedInv(p);

        gui.open(p);

        Session s = new Session(snap, gui);
        sessions.put(id, s);

        Bukkit.getScheduler().runTask(plugin, () -> {
            clearPlayerInventory(p);
            p.getInventory().setItem(HOTBAR_MID, null);
        });
    }

    public boolean isOurTop(Player p, Inventory top) {
        Session s = sessions.get(p.getUniqueId());
        return s != null && s.top.equals(top);
    }

    public void handleClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        Session s = sessions.get(p.getUniqueId());
        if (s == null) return;

        int topSize = s.top.getSize();

        if (e.getRawSlot() < topSize) {
            s.gui.onTopClick(e);
            if (!e.isCancelled()) e.setCancelled(true);
        } else {
            if (e.getClickedInventory() instanceof PlayerInventory
                    && e.getSlot() == HOTBAR_MID
                    && (e.getClick().isLeftClick() || e.getClick().isRightClick())) {
                e.setCancelled(true);
                close(p);
                return;
            }
            if (e.isShiftClick()
                    || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                e.setCancelled(true);
            } else {
                s.gui.onBottomClick(e);
            }
        }
    }

    public void handleDrag(InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();
        Session s = sessions.get(p.getUniqueId());
        if (s == null) return;

        int topSize  = s.top.getSize();
        int centerRaw = topSize + 27 + HOTBAR_MID;
        for (int raw : e.getRawSlots()) {
            if (raw < topSize || raw == centerRaw) { e.setCancelled(true); return; }
        }
        s.gui.onDrag(e);
    }

    public void handleClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        Session s = sessions.get(p.getUniqueId());
        if (s == null) return;

        if (e.getView().getTopInventory().equals(s.top)) {
            s.gui.onClose(e);

            restorePlayerInventory(p, s.saved);

            sessions.remove(p.getUniqueId());
        }
    }

    public void close(Player p) {
        p.closeInventory();
    }

    public void handleQuit(Player p) {
        Session s = sessions.remove(p.getUniqueId());
        if (s != null) restorePlayerInventory(p, s.saved);
    }

    private void clearPlayerInventory(Player p) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        p.updateInventory();
    }

    private void restorePlayerInventory(Player p, SavedInv s) {
        PlayerInventory inv = p.getInventory();
        inv.setContents(s.contents);
        inv.setArmorContents(s.armor);
        inv.setItemInOffHand(s.offhand);
        p.updateInventory();
    }
}