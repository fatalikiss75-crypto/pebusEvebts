package dev.kirill.kirevents.commands;

import dev.kirill.kirevents.KirEvents;
import dev.kirill.kirevents.events.EventType;
import dev.kirill.kirevents.events.VotingManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VoteCommand implements CommandExecutor {
    
    private final KirEvents plugin;
    
    public VoteCommand(KirEvents plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков!");
            return true;
        }
        
        VotingManager votingManager = plugin.getEventManager().getVotingManager();
        
        if (!votingManager.isVotingActive()) {
            player.sendMessage("§c§l✖ Сейчас не идет голосование!");
            return true;
        }
        
        if (args.length == 0) {
            showVoteMenu(player);
            return true;
        }
        
        int choice;
        try {
            choice = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§l✖ Используйте: /vote <1-3>");
            return true;
        }
        
        if (choice < 1 || choice > 3) {
            player.sendMessage("§c§l✖ Выберите от 1 до 3!");
            return true;
        }
        
        EventType selectedEvent = votingManager.getEventAtPosition(choice);
        if (selectedEvent == null) {
            player.sendMessage("§c§l✖ Неверный выбор!");
            return true;
        }
        
        if (votingManager.hasVoted(player.getUniqueId())) {
            player.sendMessage("§c§l✖ Вы уже проголосовали!");
            return true;
        }
        
        votingManager.addVote(player.getUniqueId(), selectedEvent);
        player.sendMessage("§a§l✔ Вы проголосовали за: " + selectedEvent.getHexName());
        
        return true;
    }
    
    private void showVoteMenu(Player player) {
        VotingManager votingManager = plugin.getEventManager().getVotingManager();
        
        player.sendMessage("§8§m                                              ");
        player.sendMessage("§6§l🎯 ГОЛОСОВАНИЕ ЗА ИВЕНТ");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("§7Голосование завершится через: §e" + 
                votingManager.getTimeRemaining() + " секунд");
        player.sendMessage("");
        
        for (int i = 0; i < 3; i++) {
            EventType event = votingManager.getEventAtPosition(i + 1);
            if (event == null) continue;
            
            int votes = votingManager.getVotes(event);
            player.sendMessage("§e" + (i + 1) + ". §r" + event.getHexName());
            player.sendMessage("    §7Описание: " + event.getDescription());
            player.sendMessage("    §a§lГолосов: " + votes);
            player.sendMessage("");
        }
        
        player.sendMessage("§8§m                                              ");
        player.sendMessage("§7Используйте: §e/vote <номер>");
        player.sendMessage("§8§m                                              ");
    }
}