package org.sayandev.MelodyPhone.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.sayandev.MelodyPhone.Main;
import org.sayandev.MelodyPhone.gui.BaseGUI;
import org.sayandev.MelodyPhone.gui.SpotifyGUI;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpotifyManager implements Listener {

    public static final String DEFAULT_SOUND_ID = "minecraft:run-on";
    public static final int    DEFAULT_DURATION_SECONDS = 180;

    private final Main plugin;
    private final GuiManager guiManager;

    private final Map<UUID, String>  playing    = new HashMap<>();
    private final Map<UUID, Integer> clearTasks = new HashMap<>();

    private final Map<String, Integer> soundDurationsSeconds = new HashMap<>();

    public SpotifyManager(Main plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player p) {
        BaseGUI gui = new SpotifyGUI(plugin, guiManager, this);
        guiManager.open(p, gui);
    }

    public void playOrReject(Player p, String soundId) {
        if (isPlaying(p)) {
            plugin.getLanguages().send(p, "messages.spotify.already-playing");
            return;
        }
        startTrack(p, soundId);
        plugin.getLanguages().send(p, "messages.spotify.hint-stop");
    }

    public boolean isPlaying(Player p) {
        return playing.containsKey(p.getUniqueId());
    }

    @EventHandler
    public void onChatStop(AsyncPlayerChatEvent e) {
        String msg = e.getMessage();
        if (msg == null || !msg.trim().equalsIgnoreCase("stop")) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> stopTrack(p));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stopTrack(e.getPlayer());
    }

    private void startTrack(Player p, String soundId) {
        String id = (soundId == null || soundId.isEmpty()) ? DEFAULT_SOUND_ID : soundId;
        int seconds = soundDurationsSeconds.getOrDefault(id, DEFAULT_DURATION_SECONDS);
        int ticks = Math.max(1, seconds * 20);

        playing.put(p.getUniqueId(), id);
        Bukkit.getScheduler().runTask(plugin, () -> p.playSound(p.getLocation(), id, 1.0f, 1.0f));

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playing.remove(p.getUniqueId());
            clearTasks.remove(p.getUniqueId());
        }, ticks).getTaskId();

        clearTasks.put(p.getUniqueId(), taskId);
    }

    public void stopTrack(Player p) {
        UUID u = p.getUniqueId();
        String id = playing.remove(u);
        Integer tid = clearTasks.remove(u);

        if (tid != null) Bukkit.getScheduler().cancelTask(tid);
        if (id != null) {
            p.stopSound(id);
            plugin.getLanguages().send(p, "messages.spotify.stopped");
        }
    }

    private void loadConfig() {
        try {
            File appsDir = new File(plugin.getDataFolder(), "apps");
            if (!appsDir.exists()) appsDir.mkdirs();
            File f = new File(appsDir, "spotify.yml");
            if (!f.exists()) {
                YamlConfiguration tmp = new YamlConfiguration();
                tmp.set("tracks.minecraft:run-on.duration-seconds", DEFAULT_DURATION_SECONDS);
                tmp.save(f);
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            if (cfg.getConfigurationSection("tracks") != null) {
                for (String key : cfg.getConfigurationSection("tracks").getKeys(false)) {
                    int sec = cfg.getInt("tracks." + key + ".duration-seconds", DEFAULT_DURATION_SECONDS);
                    soundDurationsSeconds.put(key, sec);
                }
            } else {
                soundDurationsSeconds.put(DEFAULT_SOUND_ID, DEFAULT_DURATION_SECONDS);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Spotify config load failed: " + ex.getMessage());
            soundDurationsSeconds.put(DEFAULT_SOUND_ID, DEFAULT_DURATION_SECONDS);
        }
    }
}