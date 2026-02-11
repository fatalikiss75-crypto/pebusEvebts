package dev.kirill.kirevents.listeners;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.events.EventStructure;
import dev.kirill.kirevents.utils.HologramManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventListener implements Listener {

    private final KirEvents plugin;
    private final Map<UUID, Long> playerCooldowns;
    private final Map<Location, Long> chestUnlockTime;
    private final Map<Location, Long> chestExpireTime;
    private final Map<Location, Map<Integer, ItemStack>> chestLoot;
    private final double lootCooldown;

    public EventListener(KirEvents plugin) {
        this.plugin = plugin;
        this.playerCooldowns = new HashMap<>();
        this.chestUnlockTime = new HashMap<>();
        this.chestExpireTime = new HashMap<>();
        this.chestLoot = new HashMap<>();
        this.lootCooldown = plugin.getConfig().getDouble("settings.loot-cooldown", 0.8) * 1000;
    }

    // КРИТИЧЕСКИ ВАЖНО: Блокировка взаимодействия с обычным сундуком до истечения таймера
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CHEST) return;

        Location chestLoc = event.getClickedBlock().getLocation();

        // Проверяем является ли это сундуком ивента
        if (!isEventChest(chestLoc)) return;

        // БЛОКИРУЕМ взаимодействие в любом случае
        event.setCancelled(true);

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        // Проверка времени разблокировки
        Long unlockTime = chestUnlockTime.get(chestLoc);
        if (unlockTime != null && now < unlockTime) {
            long timeLeft = (unlockTime - now) / 1000;
            long minutes = timeLeft / 60;
            long seconds = timeLeft % 60;
            String msg = getMessage("chest-locked").replace("{time}", minutes + ":" + String.format("%02d", seconds));
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            return;
        }

        // Проверка истечения времени
        Long expireTime = chestExpireTime.get(chestLoc);
        if (expireTime != null && now > expireTime) {
            player.sendMessage(getMessage("chest-expired"));
            return;
        }

        // Проверка наличия лута - ВАЖНО: не удаляем сундук если пустой!
        if (!hasLootRemaining(chestLoc)) {
            player.sendMessage(getMessage("chest-empty"));
            return;
        }

        // Если всё ОК - открываем инвентарь программно
        Block block = chestLoc.getBlock();
        if (block.getState() instanceof org.bukkit.block.Chest chest) {
            player.openInventory(chest.getInventory());
        }
    }

    // ДОПОЛНИТЕЛЬНАЯ блокировка на случай если игрок как-то обошел первую
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Проверяем обычные сундуки
        if (event.getInventory().getType() == InventoryType.CHEST) {
            Location chestLoc = event.getInventory().getLocation();

            // Если это не ивентовый сундук - разрешаем
            if (chestLoc == null || !isEventChest(chestLoc)) {
                return;
            }

            long now = System.currentTimeMillis();

            Long unlockTime = chestUnlockTime.get(chestLoc);
            if (unlockTime != null && now < unlockTime) {
                event.setCancelled(true);
                long timeLeft = (unlockTime - now) / 1000;
                long minutes = timeLeft / 60;
                long seconds = timeLeft % 60;
                String msg = getMessage("chest-locked").replace("{time}", minutes + ":" + String.format("%02d", seconds));
                player.sendMessage(msg);
                return;
            }

            Long expireTime = chestExpireTime.get(chestLoc);
            if (expireTime != null && now > expireTime) {
                event.setCancelled(true);
                player.sendMessage(getMessage("chest-expired"));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getInventory().getType() != InventoryType.CHEST) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.NAUTILUS_SHELL) return;

        Location chestLoc = event.getInventory().getLocation();
        if (chestLoc == null || !isEventChest(chestLoc)) return;

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (playerCooldowns.containsKey(playerId)) {
            long lastPickup = playerCooldowns.get(playerId);
            long timePassed = now - lastPickup;

            if (timePassed < lootCooldown) {
                event.setCancelled(true);
                double timeLeft = (lootCooldown - timePassed) / 1000.0;
                String msg = getMessage("loot-cooldown").replace("{time}", String.format("%.1f", timeLeft));
                player.sendMessage(msg);
                return;
            }
        }

        int slot = event.getSlot();
        ItemStack realLoot = getLoot(chestLoc, slot);

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

            playerCooldowns.put(playerId, now);
            removeLoot(chestLoc, slot);

            String itemName = getItemName(realLoot);
            String msg = getMessage("loot-received").replace("{item}", itemName);
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            // Проверяем если лут закончился
            if (!hasLootRemaining(chestLoc)) {
                // Закрываем инвентарь через тик
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.closeInventory();
                    removeChest(chestLoc);
                }, 5L);
            }
        }
    }

    public void registerChest(Location chestLoc, long spawnTime, Map<Integer, ItemStack> loot) {
        int unlockDelay = plugin.getConfig().getInt("loot.chest-unlock-delay", 300);
        long unlockTime = spawnTime + (unlockDelay * 1000L);
        long expireTime = unlockTime + (25 * 60 * 1000L); // 25 минут на лут

        chestUnlockTime.put(chestLoc, unlockTime);
        chestExpireTime.put(chestLoc, expireTime);
        chestLoot.put(chestLoc, new HashMap<>(loot));

        plugin.getLogger().info("Registered chest at " + chestLoc + " with " + loot.size() + " items, unlock: " + unlockTime + ", expire: " + expireTime);
    }

    public long getUnlockTime(Location chestLoc) {
        return chestUnlockTime.getOrDefault(chestLoc, 0L);
    }

    public long getExpireTime(Location chestLoc) {
        return chestExpireTime.getOrDefault(chestLoc, 0L);
    }

    private ItemStack getLoot(Location chestLoc, int slot) {
        Map<Integer, ItemStack> loot = chestLoot.get(chestLoc);
        return loot != null ? loot.get(slot) : null;
    }

    private void removeLoot(Location chestLoc, int slot) {
        Map<Integer, ItemStack> loot = chestLoot.get(chestLoc);
        if (loot != null) {
            loot.remove(slot);
        }
    }

    public boolean hasLootRemaining(Location chestLoc) {
        Map<Integer, ItemStack> loot = chestLoot.get(chestLoc);
        return loot != null && !loot.isEmpty();
    }

    private void removeChest(Location chestLoc) {
        chestLoc.getBlock().setType(Material.AIR);
        chestUnlockTime.remove(chestLoc);
        chestExpireTime.remove(chestLoc);
        chestLoot.remove(chestLoc);
        HologramManager.removeHologram(chestLoc);

        checkAndRemoveEmptyStructure(chestLoc);
    }

    private boolean isEventChest(Location loc) {
        EventStructure structure = plugin.getEventManager().getStructureAt(loc);
        return structure != null && structure.containsChest(loc);
    }

    private void checkAndRemoveEmptyStructure(Location chestLoc) {
        EventStructure structure = plugin.getEventManager().getStructureAt(chestLoc);
        if (structure != null) {
            // Проверяем все сундуки структуры
            boolean allEmpty = true;
            for (Location loc : structure.getChestLocations()) {
                if (hasLootRemaining(loc)) {
                    allEmpty = false;
                    break;
                }
            }

            if (allEmpty) {
                structure.despawn();
                String msg = "§8[§6KirEvents§8]§r §7Ивент §e" +
                        structure.getType().getDisplayName() + " §7завершился - все сундуки опустели!";
                plugin.getServer().broadcastMessage(msg);
            }
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

    private String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return (prefix + msg).replace("&", "§");
    }
}