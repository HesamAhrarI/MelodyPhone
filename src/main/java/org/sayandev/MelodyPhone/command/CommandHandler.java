package org.sayandev.MelodyPhone.command;

import org.sayandev.MelodyPhone.Main;
import org.sayandev.MelodyPhone.manager.GPSManager;
import org.sayandev.MelodyPhone.manager.PhoneManager;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public CommandHandler(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguages().send(sender, "messages.errors.only-players");
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 1 && args[0].equalsIgnoreCase("version")) {
            plugin.getLanguages().send(sender, "messages.version",
                    "name", plugin.getDescription().getName(),
                    "version", plugin.getDescription().getVersion());
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sayanphone.commands.reload")) {
                plugin.getLanguages().send(sender, "messages.errors.no-permission");
                return true;
            }
            plugin.reloadAllConfigs();
            plugin.getLanguages().send(sender, "messages.reload.success");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("get")) {
            player.getInventory().addItem(PhoneManager.getPhone());
            plugin.getLanguages().send(player, "messages.success.received");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("gps")) {
            GPSManager gps = plugin.getGpsManager();

            if (args.length >= 3 && args[1].equalsIgnoreCase("addlocation")) {
                String name = args[2];
                boolean ok = gps.addLocation(player, name);
                if (ok) plugin.getLanguages().send(sender, "messages.gps.add.success", "name", name);
                else    plugin.getLanguages().send(sender, "messages.gps.add.fail");
                return true;
            }

            if (args.length >= 4 && args[1].equalsIgnoreCase("setname")) {
                String currentName = args[2];
                String newName = args[3];
                boolean ok = gps.setName(currentName, newName);
                if (ok) plugin.getLanguages().send(sender, "messages.gps.setname.success", "new", newName);
                else    plugin.getLanguages().send(sender, "messages.gps.setname.not-found", "current", currentName);
                return true;
            }

            if (args.length >= 4 && args[1].equalsIgnoreCase("seticon")) {
                String name = args[2];
                String itemToken = args[3];
                Material mat = Material.matchMaterial(itemToken);
                if (mat == null || !mat.isItem()) {
                    plugin.getLanguages().send(sender, "messages.gps.seticon.invalid", "item", itemToken);
                    return true;
                }
                boolean ok = gps.setIcon(name, mat);
                if (ok) plugin.getLanguages().send(sender, "messages.gps.seticon.success", "item", mat.name());
                else    plugin.getLanguages().send(sender, "messages.gps.seticon.not-found", "name", name);
                return true;
            }

            if (args.length >= 3 && args[1].equalsIgnoreCase("removelocation")) {
                String name = args[2];
                boolean ok = gps.removeLocation(name);
                if (ok) plugin.getLanguages().send(sender, "messages.gps.remove.success", "name", name);
                else    plugin.getLanguages().send(sender, "messages.gps.remove.not-found", "name", name);
                return true;
            }

            plugin.getLanguages().send(sender, "messages.usage.gps");
            return true;
        }

        plugin.getLanguages().send(player, "messages.usage.root");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("phone")) return Collections.emptyList();

        if (args.length == 1) {
            return prefix(Arrays.asList("version", "reload", "get", "gps"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("gps")) {
            return prefix(Arrays.asList("addlocation", "setname", "seticon", "removelocation"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("gps")) {
            if (args[1].equalsIgnoreCase("setname")
                    || args[1].equalsIgnoreCase("seticon")
                    || args[1].equalsIgnoreCase("removelocation")) {
                if (sender instanceof Player) {
                    return prefix(plugin.getGpsManager().getLocationNames(), args[2]);
                }
            }
            return Collections.emptyList();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("gps")) {
            if (args[1].equalsIgnoreCase("seticon")) {
                return prefix(
                        Arrays.stream(Material.values())
                                .filter(Material::isItem)
                                .map(m -> m.name().toLowerCase())
                                .collect(Collectors.toList()),
                        args[3] == null ? "" : args[3].toLowerCase()
                );
            }
        }

        return Collections.emptyList();
    }

    private List<String> prefix(List<String> options, String token) {
        String t = token == null ? "" : token.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(t))
                .sorted()
                .collect(Collectors.toList());
    }
}