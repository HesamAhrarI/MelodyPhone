package org.sayandev.MelodyPhone.manager;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ContactManager implements Listener {

    private final JavaPlugin plugin;
    private final LanguagesManager lang;

    private final Set<UUID> awaitingName = ConcurrentHashMap.newKeySet();

    private static class CallSession {
        final UUID caller;
        final UUID callee;
        int timeoutTask = -1;
        boolean active = false;

        CallSession(UUID caller, UUID callee) {
            this.caller = caller;
            this.callee = callee;
        }
    }

    private final Map<UUID, CallSession> incomingByCallee = new HashMap<>();
    private final Map<UUID, CallSession> byAny = new HashMap<>();

    public ContactManager(JavaPlugin plugin, LanguagesManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void promptName(Player caller) {
        caller.closeInventory();
        awaitingName.add(caller.getUniqueId());
        caller.sendMessage(Component.text("Type the name of the person you want to call.")
                .color(NamedTextColor.AQUA));
        caller.sendMessage(Component.text("Type 'cancel' to abort.")
                .color(NamedTextColor.GRAY));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPaperChat(AsyncChatEvent e) {
        Player caller = e.getPlayer();
        if (!awaitingName.remove(caller.getUniqueId())) return;

        e.setCancelled(true);
        String msg = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();

        if (msg.equalsIgnoreCase("cancel")) {
            caller.sendMessage(Component.text("Call input canceled.").color(NamedTextColor.YELLOW));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> tryStartCall(caller, msg));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLegacyChat(AsyncPlayerChatEvent e) {
        Player caller = e.getPlayer();
        if (!awaitingName.remove(caller.getUniqueId())) return;

        e.setCancelled(true);
        String msg = e.getMessage().trim();

        if (msg.equalsIgnoreCase("cancel")) {
            caller.sendMessage(Component.text("Call input canceled.").color(NamedTextColor.YELLOW));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> tryStartCall(caller, msg));
    }

    private void tryStartCall(Player caller, String targetName) {
        Player callee = Bukkit.getPlayerExact(targetName);
        if (callee == null || !callee.isOnline()) {
            sendAB(caller, "Player is offline.", NamedTextColor.RED);
            return;
        }
        if (callee.getUniqueId().equals(caller.getUniqueId())) {
            sendAB(caller, "You cannot call yourself.", NamedTextColor.RED);
            return;
        }
        if (!hasPhoneAnywhere(callee)) {
            sendAB(caller, "Player doesn’t have a phone.", NamedTextColor.RED);
            return;
        }
        if (!isInVoice(caller) || !isInVoice(callee)) {
            sendAB(caller, "The player is not in voice.", NamedTextColor.RED);
            sendAB(callee, "You are not in voice.", NamedTextColor.RED);
            return;
        }

        CallSession session = new CallSession(caller.getUniqueId(), callee.getUniqueId());
        byAny.put(session.caller, session);
        byAny.put(session.callee, session);
        incomingByCallee.put(session.callee, session);

        sendAB(caller, "Dialing...", NamedTextColor.GREEN);
        sendAB(callee, caller.getName() + " is calling you. Hold your Phone and press Q to accept.", NamedTextColor.AQUA);

        session.timeoutTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            CallSession s = byAny.get(caller.getUniqueId());
            if (s != null && !s.active) endCall(s, "Call timed out.");
        }, 20L * 20); // 20 sec
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        CallSession incoming = incomingByCallee.get(p.getUniqueId());
        if (incoming == null || incoming.active) return;

        ItemStack dropped = e.getItemDrop().getItemStack();

        if (!isPhone(dropped)) return;

        e.setCancelled(true);

        Player caller = Bukkit.getPlayer(incoming.caller);
        Player callee = p;
        if (caller == null || !caller.isOnline()) {
            endCall(incoming, "Caller left.");
            return;
        }

        incoming.active = true;
        if (incoming.timeoutTask != -1) {
            Bukkit.getScheduler().cancelTask(incoming.timeoutTask);
            incoming.timeoutTask = -1;
        }

        sendAB(caller, "You are in a call with " + callee.getName() + ".", NamedTextColor.GREEN);
        sendAB(callee, "You are in a call with " + caller.getName() + ".", NamedTextColor.GREEN);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        awaitingName.remove(id);
        CallSession s = byAny.get(id);
        if (s != null) endCall(s, "Call ended.");
    }

    private void endCall(CallSession s, String msg) {
        Player a = Bukkit.getPlayer(s.caller);
        Player b = Bukkit.getPlayer(s.callee);
        if (s.timeoutTask != -1) Bukkit.getScheduler().cancelTask(s.timeoutTask);
        if (a != null) sendAB(a, msg, NamedTextColor.GRAY);
        if (b != null) sendAB(b, msg, NamedTextColor.GRAY);
        incomingByCallee.remove(s.callee);
        byAny.remove(s.caller);
        byAny.remove(s.callee);
    }

    private void sendAB(Player p, String text, NamedTextColor color) {
        if (p != null && p.isOnline()) p.sendActionBar(Component.text(text).color(color));
    }

    private boolean hasPhoneAnywhere(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null) continue;
            if (it.getType() == Material.BLAZE_ROD && it.hasItemMeta() && it.getItemMeta().hasDisplayName()) {
                String dn = net.md_5.bungee.api.ChatColor.stripColor(it.getItemMeta().getDisplayName());
                if (dn != null && dn.equalsIgnoreCase("Phone")) return true;
            }
        }
        return false;
    }

    private boolean isPhone(ItemStack it) {
        if (it == null) return false;
        if (it.getType() != Material.BLAZE_ROD) return false;
        if (!it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return false;
        String dn = net.md_5.bungee.api.ChatColor.stripColor(it.getItemMeta().getDisplayName());
        return dn != null && dn.equalsIgnoreCase("Phone");
    }

    // SOON
    private boolean isInVoice(Player p) {
        return true;
    }
}