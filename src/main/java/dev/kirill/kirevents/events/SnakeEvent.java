package dev.kirill.kirevents.events;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.managers.SchematicManager;
import dev.kirill.kirevents.utils.HologramManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SnakeEvent extends EventStructure {

    private final Random random;
    private BossBar bossBar;
    private BukkitRunnable bossBarTask;

    public SnakeEvent(KirEvents plugin, Location location) {
        super(plugin, location, EventType.SNAKE);
        this.random = new Random();
    }

    @Override
    public void spawn() {
        if (!checkWorldGuard()) {
            findAlternativeLocation();
        }

        location.getWorld().playSound(location, Sound.BLOCK_END_PORTAL_SPAWN,
                SoundCategory.MASTER, 10000f, 0.1f);

        announceSpawn();

        if (plugin.getSchematicManager().schematicExists("snake")) {
            spawnFromSchematic();
        } else {
            buildDefaultSnake();
        }

        plugin.getEventManager().addStructure(this);

        Bukkit.getScheduler().runTaskLater(plugin, this::createBossBar, 10L);
        scheduleDespawn();
    }

    private void spawnFromSchematic() {
        SchematicManager.SchematicData data = plugin.getSchematicManager().loadSchematic("snake");
        if (data != null) {
            List<SchematicManager.ChestLocationData> chests = plugin.getSchematicManager().pasteSchematic(data, location);

            int chestNum = 1;
            for (SchematicManager.ChestLocationData chestData : chests) {
                if (chestNum <= 10) {
                    setupChest(chestData.getLocation(), chestNum, chestData.getDirection());
                }
                chestNum++;
            }
        }
    }

    private void buildDefaultSnake() {
        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        List<Location> snakePath = generateSnakePath(baseX, baseY, baseZ);

        for (int i = 0; i < snakePath.size(); i++) {
            Location point = snakePath.get(i);

            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > 3.2) continue;

                    Block block = world.getBlockAt(
                            point.getBlockX() + dx,
                            point.getBlockY(),
                            point.getBlockZ() + dz
                    );

                    if ((i + dx + dz) % 3 == 0) {
                        block.setType(Material.GREEN_TERRACOTTA);
                    } else if ((i + dx + dz) % 3 == 1) {
                        block.setType(Material.GREEN_CONCRETE);
                    } else {
                        block.setType(Material.LIME_TERRACOTTA);
                    }

                    addBlock(block);
                }
            }
        }

        int chestCount = 10 + random.nextInt(11);
        int pathLength = snakePath.size();

        for (int i = 0; i < chestCount && i < 10; i++) {
            int pathIndex = (pathLength / (chestCount + 1)) * (i + 1);
            if (pathIndex < pathLength) {
                Location chestLoc = snakePath.get(pathIndex).clone().add(0, 1, 0);

                BlockFace direction = BlockFace.NORTH;
                if (pathIndex > 0 && pathIndex < snakePath.size()) {
                    Location current = snakePath.get(pathIndex);
                    Location previous = snakePath.get(pathIndex - 1);

                    int dx = current.getBlockX() - previous.getBlockX();
                    int dz = current.getBlockZ() - previous.getBlockZ();

                    if (dx > 0) direction = BlockFace.WEST;
                    else if (dx < 0) direction = BlockFace.EAST;
                    else if (dz > 0) direction = BlockFace.NORTH;
                    else if (dz < 0) direction = BlockFace.SOUTH;
                }

                setupChest(chestLoc, i + 1, direction);
            }
        }

        buildCloudHead(snakePath);
    }

    private void setupChest(Location chestLoc, int chestNumber, BlockFace direction) {
        List<ItemStack> configuredLoot = plugin.getLootConfigManager().getLoot(EventType.SNAKE, chestNumber);

        Block chestBlock = chestLoc.getBlock();
        chestBlock.setType(Material.CHEST);

        org.bukkit.block.data.type.Chest chestData = (org.bukkit.block.data.type.Chest) chestBlock.getBlockData();
        chestData.setFacing(direction);
        chestData.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
        chestBlock.setBlockData(chestData, true);

        boolean isEpic = random.nextInt(100) < 40;

        chestBlock.getState().update(true, true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (chestLoc.getBlock().getType() != Material.CHEST) {
                plugin.getLogger().warning("Snake chest block disappeared at " + chestLoc);
                return;
            }

            if (!(chestLoc.getBlock().getState() instanceof Chest chest)) {
                plugin.getLogger().warning("Failed to create snake chest at " + chestLoc);
                return;
            }

            Map<Integer, ItemStack> lootMap = new HashMap<>();
            Inventory inv = chest.getInventory();

            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                slots.add(i);
            }
            Collections.shuffle(slots);

            int itemCount = configuredLoot.isEmpty() ? 27 : Math.min(configuredLoot.size(), 27);

            for (int i = 0; i < itemCount && i < slots.size(); i++) {
                int slot = slots.get(i);

                ItemStack loot;
                if (!configuredLoot.isEmpty() && i < configuredLoot.size()) {
                    loot = configuredLoot.get(i).clone();
                } else {
                    loot = isEpic ? generateEpicLoot() : generateCommonLoot();
                }

                ItemStack shell = new ItemStack(Material.NAUTILUS_SHELL);
                ItemMeta meta = shell.getItemMeta();

                if (isEpic) {
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

            chest.update(true, false);

            plugin.getEventListener().registerChest(chestLoc, spawnTime, lootMap);
            addChest(chestLoc);

            HologramManager.createHologram(
                    plugin,
                    chestLoc,
                    plugin.getEventListener().getUnlockTime(chestLoc),
                    plugin.getEventListener().getExpireTime(chestLoc),
                    isEpic ? "ЭПИЧЕСКИЙ" : "ОБЫЧНЫЙ",
                    isEpic ? "§5§l" : "§a§l"
            );

            plugin.getLogger().info("Successfully created snake chest #" + chestNumber + " at " + chestLoc);
        }, 3L);
    }

    private ItemStack generateCommonLoot() {
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

    private ItemStack generateEpicLoot() {
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

    private void createBossBar() {
        bossBar = Bukkit.createBossBar("§a⚡ Жалящая Змея", BarColor.GREEN, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);

        bossBarTask = new BukkitRunnable() {
            long unlockTime = 0;
            long expireTime = 0;
            boolean initialized = false;

            @Override
            public void run() {
                if (!initialized && !chestLocations.isEmpty()) {
                    unlockTime = plugin.getEventListener().getUnlockTime(chestLocations.get(0));
                    expireTime = plugin.getEventListener().getExpireTime(chestLocations.get(0));
                    initialized = true;
                }

                long now = System.currentTimeMillis();
                int duration = plugin.getConfig().getInt("timings.snake.duration", 30);
                long endTime = spawnTime + (duration * 60 * 1000L);

                bossBar.removeAll();
                for (Player player : location.getWorld().getPlayers()) {
                    if (player.getLocation().distance(location) <= 75) {
                        bossBar.addPlayer(player);
                    }
                }

                if (now >= endTime) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }

                if (!initialized) {
                    bossBar.setTitle("§e⏳ Змея готовится...");
                    bossBar.setColor(BarColor.YELLOW);
                    bossBar.setProgress(1.0);
                } else if (now < unlockTime) {
                    long timeLeft = (unlockTime - now) / 1000;
                    long minutes = timeLeft / 60;
                    long seconds = timeLeft % 60;
                    bossBar.setTitle(String.format("§c🔒 Змея заблокирована §7│ §eОткрытие через: %d:%02d", minutes, seconds));
                    bossBar.setColor(BarColor.RED);
                    double progress = Math.max(0, Math.min(1, (double) (unlockTime - now) / (5 * 60 * 1000)));
                    bossBar.setProgress(progress);
                } else if (now < expireTime) {
                    long timeLeft = (expireTime - now) / 1000;
                    long minutes = timeLeft / 60;
                    long seconds = timeLeft % 60;
                    bossBar.setTitle(String.format("§a✔ Змея открыта §7│ §eВремя: %d:%02d", minutes, seconds));
                    bossBar.setColor(BarColor.GREEN);
                    double progress = Math.max(0, Math.min(1, (double) (expireTime - now) / (25 * 60 * 1000)));
                    bossBar.setProgress(progress);
                } else {
                    bossBar.setTitle("§c✖ Змея истекла!");
                    bossBar.setColor(BarColor.RED);
                    bossBar.setProgress(0);
                }
            }
        };

        bossBarTask.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void despawn() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        if (bossBarTask != null) {
            bossBarTask.cancel();
            bossBarTask = null;
        }
        super.despawn();
    }

    private void buildCloudHead(List<Location> snakePath) {
        World world = location.getWorld();
        Location headStart = snakePath.get(snakePath.size() - 1);

        for (int x = -6; x <= 6; x++) {
            for (int y = -4; y <= 8; y++) {
                for (int z = -6; z <= 6; z++) {
                    double distance = Math.sqrt(x*x/1.5 + y*y + z*z/1.5);
                    if (distance > 6.5) continue;

                    if (random.nextInt(100) < 85) {
                        Location cloudLoc = headStart.clone().add(x, y, z);
                        Block cloudBlock = cloudLoc.getBlock();
                        cloudBlock.setType(Material.WHITE_WOOL);
                        addBlock(cloudBlock);
                    }
                }
            }
        }
    }

    private List<Location> generateSnakePath(int startX, int startY, int startZ) {
        List<Location> path = new ArrayList<>();
        World world = location.getWorld();

        double x = startX;
        double y = startY;
        double z = startZ;
        double angle = 0;
        double radius = 10;

        for (int i = 0; i <= 70; i++) {
            path.add(new Location(world,
                    (int)(x + Math.cos(angle) * radius),
                    (int)(y + i),
                    (int)(z + Math.sin(angle) * radius)
            ));

            angle += 0.3;

            if (i % 10 == 0) {
                radius = 8 + random.nextInt(5);
            }
        }

        return path;
    }

    private boolean checkWorldGuard() {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
                return true;
            }

            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (regionManager == null) {
                return true;
            }

            for (int x = -15; x <= 15; x++) {
                for (int z = -15; z <= 15; z++) {
                    Location checkLoc = location.clone().add(x, 0, z);
                    if (!regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(checkLoc)).getRegions().isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private void findAlternativeLocation() {
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = location.getBlockX() + random.nextInt(1000) - 500;
            int z = location.getBlockZ() + random.nextInt(1000) - 500;
            int y = location.getWorld().getHighestBlockYAt(x, z) + 1;

            Location newLoc = new Location(location.getWorld(), x, y, z);

            location.setX(newLoc.getX());
            location.setY(newLoc.getY());
            location.setZ(newLoc.getZ());

            if (checkWorldGuard()) {
                return;
            }
        }
    }

    private void announceSpawn() {
        String prefix = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage(type.getHexName());
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("  §7Координаты: §f" + location.getBlockX() + " " +
                location.getBlockY() + " " + location.getBlockZ());
        Bukkit.broadcastMessage("  §7Сундуки: §a§l10-20 ОБЫЧНЫХ");
        Bukkit.broadcastMessage("  §7Редкость: §a§l60% ОБЫЧНЫЕ §7+ §5§l40% ЭПИЧЕСКИЕ");
        Bukkit.broadcastMessage("  §7Описание: " + type.getDescription());
        Bukkit.broadcastMessage("  §7Размер: §e6x6 блоков, высота 70 блоков");
        Bukkit.broadcastMessage("  §7Открытие через: §e§l5 минут");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("");
    }
}