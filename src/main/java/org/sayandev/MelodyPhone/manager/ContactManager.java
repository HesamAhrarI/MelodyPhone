package org.sayandev.MelodyPhone.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.sayandev.MelodyPhone.Main;
import org.sayandev.MelodyPhone.gui.BaseGUI;
import org.sayandev.MelodyPhone.gui.ContactGUI;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ContactManager {

    private final Main plugin;
    private final GuiManager guiManager;

    private static final class Pending {
        final UUID callerId;
        int taskId;
        Pending(UUID callerId, int taskId) {
            this.callerId = callerId;
            this.taskId = taskId;
        }
    }

    private final Map<UUID, Pending> incoming = new ConcurrentHashMap<>();

    public ContactManager(Main plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    public void open(Player p) {
        BaseGUI gui = new ContactGUI(plugin, guiManager, this);
        guiManager.open(p, gui);
    }

    public void markIncoming(Player caller, Player callee) {
        UUID calleeId = callee.getUniqueId();

        Pending old = incoming.remove(calleeId);
        if (old != null) {
            Bukkit.getScheduler().cancelTask(old.taskId);
        }

        int tid = Bukkit.getScheduler().runTaskLater(plugin, () -> incoming.remove(calleeId), 20L * 20).getTaskId();
        incoming.put(calleeId, new Pending(caller.getUniqueId(), tid));
    }

    public UUID getIncomingCaller(UUID calleeId) {
        Pending p = incoming.get(calleeId);
        return p == null ? null : p.callerId;
    }

    public void clearIncoming(UUID calleeId) {
        Pending p = incoming.remove(calleeId);
        if (p != null) {
            Bukkit.getScheduler().cancelTask(p.taskId);
        }
    }
}
