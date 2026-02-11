package dev.kirill.kirevents;

import dev.kirill.kirevents.commands.EventCommand;
import dev.kirill.kirevents.commands.EventLootCommand;
import dev.kirill.kirevents.commands.EventsMenuCommand;
import dev.kirill.kirevents.commands.VoteCommand;
import dev.kirill.kirevents.guis.LootConfigGUI;
import dev.kirill.kirevents.listeners.EventListener;
import dev.kirill.kirevents.listeners.RegionProtectionListener;
import dev.kirill.kirevents.managers.EventManager;
import dev.kirill.kirevents.managers.LootConfigManager;
import dev.kirill.kirevents.managers.SchematicManager;
import dev.kirill.kirevents.utils.HologramManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class KirEvents extends JavaPlugin {
    
    private static KirEvents instance;
    private EventManager eventManager;
    private EventListener eventListener;
    private RegionProtectionListener protectionListener;
    private SchematicManager schematicManager;
    private LootConfigManager lootConfigManager;
    private Economy economy;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        // Инициализация менеджеров
        schematicManager = new SchematicManager(this);
        lootConfigManager = new LootConfigManager(this);
        eventManager = new EventManager(this);
        eventListener = new EventListener(this);
        protectionListener = new RegionProtectionListener(this);
        
        // Регистрация команд
        getCommand("kirievent").setExecutor(new EventCommand(this));
        getCommand("events").setExecutor(new EventsMenuCommand(this));
        getCommand("eventloot").setExecutor(new EventLootCommand(this));
        getCommand("vote").setExecutor(new VoteCommand(this));
        
        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(eventListener, this);
        getServer().getPluginManager().registerEvents(protectionListener, this);
        
        // Инициализация GUI
        LootConfigGUI.init(this);
        
        // Подключение к Vault
        if (setupEconomy()) {
            getLogger().info("Vault подключен успешно!");
        } else {
            getLogger().warning("Vault не найден! Функция денег на маяке отключена.");
        }
        
        getLogger().info("╔════════════════════════════════╗");
        getLogger().info("║   KirEvents v3.0 ЗАПУЩЕН!     ║");
        getLogger().info("║   Автор: kirill                ║");
        getLogger().info("║   Новые фичи:                  ║");
        getLogger().info("║   • Система схематик           ║");
        getLogger().info("║   • Защита региона 75 блоков   ║");
        getLogger().info("║   • Деньги на маяке (Vault)    ║");
        getLogger().info("║   • Меню настройки лута        ║");
        getLogger().info("║   • Кулдаун 0.8 сек            ║");
        getLogger().info("╚════════════════════════════════╝");
    }
    
    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        HologramManager.removeAllHolograms();
        getLogger().info("KirEvents выключен!");
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
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
    
    public SchematicManager getSchematicManager() {
        return schematicManager;
    }
    
    public LootConfigManager getLootConfigManager() {
        return lootConfigManager;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public boolean hasEconomy() {
        return economy != null;
    }
}
