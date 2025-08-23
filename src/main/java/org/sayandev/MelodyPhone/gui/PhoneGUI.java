package org.sayandev.MelodyPhone.gui;

import org.bukkit.configuration.file.FileConfiguration;
import org.sayandev.MelodyPhone.Main;
import org.sayandev.MelodyPhone.manager.GuiManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class PhoneGUI extends AbstractGUI {

    public static final int DISCORD_SLOT = 12;
    public static final int GPS_SLOT     = 14;
    public static final int CONTACT_SLOT = 30;
    public static final int GROQ_SLOT    = 32;
    public static final int TAXI_SLOT    = 48;
    public static final int SPOTIFY_SLOT = 50;

    public PhoneGUI(JavaPlugin plugin, GuiManager manager) {
        super(plugin, manager);
    }

    @Override
    public void open(Player p) {
        inv = Bukkit.createInventory(null, 54, Component.empty());

        ItemStack discord = new ItemStack(Material.STONE_HOE);
        ItemMeta m0 = discord.getItemMeta();
        m0.displayName(Component.text("DISCORD").color(NamedTextColor.BLUE));
        m0.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        discord.setItemMeta(m0);
        inv.setItem(DISCORD_SLOT, discord);

        ItemStack gps = new ItemStack(Material.IRON_HOE);
        ItemMeta m1 = gps.getItemMeta();
        m1.displayName(Component.text("GPS").color(NamedTextColor.AQUA));
        m1.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        gps.setItemMeta(m1);
        inv.setItem(GPS_SLOT, gps);

        ItemStack contact = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta m2 = contact.getItemMeta();
        m2.displayName(Component.text("CONTACT").color(NamedTextColor.WHITE));
        m2.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        contact.setItemMeta(m2);
        inv.setItem(CONTACT_SLOT, contact);

        ItemStack groq = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta m3 = groq.getItemMeta();
        m3.displayName(Component.text("GROQ").color(NamedTextColor.LIGHT_PURPLE));
        m3.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        groq.setItemMeta(m3);
        inv.setItem(GROQ_SLOT, groq);

        ItemStack taxi = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta m4 = taxi.getItemMeta();
        m4.displayName(Component.text("TAXI").color(NamedTextColor.YELLOW));
        m4.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        taxi.setItemMeta(m4);
        inv.setItem(TAXI_SLOT, taxi);

        ItemStack spotify = new ItemStack(Material.NETHERITE_SHOVEL);
        ItemMeta m5 = spotify.getItemMeta();
        m5.displayName(Component.text("SPOTIFY").color(NamedTextColor.GREEN));
        m5.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        spotify.setItemMeta(m5);
        inv.setItem(SPOTIFY_SLOT, spotify);

        p.openInventory(inv);
    }

    @Override
    public void onTopClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == DISCORD_SLOT) {
            e.setCancelled(true);
            p.closeInventory();

            String url = "https://discord.gg/example";

            if (plugin instanceof Main) {
                FileConfiguration dc = ((Main) plugin).getDiscordCfg();
                url = dc.getString("discord.link", url);
            } else {
                url = plugin.getConfig().getString("discord.link",
                        plugin.getConfig().getString("general.discord.link", url));
            }

            p.sendMessage(
                    Component.text("Discord: ")
                            .append(Component.text(url)
                                    .color(NamedTextColor.AQUA)
                                    .decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.openUrl(url)))
            );
            return;
        }

        if (slot == GPS_SLOT) {
            e.setCancelled(true);
            p.closeInventory();

            if (plugin instanceof Main) {
                final Main m = (Main) plugin;
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override public void run() {
                        m.getGpsManager().openGPS(p);
                    }
                });
            }
            return;
        }

        if (slot == CONTACT_SLOT) {
            e.setCancelled(true);
            p.closeInventory();
            ((Main) plugin).getContactManager().open(p);
            return;
        }

        if (slot == GROQ_SLOT) {
            e.setCancelled(true);
            p.closeInventory();
            if (plugin instanceof Main) {
                ((Main) plugin).getGroqManager().startChat(p);
            }
            return;
        }

        if (slot == TAXI_SLOT) {
            e.setCancelled(true);
            p.closeInventory();
            if (p.hasPermission("sayanphone.taxi")) {
                ((Main) plugin).getTaxiManager().openDispatch(p);
            } else {
                ((Main) plugin).getTaxiManager().requestTaxi(p);
            }
            return;
        }
        if (slot == SPOTIFY_SLOT) {
            e.setCancelled(true);
            p.closeInventory();
            ((Main) plugin).getSpotifyManager().open(p);
            return;
        }
        e.setCancelled(true);
    }
}
