package org.sayandev.MelodyPhone.manager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.sayandev.MelodyPhone.Main;

public class PhoneManager {

    private static final String DEFAULT_MATERIAL = "BLAZE_ROD";
    private static final String DISPLAY_NAME = ChatColor.AQUA + "Phone";

    public static ItemStack getPhone(Main plugin) {
        Material mat = resolveMaterial(plugin);
        ItemStack phone = new ItemStack(mat, 1);
        ItemMeta meta = phone.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(DISPLAY_NAME);
            phone.setItemMeta(meta);
        }
        return phone;
    }

    public static boolean isPhone(Main plugin, ItemStack item) {
        if (item == null) return false;
        if (item.getType() != resolveMaterial(plugin)) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String dn = net.md_5.bungee.api.ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return dn != null && dn.equalsIgnoreCase("Phone");
    }

    public static Material resolveMaterial(Main plugin) {
        String conf = plugin.getConfig().getString("phone.material", DEFAULT_MATERIAL);
        Material m = Material.matchMaterial(conf);
        if (m == null || !m.isItem()) m = Material.matchMaterial(DEFAULT_MATERIAL);
        if (m == null) m = Material.BLAZE_ROD;
        return m;
    }
}
