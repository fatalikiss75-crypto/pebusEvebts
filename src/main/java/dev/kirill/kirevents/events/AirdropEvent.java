package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.utils.LootManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;

public class AirdropEvent extends EventStructure {
    
    public AirdropEvent(KirEvents plugin, Location location) {
        super(plugin, location.clone(), EventType.AIRDROP);
        // Поднимаем локацию высоко для падения
        this.location.setY(location.getWorld().getMaxHeight() - 10);
    }
    
    @Override
    public void spawn() {
        // Воспроизводим звук
        location.getWorld().playSound(location, Sound.BLOCK_PORTAL_TRAVEL, 
                SoundCategory.MASTER, 10000f, 0.2f);
        
        // Анонсируем
        announceSpawn();
        
        // Падение аирдропа
        dropAirdrop();
    }
    
    private void dropAirdrop() {
        World world = location.getWorld();
        
        // Создаем эффект падения
        new BukkitRunnable() {
            int currentY = (int) location.getY();
            final int groundY = world.getHighestBlockYAt(location.getBlockX(), location.getBlockZ()) + 1;
            
            @Override
            public void run() {
                if (currentY <= groundY) {
                    // Приземление
                    Location landLocation = new Location(world, location.getBlockX(), groundY, location.getBlockZ());
                    landAirdrop(landLocation);
                    cancel();
                    return;
                }
                
                // Эффект падения
                Location particleLoc = new Location(world, location.getBlockX(), currentY, location.getBlockZ());
                world.spawnParticle(Particle.CLOUD, particleLoc, 20, 0.5, 0.5, 0.5, 0.01);
                world.playSound(particleLoc, Sound.ENTITY_TNT_PRIMED, 0.3f, 1.0f);
                
                currentY -= 3;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    
    private void landAirdrop(Location landLoc) {
        World world = landLoc.getWorld();
        
        // Ставим эндер-сундук
        Block chestBlock = landLoc.getBlock();
        chestBlock.setType(Material.ENDER_CHEST);
        addChest(landLoc);
        
        // Заполняем ЭПИЧЕСКИМ лутом (аирдроп всегда эпический)
        LootManager.fillChestWithLoot(landLoc, LootManager.LootRarity.EPIC);
        
        // Регистрируем сундук и создаем голограмму
        plugin.getEventListener().registerChest(landLoc, System.currentTimeMillis());
        dev.kirill.kirevents.utils.HologramManager.createHologram(
            plugin, 
            landLoc, 
            plugin.getEventListener().getUnlockTime(landLoc),
            plugin.getEventListener().getExpireTime(landLoc)
        );
        
        // Спавним 4 зомби вокруг
        Location[] zombieLocations = {
            landLoc.clone().add(3, 0, 0),
            landLoc.clone().add(-3, 0, 0),
            landLoc.clone().add(0, 0, 3),
            landLoc.clone().add(0, 0, -3)
        };
        
        for (Location zombieLoc : zombieLocations) {
            Zombie zombie = (Zombie) world.spawnEntity(zombieLoc, EntityType.ZOMBIE);
            zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
            zombie.setHealth(100.0);
            zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10.0);
            zombie.setCustomName("§c§lОхранник Аирдропа");
            zombie.setCustomNameVisible(true);
            zombie.setRemoveWhenFarAway(false);
        }
        
        // Эффекты приземления
        world.spawnParticle(Particle.EXPLOSION, landLoc, 5, 1, 1, 1, 0);
        world.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 3f, 0.8f);
        
        // Регистрируем в менеджере
        plugin.getEventManager().addStructure(this);
        
        // Удаляем через 30 минут
        scheduleDespawn();
    }
    
    private void announceSpawn() {
        String prefix = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        
        int groundY = location.getWorld().getHighestBlockYAt(location.getBlockX(), location.getBlockZ());
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("§6§l✦ ПОЯВИЛСЯ ИВЕНТ: §e§lАИРДРОП §6§l✦");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("  §7Координаты: §f" + location.getBlockX() + " " + 
                               groundY + " " + location.getBlockZ());
        Bukkit.broadcastMessage("  §7Редкость: §5§lЭПИЧЕСКИЙ");
        Bukkit.broadcastMessage("  §7Охрана: §c4 Зомби §7(100 HP)");
        Bukkit.broadcastMessage("  §7Открытие через: §e§l5 минут");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("");
    }
}
