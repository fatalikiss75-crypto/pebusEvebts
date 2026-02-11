package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {
    
    private final KirEvents plugin;
    private final Map<EventType, Long> nextEventTime;
    private final Map<EventType, BukkitTask> eventTasks;
    private final Set<EventStructure> activeStructures;
    private final Random random;
    private final VotingManager votingManager;
    
    private Location centerLocation;
    private int spawnRadius;
    private boolean running;
    
    public EventManager(KirEvents plugin) {
        this.plugin = plugin;
        this.nextEventTime = new HashMap<>();
        this.eventTasks = new HashMap<>();
        this.activeStructures = new HashSet<>();
        this.random = new Random();
        this.running = false;
        this.votingManager = new VotingManager(plugin);
        
        loadConfig();
    }
    
    private void loadConfig() {
        int x = plugin.getConfig().getInt("settings.center.x");
        int y = plugin.getConfig().getInt("settings.center.y");
        int z = plugin.getConfig().getInt("settings.center.z");
        this.centerLocation = new Location(Bukkit.getWorld("world"), x, y, z);
        this.spawnRadius = plugin.getConfig().getInt("settings.spawn-area.radius");
    }
    
    public void setCenter(Location location) {
        this.centerLocation = location;
        plugin.getConfig().set("settings.center.x", location.getBlockX());
        plugin.getConfig().set("settings.center.y", location.getBlockY());
        plugin.getConfig().set("settings.center.z", location.getBlockZ());
        plugin.saveConfig();
    }
    
    public void setSpawnRadius(int radius) {
        this.spawnRadius = radius;
        plugin.getConfig().set("settings.spawn-area.radius", radius);
        plugin.saveConfig();
    }
    
    public void startEvents() {
        if (running) return;
        running = true;
        
        // Запускаем первое голосование через 5 минут
        scheduleFirstVoting();
    }
    
    private void scheduleFirstVoting() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (running) {
                votingManager.startVoting();
            }
        }, 5 * 60 * 20L); // 5 минут
    }
    
    public VotingManager getVotingManager() {
        return votingManager;
    }
    
    private void scheduleEvent(EventType type, int delaySeconds) {
        announceEventStart(type, delaySeconds / 60);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            spawnEvent(type);
            scheduleNextEvent(type);
        }, delaySeconds * 20L);
    }
    
    private void scheduleNextEvent(EventType type) {
        int interval = getEventInterval(type);
        nextEventTime.put(type, System.currentTimeMillis() + (interval * 60 * 1000L));
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            spawnEvent(type);
            nextEventTime.put(type, System.currentTimeMillis() + (interval * 60 * 1000L));
        }, interval * 60 * 20L, interval * 60 * 20L);
        
        eventTasks.put(type, task);
    }
    
    public void spawnEvent(EventType type) {
        Location spawnLoc = getRandomLocation();
        
        EventStructure structure = switch (type) {
            case BEACON -> new BeaconEvent(plugin, spawnLoc);
            case AIRDROP -> new AirdropEvent(plugin, spawnLoc);
            case SNAKE -> new SnakeEvent(plugin, spawnLoc);
        };
        
        structure.spawn();
    }
    
    private Location getRandomLocation() {
        if (centerLocation == null) {
            centerLocation = new Location(Bukkit.getWorld("world"), 0, 100, 0);
        }
        
        int x = centerLocation.getBlockX() + random.nextInt(spawnRadius * 2) - spawnRadius;
        int z = centerLocation.getBlockZ() + random.nextInt(spawnRadius * 2) - spawnRadius;
        int y = centerLocation.getWorld().getHighestBlockYAt(x, z) + 1;
        
        return new Location(centerLocation.getWorld(), x, y, z);
    }
    
    private void announceEventStart(EventType type, int minutes) {
        String message = plugin.getConfig().getString("messages.event-starting")
                .replace("{event}", type.getDisplayName())
                .replace("{time}", String.valueOf(minutes))
                .replace("&", "§");
        
        String prefix = plugin.getConfig().getString("messages.prefix").replace("&", "§");
        Bukkit.broadcastMessage(prefix + message);
    }
    
    private int getEventInterval(EventType type) {
        return switch (type) {
            case BEACON -> plugin.getConfig().getInt("timings.beacon.interval", 60);
            case AIRDROP -> plugin.getConfig().getInt("timings.airdrop.interval", 60);
            case SNAKE -> plugin.getConfig().getInt("timings.snake.interval", 180);
        };
    }
    
    public void addStructure(EventStructure structure) {
        activeStructures.add(structure);
    }
    
    public void removeStructure(EventStructure structure) {
        activeStructures.remove(structure);
    }
    
    public EventStructure getStructureAt(Location loc) {
        for (EventStructure structure : activeStructures) {
            if (structure.containsBlock(loc)) {
                return structure;
            }
        }
        return null;
    }
    
    public boolean isNearAnyStructure(Location loc, int radius) {
        for (EventStructure structure : activeStructures) {
            if (structure.isNear(loc, radius)) {
                return true;
            }
        }
        return false;
    }
    
    public long getNextEventTime(EventType type) {
        return nextEventTime.getOrDefault(type, 0L);
    }
    
    public void shutdown() {
        running = false;
        eventTasks.values().forEach(BukkitTask::cancel);
        eventTasks.clear();
        
        // Удаляем все активные структуры
        new HashSet<>(activeStructures).forEach(EventStructure::despawn);
        activeStructures.clear();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void stopEvents() {
        if (!running) return;
        running = false;
        eventTasks.values().forEach(BukkitTask::cancel);
        eventTasks.clear();
        votingManager.stopVoting();
        
        Bukkit.broadcastMessage("§8[§6KirEvents§8] §c§lВсе ивенты остановлены администратором!");
    }
}
