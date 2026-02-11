package dev.kirill.kirevents.events;

public enum EventType {
    BEACON("Маяк"),
    AIRDROP("Аирдроп"),
    SNAKE("Змея");
    
    private final String displayName;
    
    EventType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
