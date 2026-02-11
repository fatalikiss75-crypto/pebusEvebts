package dev.kirill.kirevents.events;

import dev.kirill.kirevents.KirEvents;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VotingManager {
    
    private final KirEvents plugin;
    private final Map<EventType, Integer> votes;
    private final Set<UUID> votedPlayers;
    private final EventType[] votingOptions;
    private BukkitTask votingTask;
    private boolean votingActive;
    private int timeRemaining;
    
    // Случайный выбор 3 уникальных ивентов для голосования
    private static final List<EventType[]> PENDING_EVENTS = List.of(
        new EventType[]{EventType.BEACON, EventType.AIRDROP, EventType.SNAKE}
    );
    
    public VotingManager(KirEvents plugin) {
        this.plugin = plugin;
        this.votes = new ConcurrentHashMap<>();
        this.votedPlayers = ConcurrentHashMap.newKeySet();
        this.votingActive = false;
        this.votingOptions = new EventType[3];
        this.timeRemaining = 30; // 30 секунд на голосование
    }
    
    public void startVoting() {
        if (votingActive) return;
        
        // Очищаем предыдущие голоса
        votes.clear();
        votedPlayers.clear();
        
        // Выбираем случайные ивенты
        selectRandomEvents();
        
        votingActive = true;
        timeRemaining = 30;
        
        // Анонсируем начало голосования
        announceVotingStart();
        
        // Запускаем таймер
        startVotingTimer();
    }
    
    private void selectRandomEvents() {
        List<EventType> allEvents = Arrays.asList(EventType.values());
        Collections.shuffle(allEvents);
        
        for (int i = 0; i < 3 && i < allEvents.size(); i++) {
            votingOptions[i] = allEvents.get(i);
            votes.put(votingOptions[i], 0);
        }
    }
    
    private void announceVotingStart() {
        String title = "§e§l🎯 ГОЛОСОВАНИЕ ЗА ИВЕНТ 🎯";
        String subtitle = "§7Выберите ивент на следующие 60 минут!";
        
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendTitle(title, subtitle, 20, 100, 20);
            player.sendMessage("");
            player.sendMessage("§8§m                                                      ");
            player.sendMessage("§6§l🎯 ГОЛОСОВАНИЕ ЗА ИВЕНТ");
            player.sendMessage("§8§m                                                      ");
            player.sendMessage("§7У вас есть 30 секунд чтобы выбрать!");
            player.sendMessage("");
            
            for (int i = 0; i < 3; i++) {
                if (votingOptions[i] != null) {
                    EventType event = votingOptions[i];
                    player.sendMessage("§e" + (i + 1) + ". §r" + event.getHexName());
                    player.sendMessage("    §7" + event.getDescription());
                    player.sendMessage("");
                }
            }
            
            player.sendMessage("§8§m                                                      ");
            player.sendMessage("§7Используйте: §e/vote <номер> §7для голосования!");
            player.sendMessage("§8§m                                                      ");
            player.sendMessage("");
        });
    }
    
    private void startVotingTimer() {
        votingTask = new BukkitRunnable() {
            @Override
            public void run() {
                timeRemaining--;
                
                // Уведомляем каждые 5 секунд
                if (timeRemaining > 0 && timeRemaining % 5 == 0) {
                    String msg = "§a§l⏰ " + timeRemaining + " секунд до конца голосования!";
                    msg += " §7Используйте: §e/vote <номер>";
                    Bukkit.broadcastMessage(msg);
                }
                
                // Время истекло
                if (timeRemaining <= 0) {
                    endVoting();
                    cancel();
                }
            }
        };
        
        votingTask.runTaskTimer(plugin, 20L, 20L); // Каждую секунду
    }
    
    private void endVoting() {
        votingActive = false;
        
        // Определяем победителя
        EventType winner = getWinningEvent();
        
        // Показываем результаты
        showResults(winner);
        
        // Спавним выбранный ивент через 5 секунд
        scheduleWinnerEvent(winner);
        
        // Планируем следующее голосование через 60 минут
        scheduleNextVoting();
    }
    
    private EventType getWinningEvent() {
        EventType winner = null;
        int maxVotes = -1;
        
        for (EventType event : votes.keySet()) {
            int voteCount = votes.get(event);
            if (voteCount > maxVotes) {
                maxVotes = voteCount;
                winner = event;
            }
        }
        
        // Если никого не голосовало, выбираем случайно
        if (winner == null || maxVotes == 0) {
            List<EventType> votingList = Arrays.asList(votingOptions);
            votingList.removeIf(Objects::isNull);
            if (!votingList.isEmpty()) {
                winner = votingList.get(new Random().nextInt(votingList.size()));
            }
        }
        
        return winner != null ? winner : EventType.BEACON;
    }
    
    private void showResults(EventType winner) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§m                                                      ");
        Bukkit.broadcastMessage("§6§l📊 РЕЗУЛЬТАТЫ ГОЛОСОВАНИЯ");
        Bukkit.broadcastMessage("§8§m                                                      ");
        Bukkit.broadcastMessage("");
        
        // Показываем все голоса
        for (int i = 0; i < 3; i++) {
            if (votingOptions[i] != null) {
                EventType event = votingOptions[i];
                int voteCount = votes.get(event);
                String voteBar = "§7" + "█".repeat(Math.max(0, voteCount));
                if (event.equals(winner)) {
                    Bukkit.broadcastMessage("§e" + (i + 1) + ". §a§lПОБЕДИТЕЛЬ! " + event.getHexName());
                    Bukkit.broadcastMessage("    §7Голосов: " + voteBar + " §a§l" + voteCount);
                } else {
                    Bukkit.broadcastMessage("§e" + (i + 1) + ". §7" + event.getHexName());
                    Bukkit.broadcastMessage("    §7Голосов: " + voteBar + " §7" + voteCount);
                }
                Bukkit.broadcastMessage("");
            }
        }
        
        Bukkit.broadcastMessage("§8§m                                                      ");
        Bukkit.broadcastMessage("§6§l🎯 Победитель: " + winner.getHexName());
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Ивент появится через §e5 секунд!");
        Bukkit.broadcastMessage("§8§m                                                      ");
        Bukkit.broadcastMessage("");
    }
    
    private void scheduleWinnerEvent(EventType winner) {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getEventManager().spawnEvent(winner);
            }
        }.runTaskLater(plugin, 5 * 20L); // 5 секунд
    }
    
    private void scheduleNextVoting() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getEventManager().isRunning()) return;
                startVoting();
            }
        }.runTaskLater(plugin, 60 * 60 * 20L); // 60 минут
    }
    
    public void addVote(UUID playerId, EventType event) {
        if (!votingActive || !votes.containsKey(event)) return;
        
        votedPlayers.add(playerId);
        votes.put(event, votes.get(event) + 1);
        
        // Обновляем голоса в реальном времени
        updateVoteDisplay();
    }
    
    private void updateVoteDisplay() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            showCurrentVotes(player);
        }
    }
    
    private void showCurrentVotes(Player player) {
        // Можно добавить отображение текущих голосов в меню
        // Для упрощения показываем только финальные результаты
    }
    
    public EventType getEventAtPosition(int position) {
        if (position < 1 || position > 3) return null;
        return votingOptions[position - 1];
    }
    
    public int getVotes(EventType event) {
        return votes.getOrDefault(event, 0);
    }
    
    public boolean hasVoted(UUID playerId) {
        return votedPlayers.contains(playerId);
    }
    
    public boolean isVotingActive() {
        return votingActive;
    }
    
    public int getTimeRemaining() {
        return timeRemaining;
    }
    
    public void stopVoting() {
        votingActive = false;
        if (votingTask != null) {
            votingTask.cancel();
        }
        votes.clear();
        votedPlayers.clear();
    }
}