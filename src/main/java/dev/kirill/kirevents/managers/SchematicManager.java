package dev.kirill.kirevents.managers;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import dev.kirill.kirevents.KirEvents;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;

public class SchematicManager {
    
    private final KirEvents plugin;
    private final File schematicsFolder;
    private final Map<UUID, Location> pos1Map;
    private final Map<UUID, Location> pos2Map;
    
    public SchematicManager(KirEvents plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
        this.pos1Map = new HashMap<>();
        this.pos2Map = new HashMap<>();
    }
    
    public void setPos1(Player player, Location location) {
        pos1Map.put(player.getUniqueId(), location);
    }
    
    public void setPos2(Player player, Location location) {
        pos2Map.put(player.getUniqueId(), location);
    }
    
    public Location getPos1(Player player) {
        return pos1Map.get(player.getUniqueId());
    }
    
    public Location getPos2(Player player) {
        return pos2Map.get(player.getUniqueId());
    }
    
    public boolean saveSchematic(Player player, String name) {
        Location pos1 = pos1Map.get(player.getUniqueId());
        Location pos2 = pos2Map.get(player.getUniqueId());
        
        if (pos1 == null || pos2 == null) {
            return false;
        }
        
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            return false;
        }
        
        try {
            BlockVector3 min = BlockVector3.at(
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ())
            );
            
            BlockVector3 max = BlockVector3.at(
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ())
            );
            
            CuboidRegion region = new CuboidRegion(
                BukkitAdapter.adapt(pos1.getWorld()),
                min,
                max
            );
            
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            
            ForwardExtentCopy copy = new ForwardExtentCopy(
                BukkitAdapter.adapt(pos1.getWorld()),
                region,
                clipboard,
                region.getMinimumPoint()
            );
            
            copy.setCopyingEntities(false);
            copy.setCopyingBiomes(false);
            Operations.complete(copy);
            
            File file = new File(schematicsFolder, name + ".schem");
            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
                writer.write(clipboard);
            }
            
            return true;
        } catch (WorldEditException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public SchematicData loadSchematic(String name) {
        File file = new File(schematicsFolder, name + ".schem");
        if (!file.exists()) {
            return null;
        }
        
        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            return new SchematicData(clipboard);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public List<Location> pasteSchematic(SchematicData data, Location location) {
        List<Location> chestLocations = new ArrayList<>();
        
        try {
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());
            
            Operation operation = new ClipboardHolder(data.getClipboard())
                .createPaste(world)
                .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                .ignoreAirBlocks(false)
                .build();
            
            Operations.complete(operation);
            
            // Поиск табличек с [loot]
            BlockVector3 origin = BlockVector3.at(location.getX(), location.getY(), location.getZ());
            BlockVector3 clipboardOffset = data.getClipboard().getOrigin();
            
            for (int x = data.getClipboard().getMinimumPoint().x(); x <= data.getClipboard().getMaximumPoint().x(); x++) {
                for (int y = data.getClipboard().getMinimumPoint().y(); y <= data.getClipboard().getMaximumPoint().y(); y++) {
                    for (int z = data.getClipboard().getMinimumPoint().z(); z <= data.getClipboard().getMaximumPoint().z(); z++) {
                        BlockVector3 pos = BlockVector3.at(x, y, z);
                        BlockVector3 offset = pos.subtract(clipboardOffset);
                        BlockVector3 worldPos = origin.add(offset);
                        
                        Location checkLoc = new Location(
                            location.getWorld(),
                            worldPos.x(),
                            worldPos.y(),
                            worldPos.z()
                        );
                        
                        Block block = checkLoc.getBlock();
                        if (block.getState() instanceof Sign sign) {
                            String[] lines = sign.getLines();
                            if (lines.length > 0 && lines[0].equalsIgnoreCase("[loot]")) {
                                // Заменяем табличку на эндер-сундук
                                block.setType(Material.ENDER_CHEST);
                                chestLocations.add(checkLoc);
                            }
                        }
                    }
                }
            }
            
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
        
        return chestLocations;
    }
    
    public boolean schematicExists(String name) {
        File file = new File(schematicsFolder, name + ".schem");
        return file.exists();
    }
    
    public List<String> getSchematics() {
        List<String> schematics = new ArrayList<>();
        File[] files = schematicsFolder.listFiles((dir, name) -> name.endsWith(".schem"));
        if (files != null) {
            for (File file : files) {
                schematics.add(file.getName().replace(".schem", ""));
            }
        }
        return schematics;
    }
    
    public static class SchematicData {
        private final Clipboard clipboard;
        
        public SchematicData(Clipboard clipboard) {
            this.clipboard = clipboard;
        }
        
        public Clipboard getClipboard() {
            return clipboard;
        }
    }
}
