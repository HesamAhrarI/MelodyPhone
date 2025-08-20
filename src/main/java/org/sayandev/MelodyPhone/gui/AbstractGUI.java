package org.sayandev.MelodyPhone.gui;

import org.sayandev.MelodyPhone.manager.GuiManager;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractGUI implements BaseGUI {
    protected final JavaPlugin plugin;
    protected final GuiManager manager;
    protected Inventory inv;

    protected AbstractGUI(JavaPlugin plugin, GuiManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}