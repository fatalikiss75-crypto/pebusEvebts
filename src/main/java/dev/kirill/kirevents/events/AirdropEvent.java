package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.managers.SchematicManager;
import dev.kirill.kirevents.utils.HologramManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AirdropEvent extends EventStructure {

    private BossBar bossBar;
    private BukkitRunnable bossBarTask;

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

        Bukkit.getScheduler().runTaskLater(plugin, this::createBossBar, 10L);
        scheduleDespawn();
    }

    private void spawnFromSchematic(Location landLoc) {
        SchematicManager.SchematicData data = plugin.getSchematicManager().loadSchematic("airdrop");
        if (data != null) {
            List<SchematicManager.ChestLocationData> chests = plugin.getSchematicManager().pasteSchematic(data, landLoc);

            if (!chests.isEmpty()) {
                setupChest(chests.get(0).getLocation(), 1, chests.get(0).getDirection());
            }
        }
    }

    private void buildDefaultAirdrop(Location landLoc) {
        setupChest(landLoc, 1, BlockFace.SOUTH);
    }

    private void setupChest(Location chestLoc, int chestNumber, BlockFace direction) {
        List<ItemStack> configuredLoot =
                plugin.getLootConfigManager().getLoot(EventType.AIRDROP, chestNumber);

        Block chestBlock = chestLoc.getBlock();
        chestBlock.setType(Material.CHEST);

        org.bukkit.block.data.type.Chest chestData = (org.bukkit.block.data.type.Chest) chestBlock.getBlockData();
        chestData.setFacing(direction);
        chestData.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
        chestBlock.setBlockData(chestData, true);

        chestBlock.getState().update(true, true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (chestLoc.getBlock().getType() != Material.CHEST) {
                plugin.getLogger().warning("Airdrop chest block disappeared at " + chestLoc);
                return;
            }

            if (!(chestLoc.getBlock().getState() instanceof Chest chest)) {
                plugin.getLogger().warning("Failed to create airdrop chest at " + chestLoc);
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

                ItemStack loot = !configuredLoot.isEmpty() && i < configuredLoot.size()
                        ? configuredLoot.get(i).clone()
                        : generateDefaultLoot();

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

            chest.update(true, false);

            plugin.getEventListener().registerChest(chestLoc, spawnTime, lootMap);
            addChest(chestLoc);

            HologramManager.createHologram(
                    plugin,
                    chestLoc,
                    plugin.getEventListener().getUnlockTime(chestLoc),
                    plugin.getEventListener().getExpireTime(chestLoc),
                    "ЭПИЧЕСКИЙ",
                    "§5§l"
            );

            plugin.getLogger().info("Successfully created airdrop chest at " + chestLoc);
        }, 3L);
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

    private void createBossBar() {
        bossBar = Bukkit.createBossBar("§5✦ Адский Аирдроп", BarColor.PURPLE, BarStyle.SEGMENTED_10);
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
                int duration = plugin.getConfig().getInt("timings.airdrop.duration", 30);
                long endTime = spawnTime + (duration * 60 * 1000L);

                bossBar.removeAll();
                Location checkLoc = chestLocations.isEmpty() ? location : chestLocations.get(0);
                for (Player player : checkLoc.getWorld().getPlayers()) {
                    if (player.getLocation().distance(checkLoc) <= 75) {
                        bossBar.addPlayer(player);
                    }
                }

                if (now >= endTime) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }

                if (!initialized) {
                    bossBar.setTitle("§e⏳ Аирдроп готовится...");
                    bossBar.setColor(BarColor.YELLOW);
                    bossBar.setProgress(1.0);
                } else if (now < unlockTime) {
                    long timeLeft = (unlockTime - now) / 1000;
                    long minutes = timeLeft / 60;
                    long seconds = timeLeft % 60;
                    bossBar.setTitle(String.format("§c🔒 Аирдроп заблокирован §7│ §eОткрытие через: %d:%02d", minutes, seconds));
                    bossBar.setColor(BarColor.RED);
                    double progress = Math.max(0, Math.min(1, (double) (unlockTime - now) / (5 * 60 * 1000)));
                    bossBar.setProgress(progress);
                } else if (now < expireTime) {
                    long timeLeft = (expireTime - now) / 1000;
                    long minutes = timeLeft / 60;
                    long seconds = timeLeft % 60;
                    bossBar.setTitle(String.format("§a✔ Аирдроп открыт §7│ §eВремя: %d:%02d", minutes, seconds));
                    bossBar.setColor(BarColor.PURPLE);
                    double progress = Math.max(0, Math.min(1, (double) (expireTime - now) / (25 * 60 * 1000)));
                    bossBar.setProgress(progress);
                } else {
                    bossBar.setTitle("§c✖ Аирдроп истек!");
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
        Bukkit.broadcastMessage(type.getHexName());
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("  §7Координаты: §f" + location.getBlockX() + " " +
                groundY + " " + location.getBlockZ());
        Bukkit.broadcastMessage("  §7Редкость: §5§lЭПИЧЕСКИЙ");
        Bukkit.broadcastMessage("  §7Описание: " + type.getDescription());
        Bukkit.broadcastMessage("  §7Охрана: §c4 Зомби §7(100 HP)");
        Bukkit.broadcastMessage("  §7Открытие через: §e§l5 минут");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("");
    }
}