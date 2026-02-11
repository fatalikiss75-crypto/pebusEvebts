package dev.kirill.kirevents.commands;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.events.EventType;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {
    
    private final KirEvents plugin;
    
    public EventCommand(KirEvents plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kirevents.admin")) {
            sender.sendMessage("§c✖ У вас нет прав!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "beacon":
            case "маяк":
                plugin.getEventManager().spawnEvent(EventType.BEACON);
                sender.sendMessage("§a§l✔ Маяк заспавнен!");
                break;
            case "airdrop":
            case "аирдроп":
                plugin.getEventManager().spawnEvent(EventType.AIRDROP);
                sender.sendMessage("§a§l✔ Аирдроп заспавнен!");
                break;
            case "snake":
            case "змея":
                plugin.getEventManager().spawnEvent(EventType.SNAKE);
                sender.sendMessage("§a§l✔ Змея заспавнена!");
                break;
            case "center":
                if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cТолько для игроков!");
                        return true;
                    }
                    Location loc = player.getLocation();
                    if (args.length == 5) {
                        try {
                            int x = Integer.parseInt(args[2]);
                            int y = Integer.parseInt(args[3]);
                            int z = Integer.parseInt(args[4]);
                            loc = new Location(player.getWorld(), x, y, z);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cНеверные координаты!");
                            return true;
                        }
                    }
                    plugin.getEventManager().setCenter(loc);
                    sender.sendMessage("§a§l✔ Центр установлен: " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
                }
                break;
            case "spawnloc":
                if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                    try {
                        int radius = Integer.parseInt(args[2]);
                        plugin.getEventManager().setSpawnRadius(radius);
                        sender.sendMessage("§a§l✔ Радиус: " + radius + " блоков");
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cНеверное число!");
                    }
                }
                break;
            case "start":
                if (plugin.getEventManager().isRunning()) {
                    sender.sendMessage("§cИвенты уже запущены!");
                } else {
                    plugin.getEventManager().startEvents();
                    sender.sendMessage("§a§l✔ Ивенты запущены!");
                }
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                    ");
        sender.sendMessage("§6§lKirEvents v2");
        sender.sendMessage("§e/kirillevent beacon");
        sender.sendMessage("§e/kirillevent airdrop");
        sender.sendMessage("§e/kirillevent snake");
        sender.sendMessage("§e/kirillevent center set [x y z]");
        sender.sendMessage("§e/kirillevent spawnloc set <radius>");
        sender.sendMessage("§e/kirillevent start");
        sender.sendMessage("§8§m                    ");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("beacon", "airdrop", "snake", "center", "spawnloc", "start");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("center") || args[0].equalsIgnoreCase("spawnloc")) {
                return List.of("set");
            }
        }
        return new ArrayList<>();
    }
}
