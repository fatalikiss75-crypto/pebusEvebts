package dev.kirill.kirevents.managers;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.events.EventType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootConfigManager {
    
    private final KirEvents plugin;
    private final Map<String, List<ItemStack>> lootCache;
    
    public LootConfigManager(KirEvents plugin) {
        this.plugin = plugin;
        this.lootCache = new HashMap<>();
        loadAllLoot();
    }
    
    private void loadAllLoot() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection lootSection = config.getConfigurationSection("loot-config");
        
        if (lootSection == null) {
            return;
        }
        
        for (String eventType : lootSection.getKeys(false)) {
            ConfigurationSection eventSection = lootSection.getConfigurationSection(eventType);
            if (eventSection == null) continue;
            
            for (String chestKey : eventSection.getKeys(false)) {
                String fullKey = eventType + "." + chestKey;
                List<?> items = eventSection.getList(chestKey);
                
                if (items != null && !items.isEmpty()) {
                    List<ItemStack> itemStacks = new ArrayList<>();
                    for (Object obj : items) {
                        if (obj instanceof ItemStack item) {
                            itemStacks.add(item);
                        }
                    }
                    lootCache.put(fullKey, itemStacks);
                }
            }
        }
    }
    
    public List<ItemStack> getLoot(EventType eventType, int chestNumber) {
        String key = eventType.name().toLowerCase() + ".chest" + chestNumber;
        return lootCache.getOrDefault(key, new ArrayList<>());
    }
    
    public void setLoot(EventType eventType, int chestNumber, List<ItemStack> items) {
        String key = eventType.name().toLowerCase() + ".chest" + chestNumber;
        lootCache.put(key, new ArrayList<>(items));
        saveLoot(eventType, chestNumber, items);
    }
    
    public void addItem(EventType eventType, int chestNumber, ItemStack item) {
        String key = eventType.name().toLowerCase() + ".chest" + chestNumber;
        List<ItemStack> current = lootCache.computeIfAbsent(key, k -> new ArrayList<>());
        current.add(item.clone());
        saveLoot(eventType, chestNumber, current);
    }
    
    public void removeItem(EventType eventType, int chestNumber, int slot) {
        String key = eventType.name().toLowerCase() + ".chest" + chestNumber;
        List<ItemStack> current = lootCache.get(key);
        if (current != null && slot >= 0 && slot < current.size()) {
            current.remove(slot);
            saveLoot(eventType, chestNumber, current);
        }
    }
    
    public void clearLoot(EventType eventType, int chestNumber) {
        String key = eventType.name().toLowerCase() + ".chest" + chestNumber;
        lootCache.put(key, new ArrayList<>());
        saveLoot(eventType, chestNumber, new ArrayList<>());
    }
    
    private void saveLoot(EventType eventType, int chestNumber, List<ItemStack> items) {
        String path = "loot-config." + eventType.name().toLowerCase() + ".chest" + chestNumber;
        plugin.getConfig().set(path, items);
        plugin.saveConfig();
    }
    
    public int getMaxChests(EventType eventType) {
        return switch (eventType) {
            case BEACON -> 4;
            case AIRDROP -> 1;
            case SNAKE -> 10;
        };
    }
    
    public void reload() {
        lootCache.clear();
        plugin.reloadConfig();
        loadAllLoot();
    }
}
