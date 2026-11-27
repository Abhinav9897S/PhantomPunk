/**
 * Every ability in the game.
 *
 * OOP: ENUM WITH FIELDS - each constant carries its own display name and hint
 *      (an enum is a class with a fixed set of objects).
 */
public enum AbilityType {
    FOCUS("Focus", "Heal 1 mask for 33 soul."),
    LIGHT_BOLT("Light Bolt", "Throw a bolt of light: 14 damage for 33 soul."),
    DASH("Umbral Dash", "Dash out of the way: the next attack misses you."),
    DOUBLE_JUMP("Wisp Leap", "Jump again in mid-air to reach high ledges.");

    private final String displayName;
    private final String hint;

    AbilityType(String displayName, String hint) {
        this.displayName = displayName;
        this.hint = hint;
    }

    public String getDisplayName() { return displayName; }
    public String getHint() { return hint; }
}
