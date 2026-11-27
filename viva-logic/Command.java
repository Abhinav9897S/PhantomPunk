/**
 * Everything the player can do in one turn.
 * OOP: ENUM WITH FIELDS + a static lookup method.
 */
public enum Command {
    ATTACK("a", "Strike the nearest enemy"),
    BOLT("b", "Light Bolt (33 soul)"),
    FOCUS("h", "Focus to heal (33 soul)"),
    DASH("j", "Dash - dodge this turn's attacks"),
    INTERACT("e", "Use a bench / talk / touch a shrine"),
    NEXT_ROOM("d", "Go to the next room"),
    SAVE("s", "Save the game"),
    QUIT("q", "Quit to the menu");

    private final String key;
    private final String description;

    Command(String key, String description) {
        this.key = key;
        this.description = description;
    }

    /** Finds the command for a typed key, or null if there is none. */
    public static Command fromKey(String typed) {
        for (Command c : values()) {
            if (c.key.equalsIgnoreCase(typed.trim())) return c;
        }
        return null;
    }

    public String getKey() { return key; }
    public String getDescription() { return description; }
}
