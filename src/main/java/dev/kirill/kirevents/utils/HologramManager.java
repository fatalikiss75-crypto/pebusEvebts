package dev.kirill.kirevents.utils;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.managers.LootConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class HologramManager {
    
    private static final Map<Location, ArmorStand> holograms = new HashMap<>();
    private static final Map<Location, BukkitRunnable> hologramTasks = new HashMap<>();
    
    public static void createHologram(KirEvents plugin, Location chestLoc, long unlockTime, long expireTime, String rarityName, String rarityColor) {
        Location hologramLoc = chestLoc.clone().add(0.5, 2.5, 0.5);
        ArmorStand hologram = (ArmorStand) chestLoc.getWorld().spawnEntity(hologramLoc, EntityType.ARMOR_STAND);
        
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setMarker(true);
        hologram.setCustomNameVisible(true);
        hologram.setInvulnerable(true);
        
        holograms.put(chestLoc, hologram);
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (hologram.isDead() || !hologram.isValid()) {
                    cancel();
                    return;
                }
                
                long now = System.currentTimeMillis();
                
                if (now < unlockTime) {
                    // До открытия - исправленный расчет времени
                    long timeLeftMs = unlockTime - now;
                    long totalSeconds = timeLeftMs / 1000;
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    
                    hologram.setCustomName(String.format("§c🔒 §e%d:%02d\n%s%s", 
                            minutes, seconds, rarityColor, rarityName));
                } else if (now < expireTime) {
                    // Открыт - исправленный расчет оставшегося времени
                    long timeLeftMs = expireTime - now;
                    long totalSeconds = timeLeftMs / 1000;
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    
                    hologram.setCustomName(String.format("§a✔ ОТКРЫТ §7⏱ %d:%02d\n%s%s", 
                            minutes, seconds, rarityColor, rarityName));
                } else {
                    // Время истекло
                    hologram.setCustomName("§c✖ ВРЕМЯ ИСТЕКЛО");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        removeHologram(chestLoc);
                    }, 40L);
                    cancel();
                }
            }
        };
        
        task.runTaskTimer(plugin, 0L, 20L);
        hologramTasks.put(chestLoc, task);
    }
    
    public static void removeHologram(Location chestLoc) {
        ArmorStand hologram = holograms.remove(chestLoc);
        if (hologram != null && !hologram.isDead()) {
            hologram.remove();
        }
        
        BukkitRunnable task = hologramTasks.remove(chestLoc);
        if (task != null) {
            task.cancel();
        }
    }
    
    public static void removeAllHolograms() {
        holograms.values().forEach(hologram -> {
            if (!hologram.isDead()) {
                hologram.remove();
            }
        });
        holograms.clear();
        
        hologramTasks.values().forEach(BukkitRunnable::cancel);
        hologramTasks.clear();
    }
}
