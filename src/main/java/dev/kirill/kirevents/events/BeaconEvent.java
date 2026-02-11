package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.utils.LootManager;
import org.bukkit.*;
import org.bukkit.block.Block;

public class BeaconEvent extends EventStructure {
    
    public BeaconEvent(KirEvents plugin, Location location) {
        super(plugin, location, EventType.BEACON);
    }
    
    @Override
    public void spawn() {
        // Строим 10x10x10 незеритовую пирамиду
        buildBeaconStructure();
        
        // Воспроизводим звук
        location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 
                SoundCategory.MASTER, 10000f, 0.2f);
        
        // Анонсируем
        announceSpawn();
        
        // Регистрируем в менеджере
        plugin.getEventManager().addStructure(this);
        
        // Удаляем через 30 минут
        scheduleDespawn();
    }
    
    private void buildBeaconStructure() {
        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        
        // Строим пирамиду из незерита 5 уровней (база 10x10)
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
        
        // Ставим маяк на верх
        Block beaconBlock = world.getBlockAt(baseX, baseY + 5, baseZ);
        beaconBlock.setType(Material.BEACON);
        addBlock(beaconBlock);
        
        // Делаем дырку в потолке над маяком (3x3 дырка)
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
        
        // Размещаем 4 эндер-сундука вокруг маяка
        Location[] chestLocations = {
            new Location(world, baseX + 3, baseY + 5, baseZ),
            new Location(world, baseX - 3, baseY + 5, baseZ),
            new Location(world, baseX, baseY + 5, baseZ + 3),
            new Location(world, baseX, baseY + 5, baseZ - 3)
        };
        
        // Распределяем редкость: 2 обычных, 2 эпических
        for (int i = 0; i < chestLocations.length; i++) {
            Location chestLoc = chestLocations[i];
            Block chestBlock = chestLoc.getBlock();
            chestBlock.setType(Material.ENDER_CHEST);
            addChest(chestLoc);
            
            // Чередуем редкость
            LootManager.LootRarity rarity = (i % 2 == 0) ? 
                    LootManager.LootRarity.COMMON : LootManager.LootRarity.EPIC;
            
            LootManager.fillChestWithLoot(chestLoc, rarity);
            
            // Регистрируем сундук и создаем голограмму
            plugin.getEventListener().registerChest(chestLoc, spawnTime);
            dev.kirill.kirevents.utils.HologramManager.createHologram(
                plugin, 
                chestLoc, 
                plugin.getEventListener().getUnlockTime(chestLoc),
                plugin.getEventListener().getExpireTime(chestLoc)
            );
        }
    }
    
    private void announceSpawn() {
        String prefix = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("§6§l✦ ПОЯВИЛСЯ ИВЕНТ: §e§lМАЯК §6§l✦");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("  §7Координаты: §f" + location.getBlockX() + " " + 
                               location.getBlockY() + " " + location.getBlockZ());
        Bukkit.broadcastMessage("  §7Сундуки: §a§l2 ОБЫЧНЫХ §7+ §5§l2 ЭПИЧЕСКИХ");
        Bukkit.broadcastMessage("  §7Открытие через: §e§l5 минут");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("");
    }
}
