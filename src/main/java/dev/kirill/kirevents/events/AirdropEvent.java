package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.managers.SchematicManager;
import dev.kirill.kirevents.utils.HologramManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AirdropEvent extends EventStructure {
    
    public AirdropEvent(KirEvents plugin, Location location) {
        super(plugin, location.clone(), EventType.AIRDROP);
        this.location.setY(location.getWorld().getMaxHeight() - 10);
    }
    
    @Override
    public void spawn() {
        location.getWorld().playSound(location, Sound.BLOCK_PORTAL_TRAVEL, 
                SoundCategory.MASTER, 10000f, 0.2f);
        
        announceSpawn();
        dropAirdrop();
    }
    
    private void dropAirdrop() {
        World world = location.getWorld();
        
        new BukkitRunnable() {
            int currentY = (int) location.getY();
            final int groundY = world.getHighestBlockYAt(location.getBlockX(), location.getBlockZ()) + 1;
            
            @Override
            public void run() {
                if (currentY <= groundY) {
                    Location landLocation = new Location(world, location.getBlockX(), groundY, location.getBlockZ());
                    landAirdrop(landLocation);
                    cancel();
                    return;
                }
                
                Location particleLoc = new Location(world, location.getBlockX(), currentY, location.getBlockZ());
                world.spawnParticle(Particle.CLOUD, particleLoc, 20, 0.5, 0.5, 0.5, 0.01);
                world.playSound(particleLoc, Sound.ENTITY_TNT_PRIMED, 0.3f, 1.0f);
                
                currentY -= 3;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    
    private void landAirdrop(Location landLoc) {
        World world = landLoc.getWorld();
        
        if (plugin.getSchematicManager().schematicExists("airdrop")) {
            spawnFromSchematic(landLoc);
        } else {
            buildDefaultAirdrop(landLoc);
        }
        
        spawnGuards(landLoc);
        
        world.spawnParticle(Particle.EXPLOSION, landLoc, 5, 1, 1, 1, 0);
        world.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 3f, 0.8f);
        
        plugin.getEventManager().addStructure(this);
        scheduleDespawn();
    }
    
    private void spawnFromSchematic(Location landLoc) {
        SchematicManager.SchematicData data = plugin.getSchematicManager().loadSchematic("airdrop");
        if (data != null) {
            List<Location> chests = plugin.getSchematicManager().pasteSchematic(data, landLoc);
            
            if (!chests.isEmpty()) {
                setupChest(chests.get(0), 1);
            }
        }
    }
    
    private void buildDefaultAirdrop(Location landLoc) {
        Block chestBlock = landLoc.getBlock();
        chestBlock.setType(Material.ENDER_CHEST);
        addChest(landLoc);
        setupChest(landLoc, 1);
    }
    
    private void setupChest(Location chestLoc, int chestNumber) {
        List<ItemStack> configuredLoot = plugin.getLootConfigManager().getLoot(EventType.AIRDROP, chestNumber);
        
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
            meta.setDisplayName("§5§l✦ Эпическая Ракушка ✦");
            meta.setLore(Arrays.asList(
                    "§7Нажми, чтобы получить награду!",
                    "§8Что-то §5очень ценное §8внутри..."
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
            "ЭПИЧЕСКИЙ",
            "§5§l"
        );
    }
    
    private ItemStack generateDefaultLoot() {
        Random random = new Random();
        int roll = random.nextInt(100);
        
        if (roll < 20) {
            return new ItemStack(Material.NETHERITE_INGOT, 1 + random.nextInt(3));
        } else if (roll < 40) {
            return new ItemStack(Material.DIAMOND, 10 + random.nextInt(21));
        } else if (roll < 60) {
            return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1 + random.nextInt(3));
        } else if (roll < 75) {
            return new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        } else {
            return new ItemStack(Material.NETHER_STAR, 1);
        }
    }
    
    private void spawnGuards(Location landLoc) {
        Location[] zombieLocations = {
            landLoc.clone().add(3, 0, 0),
            landLoc.clone().add(-3, 0, 0),
            landLoc.clone().add(0, 0, 3),
            landLoc.clone().add(0, 0, -3)
        };
        
        for (Location zombieLoc : zombieLocations) {
            Zombie zombie = (Zombie) landLoc.getWorld().spawnEntity(zombieLoc, EntityType.ZOMBIE);
            zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
            zombie.setHealth(100.0);
            zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10.0);
            zombie.setCustomName("§c§lОхранник Аирдропа");
            zombie.setCustomNameVisible(true);
            zombie.setRemoveWhenFarAway(false);
        }
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
