package org.sayandev.MelodyPhone;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sayandev.MelodyPhone.command.CommandHandler;
import org.sayandev.MelodyPhone.listener.PhoneListener;
import org.sayandev.MelodyPhone.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {

    private static final String APPS_DIR = "apps";

    private FileConfiguration discordCfg, gpsCfg, contactCfg, groqCfg, spotifyCfg;
    private GPSManager gpsManager;
    private LanguagesManager languages;
    private ContactManager contactManager;
    private GroqManager groqManager;
    private TaxiManager taxiManager;
    private SpotifyManager spotifyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        discordCfg = ensureAndLoadInApps("discord.yml");
        gpsCfg     = ensureAndLoadInApps("gps.yml");
        contactCfg = ensureAndLoadInApps("contact.yml");
        groqCfg    = ensureAndLoadInApps("groq.yml");
        spotifyCfg = ensureAndLoadInApps("spotify.yml");

        languages = new LanguagesManager(this);

        GuiManager manager = new GuiManager(this);

        contactManager = new ContactManager(this, languages);

        gpsManager = new GPSManager(this, manager, languages);
        getServer().getPluginManager().registerEvents(new PhoneListener(this, manager), this);

        groqManager = new GroqManager(this);

        taxiManager = new TaxiManager(this, manager);

        spotifyManager = new SpotifyManager(this, manager);

        CommandHandler cmd = new CommandHandler(this);
        getCommand("phone").setExecutor(cmd);
        getCommand("phone").setTabCompleter(cmd);

        getLogger().info("SayanPhone has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SayanPhone has been disabled!");
    }

    private FileConfiguration ensureAndLoadInApps(String name) {
        File appDir = new File(getDataFolder(), APPS_DIR);
        if (!appDir.exists()) appDir.mkdirs();
        File out = new File(appDir, name);
        if (!out.exists()) saveResource(APPS_DIR + "/" + name, false);
        return YamlConfiguration.loadConfiguration(out);
    }

    public FileConfiguration getDiscordCfg() { return discordCfg; }
    public FileConfiguration getGpsCfg()     { return gpsCfg; }
    public FileConfiguration getContactCfg() { return contactCfg; }
    public FileConfiguration getGroqCfg() { return groqCfg; }
    public FileConfiguration getSpotifyCfg() { return spotifyCfg; }

    public void reloadAllConfigs() {
        reloadConfig();
        discordCfg = ensureAndLoadInApps("discord.yml");
        gpsCfg     = ensureAndLoadInApps("gps.yml");
        contactCfg = ensureAndLoadInApps("contact.yml");
        groqCfg    = ensureAndLoadInApps("groq.yml");
        spotifyCfg = ensureAndLoadInApps("spotify.yml");
        if (languages != null) languages.reload();
    }

    public GPSManager getGpsManager() { return gpsManager; }
    public LanguagesManager getLanguages() { return languages; }
    public ContactManager getContactManager() { return contactManager; }
    public GroqManager getGroqManager() { return groqManager; }
    public TaxiManager getTaxiManager() { return taxiManager; }
    public SpotifyManager getSpotifyManager() { return spotifyManager; }
}
