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
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "beacon", "маяк" -> {
                plugin.getEventManager().spawnEvent(EventType.BEACON);
                sender.sendMessage(getMessage("event-spawned").replace("{event}", "Маяк"));
            }
            case "airdrop", "аирдроп" -> {
                plugin.getEventManager().spawnEvent(EventType.AIRDROP);
                sender.sendMessage(getMessage("event-spawned").replace("{event}", "Аирдроп"));
            }
            case "snake", "змея" -> {
                plugin.getEventManager().spawnEvent(EventType.SNAKE);
                sender.sendMessage(getMessage("event-spawned").replace("{event}", "Змея"));
            }
            case "pos1" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(getMessage("only-players"));
                    return true;
                }
                plugin.getSchematicManager().setPos1(player, player.getLocation());
                sender.sendMessage(getMessage("position-set").replace("{pos}", "1"));
            }
            case "pos2" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(getMessage("only-players"));
                    return true;
                }
                plugin.getSchematicManager().setPos2(player, player.getLocation());
                sender.sendMessage(getMessage("position-set").replace("{pos}", "2"));
            }
            case "save" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(getMessage("only-players"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Использование: /kirievent save <название>");
                    return true;
                }
                String name = args[1];
                if (plugin.getSchematicManager().saveSchematic(player, name)) {
                    sender.sendMessage(getMessage("schematic-saved").replace("{name}", name));
                } else {
                    sender.sendMessage("§c✖ Ошибка при сохранении схематики! Установите pos1 и pos2.");
                }
            }
            case "load" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c✖ Использование: /kirievent load <название> <beacon|airdrop|snake>");
                    return true;
                }
                String schematicName = args[1];
                String eventTypeName = args[2].toLowerCase();
                
                EventType eventType = switch (eventTypeName) {
                    case "beacon", "маяк" -> EventType.BEACON;
                    case "airdrop", "аирдроп" -> EventType.AIRDROP;
                    case "snake", "змея" -> EventType.SNAKE;
                    default -> null;
                };
                
                if (eventType == null) {
                    sender.sendMessage("§c✖ Неверный тип события! Используйте: beacon, airdrop, snake");
                    return true;
                }
                
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(getMessage("only-players"));
                    return true;
                }
                
                if (plugin.getSchematicManager().schematicExists(schematicName)) {
                    plugin.getEventManager().spawnEvent(eventType, player.getLocation());
                    sender.sendMessage(getMessage("schematic-loaded").replace("{name}", schematicName));
                } else {
                    sender.sendMessage(getMessage("schematic-not-found").replace("{name}", schematicName));
                }
            }
            case "list" -> {
                List<String> schematics = plugin.getSchematicManager().getSchematics();
                sender.sendMessage("§6§l=== Схематики ===");
                if (schematics.isEmpty()) {
                    sender.sendMessage("§7Нет сохраненных схематик");
                } else {
                    for (String schematic : schematics) {
                        sender.sendMessage("§e• §f" + schematic);
                    }
                }
            }
            case "center" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(getMessage("only-players"));
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
                            sender.sendMessage("§c✖ Неверные координаты!");
                            return true;
                        }
                    }
                    plugin.getEventManager().setCenter(loc);
                    sender.sendMessage("§a§l✔ Центр установлен: " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
                }
            }
            case "spawnloc" -> {
                if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                    try {
                        int radius = Integer.parseInt(args[2]);
                        plugin.getEventManager().setSpawnRadius(radius);
                        sender.sendMessage("§a§l✔ Радиус: " + radius + " блоков");
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§c✖ Неверное число!");
                    }
                }
            }
            case "start" -> {
                if (plugin.getEventManager().isRunning()) {
                    sender.sendMessage("§c✖ События уже запущены!");
                } else {
                    plugin.getEventManager().startEvents();
                    sender.sendMessage("§a§l✔ События запущены!");
                }
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getLootConfigManager().reload();
                sender.sendMessage("§a§l✔ Конфигурация перезагружена!");
            }
            default -> sendHelp(sender);
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("§6§lKirEvents v3.0");
        sender.sendMessage("§e/kirievent beacon §7- Заспавнить маяк");
        sender.sendMessage("§e/kirievent airdrop §7- Заспавнить аирдроп");
        sender.sendMessage("§e/kirievent snake §7- Заспавнить змею");
        sender.sendMessage("");
        sender.sendMessage("§6§lСхематики:");
        sender.sendMessage("§e/kirievent pos1 §7- Установить точку 1");
        sender.sendMessage("§e/kirievent pos2 §7- Установить точку 2");
        sender.sendMessage("§e/kirievent save <название> §7- Сохранить схематику");
        sender.sendMessage("§e/kirievent load <название> <тип> §7- Загрузить");
        sender.sendMessage("§e/kirievent list §7- Список схематик");
        sender.sendMessage("");
        sender.sendMessage("§6§lНастройки:");
        sender.sendMessage("§e/kirievent center set [x y z] §7- Центр спавна");
        sender.sendMessage("§e/kirievent spawnloc set <radius> §7- Радиус");
        sender.sendMessage("§e/kirievent start §7- Запустить события");
        sender.sendMessage("§e/kirievent reload §7- Перезагрузить конфиг");
        sender.sendMessage("§8§m                              ");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("beacon", "airdrop", "snake", "pos1", "pos2", "save", "load", "list", "center", "spawnloc", "start", "reload");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("center") || args[0].equalsIgnoreCase("spawnloc")) {
                return List.of("set");
            } else if (args[0].equalsIgnoreCase("load")) {
                return plugin.getSchematicManager().getSchematics();
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("load")) {
            return Arrays.asList("beacon", "airdrop", "snake");
        }
        return new ArrayList<>();
    }
    
    private String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return (prefix + msg).replace("&", "§");
    }
}
