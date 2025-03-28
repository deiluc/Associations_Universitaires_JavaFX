package models;

public class User {
    private String name;
    private String description;
    private String president;
    private int memberCount;

    public User(String name, String description, String president, int memberCount) {
        this.name = name;
        this.description = description;
        this.president = president;
        this.memberCount = memberCount;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPresident() { return president; }
    public int getMemberCount() { return memberCount; }
}