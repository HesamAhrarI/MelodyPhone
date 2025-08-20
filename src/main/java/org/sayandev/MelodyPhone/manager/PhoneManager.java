package org.sayandev.MelodyPhone.manager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PhoneManager {

    public static ItemStack getPhone() {
        ItemStack phone = new ItemStack(Material.BLAZE_ROD, 1);
        ItemMeta meta = phone.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Phone");
            phone.setItemMeta(meta);
        }
        return phone;
    }
}