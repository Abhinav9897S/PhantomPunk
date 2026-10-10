package phantom.input;

/**
 * Logical player actions, independent of keys or gamepad buttons.
 * <p>OOP concept: enum used as a key for {@link java.util.EnumSet}/{@link java.util.EnumMap}.</p>
 */
public enum GameAction {
    LEFT, RIGHT, UP, DOWN, JUMP, ATTACK, DASH, BOLT, FOCUS, INTERACT, MAP, PAUSE, CONFIRM, BACK
}
