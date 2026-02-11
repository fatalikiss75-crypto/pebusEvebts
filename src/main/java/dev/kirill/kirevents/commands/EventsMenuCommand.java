package dev.kirill.kirevents.commands;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.events.EventType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventsMenuCommand implements CommandExecutor, Listener {
    
    private final KirEvents plugin;
    private static final String MENU_TITLE = "§8§l⚡ События";
    
    public EventsMenuCommand(KirEvents plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков!");
            return true;
        }
        
        if (!player.hasPermission("kirevents.use")) {
            player.sendMessage("§cУ вас нет прав!");
            return true;
        }
        
        openEventsMenu(player);
        return true;
    }
    
    private void openEventsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);
        
        // Аирдроп
        ItemStack airdrop = createEventItem(
                Material.CHEST,
                "§6§lАирдроп",
                EventType.AIRDROP,
                Arrays.asList(
                        "§7Падает с неба",
                        "§7§lЭПИЧЕСКИЙ §7лут!",
                        "§74 зомби-охранника",
                        "§7Интервал: §e60 мин"
                )
        );
        inv.setItem(11, airdrop);
        
        // Маяк
        ItemStack beacon = createEventItem(
                Material.BEACON,
                "§e§lМаяк",
                EventType.BEACON,
                Arrays.asList(
                        "§7Незеритовая пирамида 10x10",
                        "§74 эндер-сундука",
                        "§7Редкость: §aОбычная §7и §5Эпическая",
                        "§7Интервал: §e60 мин"
                )
        );
        inv.setItem(13, beacon);
        
        // Змея
        ItemStack snake = createEventItem(
                Material.LIME_CONCRETE,
                "§a§lЗмея",
                EventType.SNAKE,
                Arrays.asList(
                        "§7Зеленая спираль 70 блоков",
                        "§710-20 эндер-сундуков",
                        "§7Сундук в пасти через §e15 мин",
                        "§7Интервал: §e180 мин"
                )
        );
        inv.setItem(15, snake);
        
        // Декор
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
        
        player.openInventory(inv);
    }
    
    private ItemStack createEventItem(Material material, String name, EventType type, List<String> description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        long nextTime = plugin.getEventManager().getNextEventTime(type);
        String timeString = getTimeString(nextTime);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(description);
        lore.add("");
        
        if (plugin.getEventManager().isRunning()) {
            if (nextTime > 0) {
                lore.add("§7Следующий: §f" + timeString);
            } else {
                lore.add("§aГотов к запуску!");
            }
        } else {
            lore.add("§cИвенты не запущены");
            lore.add("§7/kirillevent start");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private String getTimeString(long timestamp) {
        if (timestamp == 0) return "§7Неизвестно";
        
        long now = System.currentTimeMillis();
        if (timestamp <= now) return "§aСейчас!";
        
        long diff = timestamp - now;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        
        if (hours > 0) {
            long remainingMinutes = minutes - (hours * 60);
            return String.format("§e%dч %dмин", hours, remainingMinutes);
        } else {
            return String.format("§e%dмин", minutes);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MENU_TITLE)) return;
        event.setCancelled(true);
    }
}
