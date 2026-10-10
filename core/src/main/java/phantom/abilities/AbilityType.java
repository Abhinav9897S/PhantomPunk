package phantom.abilities;

/**
 * Every ability in the game, with its in-game name and a control hint.
 * Declaration order is also the priority order used when several abilities want the jump key.
 * <p>OOP concept: enum used as an {@link java.util.EnumMap} key in {@link phantom.components.AbilitySet}.</p>
 */
public enum AbilityType {
    DASH("Umbral Dash", "Press K / Shift to dash. It also works in mid-air."),
    WALL_CLING("Moss Grip", "Hold towards a wall while falling to cling. Jump to leap off it."),
    DOUBLE_JUMP("Wisp Leap", "Press Jump again in mid-air to leap on a wisp of light."),
    LIGHT_BOLT("Light Bolt", "Press L / Q to hurl a bolt of Light. Costs one third of the soul orb."),
    FOCUS("Focus", "Hold F on the ground to turn Light into a restored flame.");

    private final String displayName;
    private final String hint;

    AbilityType(String displayName, String hint) {
        this.displayName = displayName;
        this.hint = hint;
    }

    public String getDisplayName() { return displayName; }
    public String getHint() { return hint; }
}
