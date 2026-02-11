package dev.kirill.kirevents;

import dev.kirill.kirevents.commands.EventCommand;
import dev.kirill.kirevents.commands.EventsMenuCommand;
import dev.kirill.kirevents.events.EventManager;
import dev.kirill.kirevents.listeners.EventListener;
import org.bukkit.plugin.java.JavaPlugin;

public class KirEvents extends JavaPlugin {
    
    private static KirEvents instance;
    private EventManager eventManager;
    private EventListener eventListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        eventManager = new EventManager(this);
        eventListener = new EventListener(this);
        
        getCommand("kirillevent").setExecutor(new EventCommand(this));
        getCommand("events").setExecutor(new EventsMenuCommand(this));
        
        getServer().getPluginManager().registerEvents(eventListener, this);
        
        getLogger().info("KirEvents v2 успешно запущен!");
        getLogger().info("Автор: kirill");
        getLogger().info("Новые фичи: защита блоков, система редкости, зеленая змея!");
    }
    
    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        dev.kirill.kirevents.utils.HologramManager.removeAllHolograms();
        getLogger().info("KirEvents выключен!");
    }
    
    public static KirEvents getInstance() {
        return instance;
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
    
    public EventListener getEventListener() {
        return eventListener;
    }
}
