package org.sayandev.MelodyPhone.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.sayandev.MelodyPhone.gui.GPSGUI;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GPSManager {

    private final JavaPlugin plugin;
    private final GuiManager guiManager;
    private final LanguagesManager languagesManager;

    public GPSManager(JavaPlugin plugin, GuiManager guiManager, LanguagesManager languagesManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.languagesManager = languagesManager;
    }
    public void openGPS(Player player) {
        guiManager.open(player, new GPSGUI(plugin, guiManager, languagesManager));
    }

    private File getGpsFile() {
        return new File(plugin.getDataFolder(), "apps/gps.yml");
    }

    private YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(getGpsFile());
    }

    private boolean save(YamlConfiguration cfg) {
        try {
            cfg.save(getGpsFile());
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save apps/gps.yml: " + e.getMessage());
            return false;
        }
    }

    public boolean addLocation(Player p, String name) {
        if (name == null || name.trim().isEmpty()) return false;

        YamlConfiguration cfg = load();
        List<Map<?, ?>> list = cfg.getMapList("gps.locations");
        if (list == null) list = new ArrayList<>();

        Location loc = p.getLocation();
        Map<String, Object> pos = new LinkedHashMap<>();
        pos.put("x", loc.getBlockX());
        pos.put("y", loc.getBlockY());
        pos.put("z", loc.getBlockZ());

        Map<?, ?> found = null;
        for (Map<?, ?> raw : list) {
            Object n = raw.get("name");
            if (n != null && String.valueOf(n).equalsIgnoreCase(name)) { found = raw; break; }
        }

        if (found != null) {
            ((Map) found).put("location", pos);
        } else {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("icon", "COMPASS");
            entry.put("location", pos);
            list.add(entry);
        }

        cfg.set("gps.locations", list);
        return save(cfg);
    }

    public boolean setName(String currentName, String newName) {
        if (currentName == null || newName == null || newName.trim().isEmpty()) return false;

        YamlConfiguration cfg = load();
        List<Map<?, ?>> list = cfg.getMapList("gps.locations");
        if (list == null) return false;

        for (Map<?, ?> raw : list) {
            Object n = raw.get("name");
            if (n != null && String.valueOf(n).equalsIgnoreCase(currentName)) {
                ((Map) raw).put("name", newName);
                cfg.set("gps.locations", list);
                return save(cfg);
            }
        }
        return false;
    }

    public boolean setIcon(String name, Material icon) {
        if (name == null || icon == null) return false;

        YamlConfiguration cfg = load();
        List<Map<?, ?>> list = cfg.getMapList("gps.locations");
        if (list == null) return false;

        for (Map<?, ?> raw : list) {
            Object n = raw.get("name");
            if (n != null && String.valueOf(n).equalsIgnoreCase(name)) {
                ((Map) raw).put("icon", icon.name());
                cfg.set("gps.locations", list);
                return save(cfg);
            }
        }
        return false;
    }

    public boolean removeLocation(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "apps/gps.yml"));
        java.util.List<java.util.Map<?, ?>> list = cfg.getMapList("gps.locations");
        if (list == null || list.isEmpty()) return false;

        boolean removed = false;
        java.util.Iterator<java.util.Map<?, ?>> it = list.iterator();
        while (it.hasNext()) {
            java.util.Map<?, ?> raw = it.next();
            Object n = raw.get("name");
            if (n != null && String.valueOf(n).equalsIgnoreCase(name)) {
                it.remove();
                removed = true;
                break;
            }
        }
        if (!removed) return false;
        cfg.set("gps.locations", list);
        try {
            cfg.save(new java.io.File(plugin.getDataFolder(), "apps/gps.yml"));
            return true;
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Failed to save apps/gps.yml: " + e.getMessage());
            return false;
        }
    }

    public List<String> getLocationNames() {
        YamlConfiguration cfg = load();
        List<Map<?, ?>> list = cfg.getMapList("gps.locations");
        List<String> out = new ArrayList<>();
        if (list != null) {
            for (Map<?, ?> raw : list) {
                Object n = raw.get("name");
                if (n != null) out.add(String.valueOf(n));
            }
        }
        return out;
    }
}