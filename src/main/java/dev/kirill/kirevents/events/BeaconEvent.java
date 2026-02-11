package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.managers.SchematicManager;
import dev.kirill.kirevents.utils.HologramManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BeaconEvent extends EventStructure {

    private BukkitRunnable moneyTask;

    public BeaconEvent(KirEvents plugin, Location location) {
        super(plugin, location, EventType.BEACON);
    }

    @Override
    public void spawn() {
        // Проверяем наличие схематики
        if (plugin.getSchematicManager().schematicExists("beacon")) {
            spawnFromSchematic();
        } else {
            buildDefaultBeacon();
        }

        // Звук
        location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                SoundCategory.MASTER, 10000f, 0.2f);

        // Анонс
        announceSpawn();

        // Регистрация
        plugin.getEventManager().addStructure(this);

        // Запуск системы денег
        startMoneySystem();

        // Удаление через время
        scheduleDespawn();
    }

    private void spawnFromSchematic() {
        SchematicManager.SchematicData data = plugin.getSchematicManager().loadSchematic("beacon");
        if (data != null) {
            List<SchematicManager.ChestLocationData> chests = plugin.getSchematicManager().pasteSchematic(data, location);

            int chestNum = 1;
            for (SchematicManager.ChestLocationData chestData : chests) {
                setupChest(chestData.getLocation(), chestNum);
                chestNum++;
            }
        }
    }

    private void buildDefaultBeacon() {
        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

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

        Block beaconBlock = world.getBlockAt(baseX, baseY + 5, baseZ);
        beaconBlock.setType(Material.BEACON);
        addBlock(beaconBlock);

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

        // Создаем сундуки с правильным направлением
        Map<Location, BlockFace> chestLocations = new HashMap<>();
        chestLocations.put(new Location(world, baseX + 3, baseY + 5, baseZ), BlockFace.WEST);
        chestLocations.put(new Location(world, baseX - 3, baseY + 5, baseZ), BlockFace.EAST);
        chestLocations.put(new Location(world, baseX, baseY + 5, baseZ + 3), BlockFace.NORTH);
        chestLocations.put(new Location(world, baseX, baseY + 5, baseZ - 3), BlockFace.SOUTH);

        int i = 1;
        for (Map.Entry<Location, BlockFace> entry : chestLocations.entrySet()) {
            Location chestLoc = entry.getKey();
            BlockFace direction = entry.getValue();

            Block chestBlock = chestLoc.getBlock();
            chestBlock.setType(Material.ENDER_CHEST);

            // Устанавливаем направление
            if (chestBlock.getBlockData() instanceof Directional directional) {
                directional.setFacing(direction);
                chestBlock.setBlockData(directional);
            }

            addChest(chestLoc);
            setupChest(chestLoc, i);
            i++;
        }
    }

    private void setupChest(Location chestLoc, int chestNumber) {
        // Получаем лут из конфигурации
        List<ItemStack> configuredLoot = plugin.getLootConfigManager().getLoot(EventType.BEACON, chestNumber);

        Map<Integer, ItemStack> lootMap = new HashMap<>();

        // Используем Container вместо Chest для работы с ENDER_CHEST
        if (!(chestLoc.getBlock().getState() instanceof Container container)) {
            plugin.getLogger().warning("Block at " + chestLoc + " is not a Container!");
            return;
        }

        Inventory inv = container.getInventory();

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
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
            meta.setDisplayName("§a§lМистическая Ракушка");
            meta.setLore(Arrays.asList(
                    "§7Нажми, чтобы получить награду!",
                    "§8Что-то ценное внутри..."
            ));
            shell.setItemMeta(meta);

            inv.setItem(slot, shell);
            lootMap.put(slot, loot);
        }

        container.update();

        plugin.getEventListener().registerChest(chestLoc, spawnTime, lootMap);

        // Создаем голограмму
        HologramManager.createHologram(
                plugin,
                chestLoc,
                plugin.getEventListener().getUnlockTime(chestLoc),
                plugin.getEventListener().getExpireTime(chestLoc),
                "ОБЫЧНЫЙ",
                "§a§l"
        );
    }

    private ItemStack generateDefaultLoot() {
        Random random = new Random();
        int roll = random.nextInt(100);

        if (roll < 30) {
            return new ItemStack(Material.DIAMOND, 5 + random.nextInt(16));
        } else if (roll < 50) {
            return new ItemStack(Material.EMERALD, 3 + random.nextInt(10));
        } else if (roll < 70) {
            return new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(4));
        } else {
            return new ItemStack(Material.NETHERITE_INGOT, 1);
        }
    }

    private void startMoneySystem() {
        if (!plugin.hasEconomy()) {
            plugin.getLogger().info("Economy not available, skipping money system for beacon");
            return;
        }

        int moneyRadius = plugin.getConfig().getInt("beacon.money-radius", 15);
        double moneyAmount = plugin.getConfig().getDouble("beacon.money-amount", 1000);
        int moneyInterval = plugin.getConfig().getInt("beacon.money-interval", 60);

        plugin.getLogger().info("Starting money system: " + moneyAmount + " every " + moneyInterval + " seconds in " + moneyRadius + " blocks radius");

        moneyTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Ищем игроков рядом с маяком
                List<Player> nearbyPlayers = new ArrayList<>();
                for (Player player : location.getWorld().getPlayers()) {
                    if (player.getLocation().distance(location) <= moneyRadius) {
                        nearbyPlayers.add(player);
                    }
                }

                if (nearbyPlayers.isEmpty()) {
                    return;
                }

                double amountPerPlayer = moneyAmount / nearbyPlayers.size();

                for (Player player : nearbyPlayers) {
                    plugin.getEconomy().depositPlayer(player, amountPerPlayer);
                    String msg = plugin.getConfig().getString("messages.money-received", "§a§l$ §aПолучено: §f{amount}₽")
                            .replace("{amount}", String.format("%.0f", amountPerPlayer))
                            .replace("&", "§");
                    player.sendMessage(msg);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
        };

        // Запускаем задачу сразу и потом каждые moneyInterval секунд
        moneyTask.runTaskTimer(plugin, 0L, moneyInterval * 20L);
    }

    @Override
    public void despawn() {
        if (moneyTask != null) {
            moneyTask.cancel();
            moneyTask = null;
        }
        super.despawn();
    }

    private void announceSpawn() {
        String prefix = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage(type.getHexName());
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("  §7Координаты: §f" + location.getBlockX() + " " +
                location.getBlockY() + " " + location.getBlockZ());
        Bukkit.broadcastMessage("  §7Сундуки: §a§l4 ОБЫЧНЫХ");
        Bukkit.broadcastMessage("  §7Описание: " + type.getDescription());
        if (plugin.hasEconomy()) {
            Bukkit.broadcastMessage("  §7Деньги: §a§l+" + plugin.getConfig().getDouble("beacon.money-amount") +
                    "₽ §7каждые " + plugin.getConfig().getInt("beacon.money-interval") + " сек");
        }
        Bukkit.broadcastMessage("  §7Открытие через: §e§l5 минут");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("");
    }
}