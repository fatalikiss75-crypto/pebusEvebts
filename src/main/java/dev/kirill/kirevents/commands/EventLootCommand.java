package dev.kirill.kirevents.commands;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.guis.LootConfigGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventLootCommand implements CommandExecutor {
    
    private final KirEvents plugin;
    
    public EventLootCommand(KirEvents plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getMessage("only-players"));
            return true;
        }
        
        if (!player.hasPermission("kirevents.loot")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        LootConfigGUI.openMainMenu(plugin, player);
        return true;
    }
    
    private String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return (prefix + msg).replace("&", "§");
    }
}
