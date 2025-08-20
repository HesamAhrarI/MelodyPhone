package org.sayandev.MelodyPhone.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.sayandev.MelodyPhone.manager.GuiManager;
import org.sayandev.MelodyPhone.manager.LanguagesManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GPSGUI extends AbstractGUI {

    private static final int[] LOC_SLOTS = {12, 14, 30, 32};
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;
    private static final int PAGE_SIZE = 4;

    private int currentPage = 0;
    private final List<LocationEntry> entries = new ArrayList<>();

    private final LanguagesManager languagesManager;

    public GPSGUI(JavaPlugin plugin, GuiManager manager, LanguagesManager languagesManager) {
        super(plugin, manager);
        this.languagesManager = languagesManager;
    }

    @Override
    public void open(Player p) {
        inv = Bukkit.createInventory(null, 54, Component.empty());
        reloadEntries();
        renderPage(p);
        p.openInventory(inv);
    }

    @Override
    public void onTopClick(InventoryClickEvent e) {
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == PREV_SLOT) {
            if (currentPage > 0) {
                currentPage--;
                renderPage(p);
            }
            return;
        }
        if (slot == NEXT_SLOT) {
            if ((currentPage + 1) * PAGE_SIZE < entries.size()) {
                currentPage++;
                renderPage(p);
            }
            return;
        }

        for (int i = 0; i < LOC_SLOTS.length; i++) {
            if (slot == LOC_SLOTS[i]) {
                int idx = currentPage * PAGE_SIZE + i;
                if (idx >= 0 && idx < entries.size()) {
                    p.closeInventory();
                    LocationEntry le = entries.get(idx);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!hasCompassAnywhere(p)) {
                            languagesManager.send(p, "messages.gps.mark.need-compass");
                            return;
                        }
                        Location dest = new Location(p.getWorld(), le.x, le.y, le.z);
                        p.setCompassTarget(dest);
                        languagesManager.send(p, "messages.gps.mark.set",
                                "name", le.name,
                                "x", Integer.toString(le.x),
                                "y", Integer.toString(le.y),
                                "z", Integer.toString(le.z));
                    });
                }
                return;
            }
        }
    }

    private boolean hasCompassAnywhere(Player p) {
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && is.getType() == Material.COMPASS) return true;
        }
        ItemStack off = p.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.COMPASS) return true;
        return false;
    }

    private void reloadEntries() {
        entries.clear();
        File f = new File(plugin.getDataFolder(), "apps/gps.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        List<Map<?, ?>> list = cfg.getMapList("gps.locations");
        if (list == null) return;

        for (Map<?, ?> raw : list) {
            String name = raw.get("name") != null ? String.valueOf(raw.get("name")) : "Unnamed";
            String iconStr = raw.get("icon") != null ? String.valueOf(raw.get("icon")) : "COMPASS";
            Material icon = Material.matchMaterial(iconStr);
            if (icon == null || !icon.isItem()) icon = Material.COMPASS;

            int x = 0, y = 0, z = 0;
            Object locObj = raw.get("location");
            if (locObj instanceof Map) {
                Map<?, ?> pos = (Map<?, ?>) locObj;
                x = toInt(pos.get("x"), 0);
                y = toInt(pos.get("y"), 0);
                z = toInt(pos.get("z"), 0);
            }

            entries.add(new LocationEntry(name, icon, x, y, z));
        }
    }

    private void renderPage(Player p) {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, null);

        int start = currentPage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= entries.size()) break;
            LocationEntry le = entries.get(idx);
            ItemStack it = new ItemStack(le.icon);
            ItemMeta m = it.getItemMeta();
            m.displayName(Component.text(le.name).color(NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("X: " + le.x + "  Y: " + le.y + "  Z: " + le.z).color(NamedTextColor.GRAY));
            m.lore(lore);
            m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(m);
            inv.setItem(LOC_SLOTS[i], it);
        }

        inv.setItem(PREV_SLOT, prevItem(currentPage > 0));
        boolean hasNext = (currentPage + 1) * PAGE_SIZE < entries.size();
        inv.setItem(NEXT_SLOT, nextItem(hasNext));

        p.updateInventory();
    }

    private ItemStack nextItem(boolean enabled) {
        ItemStack it = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text("Next").color(NamedTextColor.GREEN));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack prevItem(boolean enabled) {
        ItemStack it = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text("Prev").color(NamedTextColor.RED));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    private static class LocationEntry {
        final String name;
        final Material icon;
        final int x, y, z;
        LocationEntry(String name, Material icon, int x, int y, int z) {
            this.name = name;
            this.icon = icon;
            this.x = x; this.y = y; this.z = z;
        }
    }
}