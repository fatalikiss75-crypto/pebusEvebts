package dev.kirill.kirevents.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LootManager {
    
    private static final Random random = new Random();
    private static final Map<Location, Map<Integer, ItemStack>> chestLoot = new HashMap<>();
    private static final Map<Location, Integer> chestItemCount = new HashMap<>();
    private static final Map<Location, LootRarity> chestRarity = new HashMap<>();
    
    public enum LootRarity {
        COMMON,  // Обычная - алмазные вещи
        EPIC     // Эпическая - незеритовые и редкие
    }
    
    public static void fillChestWithLoot(Location chestLocation, LootRarity rarity) {
        Block block = chestLocation.getBlock();
        if (!(block.getState() instanceof Container container)) {
            return;
        }
        
        Inventory inv = container.getInventory();
        Map<Integer, ItemStack> lootMap = new HashMap<>();
        
        // Всегда 30 предметов
        int itemCount = 30;
        chestItemCount.put(chestLocation, itemCount);
        chestRarity.put(chestLocation, rarity);
        
        // Генерируем случайные слоты для лута (54 слота)
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            slots.add(i);
        }
        Collections.shuffle(slots);
        
        // Размещаем 30 ракушек
        for (int i = 0; i < itemCount && i < slots.size(); i++) {
            int slot = slots.get(i);
            ItemStack loot = generateLootItem(rarity);
            
            // Создаем ракушку
            ItemStack shell = new ItemStack(Material.NAUTILUS_SHELL);
            ItemMeta meta = shell.getItemMeta();
            
            if (rarity == LootRarity.EPIC) {
                meta.setDisplayName("§5§l✦ Эпическая Ракушка ✦");
                meta.setLore(Arrays.asList(
                        "§7Нажми, чтобы получить награду!",
                        "§8Что-то §5очень ценное §8внутри..."
                ));
            } else {
                meta.setDisplayName("§a§lМистическая Ракушка");
                meta.setLore(Arrays.asList(
                        "§7Нажми, чтобы получить награду!",
                        "§8Что-то ценное внутри..."
                ));
            }
            
            shell.setItemMeta(meta);
            
            inv.setItem(slot, shell);
            lootMap.put(slot, loot);
        }
        
        chestLoot.put(chestLocation, lootMap);
    }
    
    private static ItemStack generateLootItem(LootRarity rarity) {
        if (rarity == LootRarity.EPIC) {
            return generateEpicLoot();
        } else {
            return generateCommonLoot();
        }
    }
    
    private static ItemStack generateCommonLoot() {
        int roll = random.nextInt(100);
        
        if (roll < 25) { // 25% - Зачарованная алмазная броня
            return createEnchantedDiamondArmor();
        } else if (roll < 45) { // 20% - Зачарованное алмазное оружие
            return createEnchantedDiamondWeapon();
        } else if (roll < 65) { // 20% - Алмазы
            return new ItemStack(Material.DIAMOND, 5 + random.nextInt(16));
        } else if (roll < 75) { // 10% - Золотые яблоки
            return new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(4));
        } else if (roll < 85) { // 10% - Изумруды
            return new ItemStack(Material.EMERALD, 3 + random.nextInt(10));
        } else if (roll < 92) { // 7% - Жемчуг края
            return new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(8));
        } else { // 8% - Зачарованные книги
            return createEnchantedBook();
        }
    }
    
    private static ItemStack generateEpicLoot() {
        int roll = random.nextInt(100);
        
        if (roll < 15) { // 15% - Зачарованная незеритовая броня
            return createEnchantedNetheriteArmor();
        } else if (roll < 28) { // 13% - Зачарованное незеритовое оружие
            return createEnchantedNetheriteWeapon();
        } else if (roll < 38) { // 10% - Незеритовые слитки
            return new ItemStack(Material.NETHERITE_INGOT, 1 + random.nextInt(3));
        } else if (roll < 50) { // 12% - Алмазы (больше)
            return new ItemStack(Material.DIAMOND, 10 + random.nextInt(21));
        } else if (roll < 62) { // 12% - Зачарованное золотое яблоко
            return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1 + random.nextInt(3));
        } else if (roll < 72) { // 10% - Тотем бессмертия
            return new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        } else if (roll < 80) { // 8% - Элитры
            return createEnchantedElytra();
        } else if (roll < 87) { // 7% - Осколки незерита
            return new ItemStack(Material.NETHERITE_SCRAP, 3 + random.nextInt(6));
        } else if (roll < 93) { // 6% - Древние обломки
            return new ItemStack(Material.ANCIENT_DEBRIS, 2 + random.nextInt(4));
        } else if (roll < 97) { // 4% - Звезда нижнего мира
            return new ItemStack(Material.NETHER_STAR, 1);
        } else { // 3% - Мощные зачарованные книги
            return createPowerfulEnchantedBook();
        }
    }
    
    private static ItemStack createEnchantedNetheriteArmor() {
        Material[] armors = {
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS
        };
        
        ItemStack item = new ItemStack(armors[random.nextInt(armors.length)]);
        addRandomEnchantments(item, 3, 5, true);
        return item;
    }
    
    private static ItemStack createEnchantedNetheriteWeapon() {
        Material[] weapons = {
            Material.NETHERITE_SWORD,
            Material.NETHERITE_AXE,
            Material.NETHERITE_PICKAXE
        };
        
        ItemStack item = new ItemStack(weapons[random.nextInt(weapons.length)]);
        addRandomEnchantments(item, 3, 5, true);
        return item;
    }
    
    private static ItemStack createEnchantedDiamondArmor() {
        Material[] armors = {
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS
        };
        
        ItemStack item = new ItemStack(armors[random.nextInt(armors.length)]);
        addRandomEnchantments(item, 2, 4, false);
        return item;
    }
    
    private static ItemStack createEnchantedDiamondWeapon() {
        Material[] weapons = {
            Material.DIAMOND_SWORD,
            Material.DIAMOND_AXE,
            Material.DIAMOND_PICKAXE
        };
        
        ItemStack item = new ItemStack(weapons[random.nextInt(weapons.length)]);
        addRandomEnchantments(item, 2, 4, false);
        return item;
    }
    
    private static ItemStack createEnchantedElytra() {
        ItemStack item = new ItemStack(Material.ELYTRA);
        item.addEnchantment(Enchantment.UNBREAKING, 3);
        item.addEnchantment(Enchantment.MENDING, 1);
        return item;
    }
    
    private static ItemStack createEnchantedBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        addRandomEnchantments(book, 1, 2, false);
        return book;
    }
    
    private static ItemStack createPowerfulEnchantedBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        addRandomEnchantments(book, 2, 4, true);
        return book;
    }
    
    private static void addRandomEnchantments(ItemStack item, int min, int max, boolean highLevel) {
        List<Enchantment> validEnchants = new ArrayList<>();
        
        for (Enchantment ench : Enchantment.values()) {
            if (ench.canEnchantItem(item) || item.getType() == Material.ENCHANTED_BOOK) {
                validEnchants.add(ench);
            }
        }
        
        Collections.shuffle(validEnchants);
        int count = min + random.nextInt(max - min + 1);
        
        for (int i = 0; i < count && i < validEnchants.size(); i++) {
            Enchantment ench = validEnchants.get(i);
            int maxLevel = highLevel ? ench.getMaxLevel() : Math.min(ench.getMaxLevel(), 3);
            int level = Math.max(1, maxLevel - random.nextInt(2));
            item.addUnsafeEnchantment(ench, level);
        }
    }
    
    public static ItemStack getRealLoot(Location chestLoc, int slot) {
        Map<Integer, ItemStack> loot = chestLoot.get(chestLoc);
        if (loot == null) return null;
        return loot.remove(slot); // Удаляем из карты после получения
    }
    
    public static void decrementItemCount(Location chestLoc) {
        Integer count = chestItemCount.get(chestLoc);
        if (count != null && count > 0) {
            chestItemCount.put(chestLoc, count - 1);
        }
    }
    
    public static boolean isChestEmpty(Location chestLoc) {
        Integer count = chestItemCount.get(chestLoc);
        return count == null || count <= 0;
    }
    
    public static LootRarity getChestRarity(Location chestLoc) {
        return chestRarity.getOrDefault(chestLoc, LootRarity.COMMON);
    }
    
    public static void removeChestData(Location chestLoc) {
        chestLoot.remove(chestLoc);
        chestItemCount.remove(chestLoc);
        chestRarity.remove(chestLoc);
    }
}
