package dev.kirill.kirevents.events;

public enum EventType {
    BEACON("Маяк", "§c⛦ Маяк Смерти §c⛦", "§cСигнал опасности! Найди маяк и забери награды!"),
    AIRDROP("Аирдроп", "§5✦ Адский Аирдроп ✦", "§5С небес падает ценный груз! Успей первым!"),
    SNAKE("Змея", "§a⚡ Жалящая Змея ⚡", "§aПреследователь из глубин! Найди все сундуки!");
    
    private final String displayName;
    private final String hexName;
    private final String description;
    
    EventType(String displayName, String hexName, String description) {
        this.displayName = displayName;
        this.hexName = hexName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getHexName() {
        return hexName;
    }
    
    public String getDescription() {
        return description;
    }
}
