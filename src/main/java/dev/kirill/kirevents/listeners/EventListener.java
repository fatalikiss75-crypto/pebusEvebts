package dev.kirill.kirevents.listeners;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.events.EventStructure;
import dev.kirill.kirevents.utils.LootManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventListener implements Listener {
    
    private final KirEvents plugin;
    private final Map<UUID, Long> shellCooldowns;
    private final Map<Location, Long> chestUnlockTime;
    private final Map<Location, Long> chestExpireTime;
    
    public EventListener(KirEvents plugin) {
        this.plugin = plugin;
        this.shellCooldowns = new HashMap<>();
        this.chestUnlockTime = new HashMap<>();
        this.chestExpireTime = new HashMap<>();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (isEventBlock(block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c✖ Нельзя ломать блоки ивента!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        
        if (isNearEventStructure(loc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c✖ Нельзя строить рядом с ивентами!");
        }
    }
    
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST) return;
        
        Location chestLoc = event.getInventory().getLocation();
        if (chestLoc == null) return;
        
        if (!isEventBlock(chestLoc)) return;
        
        long now = System.currentTimeMillis();
        
        // Проверяем время разблокировки
        Long unlockTime = chestUnlockTime.get(chestLoc);
        if (unlockTime != null && now < unlockTime) {
            event.setCancelled(true);
            long timeLeft = (unlockTime - now) / 1000;
            long minutes = timeLeft / 60;
            long seconds = timeLeft % 60;
            player.sendMessage(String.format("§c🔒 Сундук откроется через §e%d:%02d", minutes, seconds));
            return;
        }
        
        // Проверяем время истечения
        Long expireTime = chestExpireTime.get(chestLoc);
        if (expireTime != null && now > expireTime) {
            event.setCancelled(true);
            player.sendMessage("§7⏱ Время на забор лута истекло!");
            
            // Удаляем сундук
            chestLoc.getBlock().setType(Material.AIR);
            LootManager.removeChestData(chestLoc);
            chestUnlockTime.remove(chestLoc);
            chestExpireTime.remove(chestLoc);
            
            checkAndRemoveEmptyStructure(chestLoc);
            return;
        }
        
        // Проверяем пустоту
        if (LootManager.isChestEmpty(chestLoc)) {
            event.setCancelled(true);
            player.sendMessage("§7Этот сундук уже пуст");
            
            chestLoc.getBlock().setType(Material.AIR);
            LootManager.removeChestData(chestLoc);
            chestUnlockTime.remove(chestLoc);
            chestExpireTime.remove(chestLoc);
            
            checkAndRemoveEmptyStructure(chestLoc);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.NAUTILUS_SHELL) return;
        
        Location chestLoc = event.getInventory().getLocation();
        if (chestLoc == null || !isEventBlock(chestLoc)) return;
        
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        if (shellCooldowns.containsKey(playerId)) {
            long lastPickup = shellCooldowns.get(playerId);
            long timeLeft = (lastPickup + 3000L) - now;
            
            if (timeLeft > 0) {
                event.setCancelled(true);
                player.sendMessage(String.format("§c⏱ Подожди §e%.1f §cсекунд!", timeLeft / 1000.0));
                return;
            }
        }
        
        int slot = event.getSlot();
        ItemStack realLoot = LootManager.getRealLoot(chestLoc, slot);
        
        if (realLoot != null) {
            event.setCancelled(true);
            
            event.getClickedInventory().setItem(slot, new ItemStack(Material.AIR));
            
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(realLoot);
            
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
                player.sendMessage("§e⚠ Инвентарь полон! Предметы выпали на землю");
            }
            
            shellCooldowns.put(playerId, now);
            LootManager.decrementItemCount(chestLoc);
            
            player.sendMessage("§a§l✔ §aПолучено: §f" + getItemName(realLoot));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            
            if (LootManager.isChestEmpty(chestLoc)) {
                player.closeInventory();
                chestLoc.getBlock().setType(Material.AIR);
                LootManager.removeChestData(chestLoc);
                chestUnlockTime.remove(chestLoc);
                chestExpireTime.remove(chestLoc);
                player.sendMessage("§7Сундук опустел и исчез");
                
                checkAndRemoveEmptyStructure(chestLoc);
            }
        }
    }
    
    public void registerChest(Location chestLoc, long spawnTime) {
        long unlockTime = spawnTime + (5 * 60 * 1000L); // +5 минут
        long expireTime = unlockTime + (5 * 60 * 1000L); // еще +5 минут
        
        chestUnlockTime.put(chestLoc, unlockTime);
        chestExpireTime.put(chestLoc, expireTime);
    }
    
    public long getUnlockTime(Location chestLoc) {
        return chestUnlockTime.getOrDefault(chestLoc, 0L);
    }
    
    public long getExpireTime(Location chestLoc) {
        return chestExpireTime.getOrDefault(chestLoc, 0L);
    }
    
    private boolean isEventBlock(Location loc) {
        EventStructure structure = plugin.getEventManager().getStructureAt(loc);
        return structure != null && structure.containsBlock(loc);
    }
    
    private boolean isNearEventStructure(Location loc) {
        return plugin.getEventManager().isNearAnyStructure(loc, 5);
    }
    
    private void checkAndRemoveEmptyStructure(Location chestLoc) {
        EventStructure structure = plugin.getEventManager().getStructureAt(chestLoc);
        if (structure != null && structure.areAllChestsEmpty()) {
            structure.despawn();
            plugin.getServer().broadcastMessage("§8[§6KirEvents§8]§r §7Ивент §e" + 
                    structure.getType().getDisplayName() + " §7завершился - все сундуки опустели!");
        }
    }
    
    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        
        String name = item.getType().name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim() + (item.getAmount() > 1 ? " x" + item.getAmount() : "");
    }
}
