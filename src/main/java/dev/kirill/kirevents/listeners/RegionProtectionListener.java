package dev.kirill.kirevents.listeners;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.events.EventStructure;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class RegionProtectionListener implements Listener {
    
    private final KirEvents plugin;
    private final int protectionRadius;
    
    public RegionProtectionListener(KirEvents plugin) {
        this.plugin = plugin;
        this.protectionRadius = plugin.getConfig().getInt("settings.region-protection-radius", 75);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInProtectedRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(getMessage("region-protected"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInProtectedRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(getMessage("region-protected"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (isInProtectedRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (isInProtectedRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isInProtectedRegion(block.getLocation()));
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isInProtectedRegion(block.getLocation()));
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isInProtectedRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(getMessage("region-protected"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (isInProtectedRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(getMessage("region-protected"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (isInProtectedRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        
        for (var block : event.getBlocks()) {
            if (isInProtectedRegion(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (isInProtectedRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        
        for (var block : event.getBlocks()) {
            if (isInProtectedRegion(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    private boolean isInProtectedRegion(org.bukkit.Location location) {
        for (EventStructure structure : plugin.getEventManager().getActiveStructures()) {
            if (structure.getLocation().distance(location) <= protectionRadius) {
                return true;
            }
        }
        return false;
    }
    
    private String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return (prefix + msg).replace("&", "§");
    }
}
