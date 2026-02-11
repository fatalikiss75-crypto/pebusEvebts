package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.listeners.EventListener;
import dev.kirill.kirevents.utils.HologramManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public abstract class EventStructure {

    protected final KirEvents plugin;
    protected final Location location;
    protected final Set<Location> structureBlocks;
    protected final List<Location> chestLocations;
    protected final EventType type;
    protected final long spawnTime;

    public EventStructure(KirEvents plugin, Location location, EventType type) {
        this.plugin = plugin;
        this.location = location;
        this.type = type;
        this.structureBlocks = new HashSet<>();
        this.chestLocations = new ArrayList<>();
        this.spawnTime = System.currentTimeMillis();
    }

    public abstract void spawn();

    public void despawn() {
        // Плавная анимация распадания
        smoothDespawnAnimation();
    }

    private void smoothDespawnAnimation() {
        List<Location> blocksToRemove = new ArrayList<>(structureBlocks);
        
        new BukkitRunnable() {
            int step = 0;
            final int totalSteps = 20; // 20 шагов для плавного исчезновения
            
            @Override
            public void run() {
                if (step >= totalSteps || blocksToRemove.isEmpty()) {
                    // Удаляем оставшиеся блоки
                    for (Location loc : blocksToRemove) {
                        if (loc.getBlock().getType() != Material.AIR) {
                            loc.getBlock().setType(Material.AIR);
                        }
                    }
                    
                    // Удаляем голограммы
                    for (Location chestLoc : chestLocations) {
                        HologramManager.removeHologram(chestLoc);
                    }
                    
                    structureBlocks.clear();
                    chestLocations.clear();
                    plugin.getEventManager().removeStructure(EventStructure.this);
                    cancel();
                    return;
                }
                
                // Удаляем часть блоков на каждом шаге
                int blocksToRemoveThisStep = Math.max(1, blocksToRemove.size() / (totalSteps - step));
                for (int i = 0; i < blocksToRemoveThisStep && !blocksToRemove.isEmpty(); i++) {
                    int randomIndex = new Random().nextInt(blocksToRemove.size());
                    Location loc = blocksToRemove.get(randomIndex);
                    if (loc.getBlock().getType() != Material.AIR) {
                        loc.getBlock().setType(Material.AIR);
                        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);
                        loc.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, loc.clone().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.1);
                    }
                    blocksToRemove.remove(randomIndex);
                }
                
                step++;
            }
        }.runTaskTimer(plugin, 0L, 2L); // Каждые 2 тика
    }

    protected void scheduleDespawn() {
        int duration = plugin.getConfig().getInt("timings." + type.name().toLowerCase() + ".duration", 30);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Удаляем структуру в любом случае после окончания времени
                despawn();
                plugin.getServer().broadcastMessage("§8[§6KirEvents§8]§r §7Ивент §e" +
                        type.getDisplayName() + " §7завершился!");
            }
        }.runTaskLater(plugin, duration * 60 * 20L);
    }

    protected void addBlock(Block block) {
        structureBlocks.add(block.getLocation());
    }

    protected void addChest(Location location) {
        chestLocations.add(location);
        structureBlocks.add(location);
    }

    public boolean containsBlock(Location loc) {
        return structureBlocks.contains(loc);
    }

    public boolean containsChest(Location loc) {
        return chestLocations.contains(loc);
    }

    public boolean isNear(Location loc, int radius) {
        return location.distance(loc) <= radius;
    }

    public EventType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public long getSpawnTime() {
        return spawnTime;
    }

    public List<Location> getChestLocations() {
        return new ArrayList<>(chestLocations);
    }
}