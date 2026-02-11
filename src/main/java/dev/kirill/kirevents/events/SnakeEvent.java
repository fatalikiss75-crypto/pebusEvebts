package dev.kirill.kirevents.events;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.utils.LootManager;
import org.bukkit.*;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeEvent extends EventStructure {
    
    private final Random random;
    
    public SnakeEvent(KirEvents plugin, Location location) {
        super(plugin, location, EventType.SNAKE);
        this.random = new Random();
    }
    
    @Override
    public void spawn() {
        if (!checkWorldGuard()) {
            findAlternativeLocation();
        }
        
        location.getWorld().playSound(location, Sound.BLOCK_END_PORTAL_SPAWN, 
                SoundCategory.MASTER, 10000f, 0.1f);
        
        announceSpawn();
        buildSnake();
        
        plugin.getEventManager().addStructure(this);
        scheduleDespawn();
    }
    
    private void buildSnake() {
        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        
        List<Location> snakePath = generateSnakePath(baseX, baseY, baseZ);
        
        // ТОЛСТАЯ змея 6x6
        for (int i = 0; i < snakePath.size(); i++) {
            Location point = snakePath.get(i);
            
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > 3.2) continue;
                    
                    Block block = world.getBlockAt(
                            point.getBlockX() + dx,
                            point.getBlockY(),
                            point.getBlockZ() + dz
                    );
                    
                    if ((i + dx + dz) % 3 == 0) {
                        block.setType(Material.GREEN_TERRACOTTA);
                    } else if ((i + dx + dz) % 3 == 1) {
                        block.setType(Material.GREEN_CONCRETE);
                    } else {
                        block.setType(Material.LIME_TERRACOTTA);
                    }
                    
                    addBlock(block);
                }
            }
        }
        
        // Сундуки с голограммами
        int chestCount = 10 + random.nextInt(11);
        int pathLength = snakePath.size();
        
        for (int i = 0; i < chestCount; i++) {
            int pathIndex = (pathLength / (chestCount + 1)) * (i + 1);
            if (pathIndex < pathLength) {
                Location chestLoc = snakePath.get(pathIndex).clone().add(0, 1, 0);
                Block chestBlock = chestLoc.getBlock();
                chestBlock.setType(Material.ENDER_CHEST);
                addChest(chestLoc);
                
                LootManager.LootRarity rarity = (random.nextInt(100) < 60) ? 
                        LootManager.LootRarity.COMMON : LootManager.LootRarity.EPIC;
                
                LootManager.fillChestWithLoot(chestLoc, rarity);
                
                plugin.getEventListener().registerChest(chestLoc, spawnTime);
                dev.kirill.kirevents.utils.HologramManager.createHologram(
                    plugin, 
                    chestLoc, 
                    plugin.getEventListener().getUnlockTime(chestLoc),
                    plugin.getEventListener().getExpireTime(chestLoc)
                );
            }
        }
        
        buildCloudHead(snakePath);
    }
    
    private void buildCloudHead(List<Location> snakePath) {
        World world = location.getWorld();
        Location headStart = snakePath.get(snakePath.size() - 1);
        
        for (int x = -6; x <= 6; x++) {
            for (int y = -4; y <= 8; y++) {
                for (int z = -6; z <= 6; z++) {
                    double distance = Math.sqrt(x*x/1.5 + y*y + z*z/1.5);
                    if (distance > 6.5) continue;
                    
                    if (random.nextInt(100) < 85) {
                        Location cloudLoc = headStart.clone().add(x, y, z);
                        Block cloudBlock = cloudLoc.getBlock();
                        cloudBlock.setType(Material.WHITE_WOOL);
                        addBlock(cloudBlock);
                    }
                }
            }
        }
    }
    
    private List<Location> generateSnakePath(int startX, int startY, int startZ) {
        List<Location> path = new ArrayList<>();
        World world = location.getWorld();
        
        double x = startX;
        double y = startY;
        double z = startZ;
        double angle = 0;
        double radius = 10;
        
        for (int i = 0; i <= 70; i++) {
            path.add(new Location(world, 
                    (int)(x + Math.cos(angle) * radius),
                    (int)(y + i),
                    (int)(z + Math.sin(angle) * radius)
            ));
            
            angle += 0.3;
            
            if (i % 10 == 0) {
                radius = 8 + random.nextInt(5);
            }
        }
        
        return path;
    }
    
    private boolean checkWorldGuard() {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
                return true;
            }
            
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));
            
            if (regionManager == null) {
                return true;
            }
            
            for (int x = -15; x <= 15; x++) {
                for (int z = -15; z <= 15; z++) {
                    Location checkLoc = location.clone().add(x, 0, z);
                    if (!regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(checkLoc)).getRegions().isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }
    
    private void findAlternativeLocation() {
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = location.getBlockX() + random.nextInt(1000) - 500;
            int z = location.getBlockZ() + random.nextInt(1000) - 500;
            int y = location.getWorld().getHighestBlockYAt(x, z) + 1;
            
            Location newLoc = new Location(location.getWorld(), x, y, z);
            
            location.setX(newLoc.getX());
            location.setY(newLoc.getY());
            location.setZ(newLoc.getZ());
            
            if (checkWorldGuard()) {
                return;
            }
        }
    }
    
    private void announceSpawn() {
        String prefix = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("§6§l✦ ПОЯВИЛСЯ ИВЕНТ: §a§lЗМЕЯ §6§l✦");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("  §7Координаты: §f" + location.getBlockX() + " " + 
                               location.getBlockY() + " " + location.getBlockZ());
        Bukkit.broadcastMessage("  §7Сундуки: §a§l60% ОБЫЧНЫЕ §7+ §5§l40% ЭПИЧЕСКИЕ");
        Bukkit.broadcastMessage("  §7Размер: §e6x6 блоков, высота 70 блоков");
        Bukkit.broadcastMessage("  §7Открытие через: §e§l5 минут");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("");
    }
}
