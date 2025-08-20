package org.sayandev.MelodyPhone.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.sayandev.MelodyPhone.Main;
import org.sayandev.MelodyPhone.gui.BaseGUI;
import org.sayandev.MelodyPhone.gui.TaxiDispatchGUI;

import java.util.*;

public class TaxiManager implements Listener {

    private final Main plugin;
    private final GuiManager guiManager;

    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();

    public TaxiManager(Main plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void requestTaxi(Player requester) {
        UUID id = requester.getUniqueId();
        queue.remove(id);
        queue.add(id);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("sayanphone.taxi")) {
                plugin.getLanguages().send(p, "messages.taxi.driver-notify",
                        "player", requester.getName());
            }
        }
        plugin.getLanguages().send(requester, "messages.taxi.request-sent");
    }

    public void openDispatch(Player driver) {
        if (!driver.hasPermission("sayanphone.taxi")) {
            plugin.getLanguages().send(driver, "messages.errors.no-permission");
            return;
        }
        BaseGUI gui = new TaxiDispatchGUI(plugin, guiManager, this);
        guiManager.open(driver, gui);
    }

    public void accept(Player driver, Player target) {
        queue.remove(target.getUniqueId());
        plugin.getLanguages().send(driver, "messages.taxi.accepted.driver",
                "player", target.getName());
        plugin.getLanguages().send(target, "messages.taxi.accepted.target",
                "driver", driver.getName());
    }

    public List<Player> onlineRequestsSnapshot() {
        List<Player> out = new ArrayList<>();
        for (UUID id : queue) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) out.add(p);
        }
        return out;
    }


    public void cancelRequest(UUID requesterId) {
        queue.remove(requesterId);
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        cancelRequest(e.getPlayer().getUniqueId());
    }
}