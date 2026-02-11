package dev.kirill.kirevents.guis;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.events.EventType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LootConfigGUI implements Listener {
    
    private static final Map<UUID, MenuState> playerStates = new HashMap<>();
    
    public static void init(KirEvents plugin) {
        plugin.getServer().getPluginManager().registerEvents(new LootConfigGUI(), plugin);
    }
    
    public static void openMainMenu(KirEvents plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lНастройка Лута");
        
        ItemStack beacon = createItem(Material.BEACON, "§e§lМаяк", 
                Arrays.asList("§74 сундука", "§7Кликни для настройки"));
        inv.setItem(11, beacon);
        
        ItemStack airdrop = createItem(Material.CHEST, "§6§lАирдроп",
                Arrays.asList("§71 сундук", "§7Кликни для настройки"));
        inv.setItem(13, airdrop);
        
        ItemStack snake = createItem(Material.LIME_CONCRETE, "§a§lЗмея",
                Arrays.asList("§710 сундуков", "§7Кликни для настройки"));
        inv.setItem(15, snake);
        
        fillEmpty(inv);
        player.openInventory(inv);
        playerStates.put(player.getUniqueId(), new MenuState(MenuType.MAIN, null, 0));
    }
    
    public static void openEventMenu(KirEvents plugin, Player player, EventType eventType) {
        int maxChests = plugin.getLootConfigManager().getMaxChests(eventType);
        int rows = (int) Math.ceil(maxChests / 9.0) + 2;
        
        Inventory inv = Bukkit.createInventory(null, rows * 9, "§6" + eventType.getDisplayName() + " §7- Выбор Сундука");
        
        for (int i = 1; i <= maxChests; i++) {
            List<ItemStack> loot = plugin.getLootConfigManager().getLoot(eventType, i);
            ItemStack chest = createItem(Material.ENDER_CHEST, 
                    "§e§lСундук #" + i,
                    Arrays.asList(
                            "§7Предметов: §f" + loot.size(),
                            "",
                            "§aЛКМ §7- Настроить лут",
                            "§cПКМ §7- Очистить лут"
                    ));
            inv.setItem(i - 1, chest);
        }
        
        ItemStack back = createItem(Material.BARRIER, "§cНазад", Collections.singletonList("§7Вернуться в главное меню"));
        inv.setItem(rows * 9 - 1, back);
        
        fillEmpty(inv);
        player.openInventory(inv);
        playerStates.put(player.getUniqueId(), new MenuState(MenuType.EVENT, eventType, 0));
    }
    
    public static void openChestEditor(KirEvents plugin, Player player, EventType eventType, int chestNumber) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6Редактор §7- " + eventType.getDisplayName() + " #" + chestNumber);
        
        List<ItemStack> loot = plugin.getLootConfigManager().getLoot(eventType, chestNumber);
        for (int i = 0; i < loot.size() && i < 45; i++) {
            inv.setItem(i, loot.get(i).clone());
        }
        
        ItemStack save = createItem(Material.LIME_DYE, "§a§lСохранить",
                Collections.singletonList("§7Сохранить изменения"));
        inv.setItem(45, save);
        
        ItemStack clear = createItem(Material.RED_DYE, "§c§lОчистить",
                Collections.singletonList("§7Удалить весь лут"));
        inv.setItem(46, clear);
        
        ItemStack back = createItem(Material.BARRIER, "§cНазад",
                Collections.singletonList("§7Вернуться без сохранения"));
        inv.setItem(53, back);
        
        for (int i = 47; i < 53; i++) {
            inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList()));
        }
        
        player.openInventory(inv);
        playerStates.put(player.getUniqueId(), new MenuState(MenuType.CHEST_EDITOR, eventType, chestNumber));
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        UUID playerId = player.getUniqueId();
        MenuState state = playerStates.get(playerId);
        
        if (state == null) return;
        
        KirEvents plugin = KirEvents.getInstance();
        
        if (title.equals("§6§lНастройка Лута")) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            if (clicked.getType() == Material.BEACON) {
                openEventMenu(plugin, player, EventType.BEACON);
            } else if (clicked.getType() == Material.CHEST) {
                openEventMenu(plugin, player, EventType.AIRDROP);
            } else if (clicked.getType() == Material.LIME_CONCRETE) {
                openEventMenu(plugin, player, EventType.SNAKE);
            }
        } else if (title.contains("Выбор Сундука")) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            if (clicked.getType() == Material.BARRIER) {
                openMainMenu(plugin, player);
            } else if (clicked.getType() == Material.ENDER_CHEST) {
                int slot = event.getSlot();
                int chestNumber = slot + 1;
                
                if (event.isLeftClick()) {
                    openChestEditor(plugin, player, state.eventType, chestNumber);
                } else if (event.isRightClick()) {
                    plugin.getLootConfigManager().clearLoot(state.eventType, chestNumber);
                    player.sendMessage("§a§l✔ §aЛут сундука #" + chestNumber + " очищен!");
                    openEventMenu(plugin, player, state.eventType);
                }
            }
        } else if (title.contains("Редактор")) {
            int slot = event.getSlot();
            
            if (slot == 45) { // Save
                event.setCancelled(true);
                List<ItemStack> newLoot = new ArrayList<>();
                for (int i = 0; i < 45; i++) {
                    ItemStack item = event.getInventory().getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        newLoot.add(item.clone());
                    }
                }
                plugin.getLootConfigManager().setLoot(state.eventType, state.chestNumber, newLoot);
                player.sendMessage("§a§l✔ §aЛут сохранен!");
                openEventMenu(plugin, player, state.eventType);
            } else if (slot == 46) { // Clear
                event.setCancelled(true);
                for (int i = 0; i < 45; i++) {
                    event.getInventory().setItem(i, null);
                }
                player.sendMessage("§c§lОчищено! §7Не забудьте сохранить");
            } else if (slot == 53) { // Back
                event.setCancelled(true);
                openEventMenu(plugin, player, state.eventType);
            } else if (slot >= 47 && slot < 53) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (title.equals("§6§lНастройка Лута") || title.contains("Выбор Сундука") || title.contains("Редактор")) {
            Bukkit.getScheduler().runTaskLater(KirEvents.getInstance(), () -> {
                if (!player.getOpenInventory().getType().name().contains("CHEST")) {
                    playerStates.remove(player.getUniqueId());
                }
            }, 1L);
        }
    }
    
    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    private static void fillEmpty(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }
    
    private enum MenuType {
        MAIN, EVENT, CHEST_EDITOR
    }
    
    private static class MenuState {
        MenuType type;
        EventType eventType;
        int chestNumber;
        
        MenuState(MenuType type, EventType eventType, int chestNumber) {
            this.type = type;
            this.eventType = eventType;
            this.chestNumber = chestNumber;
        }
    }
}
