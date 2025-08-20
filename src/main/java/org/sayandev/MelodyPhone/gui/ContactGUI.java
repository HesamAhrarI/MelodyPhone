package org.sayandev.MelodyPhone.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.sayandev.MelodyPhone.manager.ContactManager;
import org.sayandev.MelodyPhone.manager.GuiManager;

import ir.taher7.melodymine.core.MelodyManager;
import ir.taher7.melodymine.models.MelodyPlayer;
import ir.taher7.melodymine.storage.Storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ContactGUI extends AbstractGUI {

    private static final int[] USER_SLOTS = {12, 14, 30, 32};
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;
    private static final int PAGE_SIZE = 4;

    private final ContactManager contactMgr;

    private Player viewer;
    private final List<Player> candidates = new ArrayList<>();
    private int currentPage = 0;

    private boolean incomingMode = false;
    private Player incomingCaller = null;

    public ContactGUI(JavaPlugin plugin, GuiManager manager, ContactManager contactMgr) {
        super(plugin, manager);
        this.contactMgr = contactMgr;
    }

    @Override
    public void open(Player p) {
        this.viewer = p;
        inv = Bukkit.createInventory(null, 54, Component.empty());

        UUID callerId = contactMgr.getIncomingCaller(p.getUniqueId());
        if (callerId != null) {
            Player caller = Bukkit.getPlayer(callerId);
            if (caller != null && caller.isOnline()) {
                incomingMode = true;
                incomingCaller = caller;
            } else {
                contactMgr.clearIncoming(p.getUniqueId());
            }
        }

        if (incomingMode) {
            renderIncoming();
        } else {
            reloadCandidates();
            renderPage();
        }
        p.openInventory(inv);
    }

    private void reloadCandidates() {
        candidates.clear();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(viewer)) continue;
            if (!online.isOnline()) continue;
            candidates.add(online);
        }
    }

    private void renderIncoming() {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, null);
        inv.setItem(USER_SLOTS[0], headOf(incomingCaller, incomingCaller.getName(), "Click to accept"));
        inv.setItem(PREV_SLOT, button(Material.RED_STAINED_GLASS_PANE, "Prev", NamedTextColor.RED));
        inv.setItem(NEXT_SLOT, button(Material.LIME_STAINED_GLASS_PANE, "Next", NamedTextColor.GREEN));
        viewer.updateInventory();
    }

    private void renderPage() {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, null);

        int start = currentPage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= candidates.size()) break;
            Player target = candidates.get(idx);
            inv.setItem(USER_SLOTS[i], headOf(target, target.getName(), "Click to call"));
        }

        inv.setItem(PREV_SLOT, button(Material.RED_STAINED_GLASS_PANE, "Prev", NamedTextColor.RED));
        inv.setItem(NEXT_SLOT, button(Material.LIME_STAINED_GLASS_PANE, "Next", NamedTextColor.GREEN));
        viewer.updateInventory();
    }

    @Override
    public void onTopClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == PREV_SLOT) {
            if (!incomingMode && currentPage > 0) { currentPage--; renderPage(); }
            return;
        }
        if (slot == NEXT_SLOT) {
            if (!incomingMode && (currentPage + 1) * PAGE_SIZE < candidates.size()) { currentPage++; renderPage(); }
            return;
        }

        if (incomingMode) {
            if (slot == USER_SLOTS[0] && incomingCaller != null) {
                manager.close(p);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MelodyPlayer mpCallee = Storage.INSTANCE.getOnlinePlayers().get(p.getName());
                    MelodyPlayer mpCaller = Storage.INSTANCE.getOnlinePlayers().get(incomingCaller.getName());
                    if (mpCallee == null || mpCaller == null) {
                        p.sendActionBar(Component.text("Call system not ready.").color(NamedTextColor.RED));
                        return;
                    }
                    try {
                        MelodyManager.INSTANCE.acceptCall(mpCallee, mpCaller);
                        p.sendActionBar(Component.text("Accepted call from " + incomingCaller.getName())
                                .color(NamedTextColor.GREEN));
                        contactMgr.clearIncoming(p.getUniqueId());
                    } catch (Throwable t) {
                        p.sendActionBar(Component.text("Failed to accept call.").color(NamedTextColor.RED));
                    }
                });
            }
            return;
        }

        for (int i = 0; i < USER_SLOTS.length; i++) {
            if (slot == USER_SLOTS[i]) {
                int absoluteIndex = currentPage * PAGE_SIZE + i;
                if (absoluteIndex < 0 || absoluteIndex >= candidates.size()) return;

                Player target = candidates.get(absoluteIndex);
                manager.close(p);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    MelodyPlayer mpCaller = Storage.INSTANCE.getOnlinePlayers().get(p.getName());
                    MelodyPlayer mpCallee = Storage.INSTANCE.getOnlinePlayers().get(target.getName());
                    if (mpCaller == null || mpCallee == null) {
                        p.sendActionBar(Component.text("Call system not ready.").color(NamedTextColor.RED));
                        return;
                    }
                    try {
                        MelodyManager.INSTANCE.startCall(mpCaller, mpCallee);
                        p.sendActionBar(Component.text("Dialing " + target.getName() + "...")
                                .color(NamedTextColor.GREEN));
                        target.sendActionBar(Component.text(p.getName() + " is calling you. Hold your Phone and press Q to accept.")
                                .color(NamedTextColor.AQUA));
                        
                        contactMgr.markIncoming(p, target);
                    } catch (Throwable t) {
                        p.sendActionBar(Component.text("Failed to start call.").color(NamedTextColor.RED));
                    }
                });
                return;
            }
        }
    }

    @Override public void onBottomClick(InventoryClickEvent e) { e.setCancelled(true); }
    @Override public void onDrag(InventoryDragEvent e) { e.setCancelled(true); }
    @Override public void onClose(InventoryCloseEvent e) { }

    private ItemStack headOf(OfflinePlayer player, String title, String loreLine) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(player);
        sm.displayName(Component.text(title).color(NamedTextColor.AQUA));
        sm.lore(Collections.singletonList(Component.text(loreLine).color(NamedTextColor.GRAY)));
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
