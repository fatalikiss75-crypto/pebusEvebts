package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.managers.SchematicManager;
import dev.kirill.kirevents.utils.HologramManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BeaconEvent extends EventStructure {
    
    private BukkitRunnable moneyTask;
    
    public BeaconEvent(KirEvents plugin, Location location) {
        super(plugin, location, EventType.BEACON);
    }
    
    @Override
    public void spawn() {
        // Проверяем наличие схематики
        if (plugin.getSchematicManager().schematicExists("beacon")) {
            spawnFromSchematic();
        } else {
            buildDefaultBeacon();
        }
        
        // Звук
        location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 
                SoundCategory.MASTER, 10000f, 0.2f);
        
        // Анонс
        announceSpawn();
        
        // Регистрация
        plugin.getEventManager().addStructure(this);
        
        // Запуск системы денег
        startMoneySystem();
        
        // Удаление через время
        scheduleDespawn();
    }
    
    private void spawnFromSchematic() {
        SchematicManager.SchematicData data = plugin.getSchematicManager().loadSchematic("beacon");
        if (data != null) {
            List<Location> chests = plugin.getSchematicManager().pasteSchematic(data, location);
            
            int chestNum = 1;
            for (Location chestLoc : chests) {
                setupChest(chestLoc, chestNum);
                chestNum++;
            }
        }
    }
    
    private void buildDefaultBeacon() {
        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        
        for (int level = 0; level < 5; level++) {
            int size = 10 - level * 2;
            
            for (int x = -size/2; x <= size/2; x++) {
                for (int z = -size/2; z <= size/2; z++) {
                    Block block = world.getBlockAt(baseX + x, baseY + level, baseZ + z);
                    block.setType(Material.NETHERITE_BLOCK);
                    addBlock(block);
                }
            }
        }
        
        Block beaconBlock = world.getBlockAt(baseX, baseY + 5, baseZ);
        beaconBlock.setType(Material.BEACON);
        addBlock(beaconBlock);
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 6; y <= 20; y++) {
                    Block airBlock = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                    if (airBlock.getType() != Material.AIR) {
                        airBlock.setType(Material.AIR);
                    }
                }
            }
        }
        
        Location[] chestLocations = {
            new Location(world, baseX + 3, baseY + 5, baseZ),
            new Location(world, baseX - 3, baseY + 5, baseZ),
            new Location(world, baseX, baseY + 5, baseZ + 3),
            new Location(world, baseX, baseY + 5, baseZ - 3)
        };
        
        for (int i = 0; i < chestLocations.length; i++) {
            Location chestLoc = chestLocations[i];
            Block chestBlock = chestLoc.getBlock();
            chestBlock.setType(Material.ENDER_CHEST);
            addChest(chestLoc);
            setupChest(chestLoc, i + 1);
        }
    }
    
    private void setupChest(Location chestLoc, int chestNumber) {
        // Получаем лут из конфигурации
        List<ItemStack> configuredLoot = plugin.getLootConfigManager().getLoot(EventType.BEACON, chestNumber);
        
        Map<Integer, ItemStack> lootMap = new HashMap<>();
        Inventory inv = ((org.bukkit.block.Container) chestLoc.getBlock().getState()).getInventory();
        
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            slots.add(i);
        }
        Collections.shuffle(slots);
        
        int itemCount = configuredLoot.isEmpty() ? 30 : Math.min(configuredLoot.size(), 30);
        
        for (int i = 0; i < itemCount && i < slots.size(); i++) {
            int slot = slots.get(i);
            
            ItemStack loot;
            if (!configuredLoot.isEmpty() && i < configuredLoot.size()) {
                loot = configuredLoot.get(i).clone();
            } else {
                loot = generateDefaultLoot();
            }
            
            ItemStack shell = new ItemStack(Material.NAUTILUS_SHELL);
            ItemMeta meta = shell.getItemMeta();
            meta.setDisplayName("§a§lМистическая Ракушка");
            meta.setLore(Arrays.asList(
                    "§7Нажми, чтобы получить награду!",
                    "§8Что-то ценное внутри..."
            ));
            shell.setItemMeta(meta);
            
            inv.setItem(slot, shell);
            lootMap.put(slot, loot);
        }
        
        plugin.getEventListener().registerChest(chestLoc, spawnTime, lootMap);
        
        HologramManager.createHologram(
            plugin,
            chestLoc,
            plugin.getEventListener().getUnlockTime(chestLoc),
            plugin.getEventListener().getExpireTime(chestLoc),
            "ОБЫЧНЫЙ",
            "§a§l"
        );
    }
    
    private ItemStack generateDefaultLoot() {
        Random random = new Random();
        int roll = random.nextInt(100);
        
        if (roll < 30) {
            return new ItemStack(Material.DIAMOND, 5 + random.nextInt(16));
        } else if (roll < 50) {
            return new ItemStack(Material.EMERALD, 3 + random.nextInt(10));
        } else if (roll < 70) {
            return new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(4));
        } else {
            return new ItemStack(Material.NETHERITE_INGOT, 1);
        }
    }
    
    private void startMoneySystem() {
        if (!plugin.hasEconomy()) return;
        
        int moneyRadius = plugin.getConfig().getInt("beacon.money-radius", 15);
        double moneyAmount = plugin.getConfig().getDouble("beacon.money-amount", 1000);
        int moneyInterval = plugin.getConfig().getInt("beacon.money-interval", 60);
        
        moneyTask = new BukkitRunnable() {
            @Override
            public void run() {
                Collection<Player> nearbyPlayers = location.getWorld().getNearbyEntities(
                    location, moneyRadius, moneyRadius, moneyRadius,
                    entity -> entity instanceof Player
                ).stream()
                .map(entity -> (Player) entity)
                .toList();
                
                if (nearbyPlayers.isEmpty()) return;
                
                double amountPerPlayer = moneyAmount / nearbyPlayers.size();
                
                for (Player player : nearbyPlayers) {
                    plugin.getEconomy().depositPlayer(player, amountPerPlayer);
                    String msg = plugin.getConfig().getString("messages.money-received")
                        .replace("{amount}", String.format("%.2f", amountPerPlayer))
                        .replace("&", "§");
                    player.sendMessage(msg);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
        };
        
        moneyTask.runTaskTimer(plugin, moneyInterval * 20L, moneyInterval * 20L);
    }
    
    @Override
    public void despawn() {
        if (moneyTask != null) {
            moneyTask.cancel();
        }
        super.despawn();
    }
    
    private void announceSpawn() {
        String prefix = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("§6§l✦ ПОЯВИЛСЯ ИВЕНТ: §e§lМАЯК §6§l✦");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("  §7Координаты: §f" + location.getBlockX() + " " + 
                               location.getBlockY() + " " + location.getBlockZ());
        Bukkit.broadcastMessage("  §7Сундуки: §a§l4 ОБЫЧНЫХ");
        if (plugin.hasEconomy()) {
            Bukkit.broadcastMessage("  §7Деньги: §a§l+" + plugin.getConfig().getDouble("beacon.money-amount") + 
                    "₽ §7каждые " + plugin.getConfig().getInt("beacon.money-interval") + " сек");
        }
        Bukkit.broadcastMessage("  §7Открытие через: §e§l5 минут");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("");
    }
}
