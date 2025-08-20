package org.sayandev.MelodyPhone.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LanguagesManager {

    private static final String[] BUNDLED = {"en_US", "es_ES", "pt_BR", "zh_CN"};

    private final JavaPlugin plugin;
    private final File langDir;
    private YamlConfiguration lang;
    private String currentCode = "en_US";

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public LanguagesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) langDir.mkdirs();

        for (String code : BUNDLED) {
            ensureDefault(code);
        }

        String desired = plugin.getConfig().getString("general.language", "en_US");
        load(desired);
    }

    public void reload() {
        for (String code : BUNDLED) {
            ensureDefault(code);
        }
        load(plugin.getConfig().getString("general.language", currentCode));
    }

    public void load(String code) {
        if (code == null || code.trim().isEmpty()) code = "en_US";
        currentCode = code;
        File f = new File(langDir, code + ".yml");
        if (!f.exists()) ensureDefault(code);
        this.lang = YamlConfiguration.loadConfiguration(f);
    }

    private void ensureDefault(String code) {
        File out = new File(langDir, code + ".yml");
        if (out.exists()) return;
        String path = "languages/" + code + ".yml";
        if (plugin.getResource(path) != null) {
            plugin.saveResource(path, false);
        }
    }

    private String raw(String key) {
        if (lang == null) return key;
        String s = lang.getString(key);
        return (s == null) ? key : s;
    }

    public Component prefix() {
        String p = raw("general.prefix");
        return parse(p);
    }

    public Component component(String key, Object... placeholders) {
        String s = raw(key);
        s = s.replace("{prefix}", raw("general.prefix"));
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String k = String.valueOf(placeholders[i]);
            String v = String.valueOf(placeholders[i + 1]);
            s = s.replace("{" + k + "}", v);
        }
        return parse(s);
    }

    private Component parse(String s) {
        if (s.indexOf('<') != -1 && s.indexOf('>') != -1) return mm.deserialize(s);
        return legacy.deserialize(s);
    }

    public void send(CommandSender to, String key, Object... placeholders) {
        to.sendMessage(component(key, placeholders));
    }

    public void sendPrefixed(CommandSender to, String key, Object... placeholders) {
        to.sendMessage(prefix().append(Component.space()).append(component(key, placeholders)));
    }

    public String getCurrentCode() { return currentCode; }
}