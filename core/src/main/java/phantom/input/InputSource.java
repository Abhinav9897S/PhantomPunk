package phantom.input;

/**
 * Read-only view of the player's input for one simulation tick.
 * <p>OOP concept: <b>abstraction</b> - the player reads this interface, so it can be driven by
 * the keyboard, a gamepad or a scripted test without any change.</p>
 */
public interface InputSource {
    /** True while the action is held. */
    boolean isDown(GameAction action);

    /** True only on the tick the action was first pressed. */
    boolean isJustPressed(GameAction action);

    /** -1, 0 or +1 from LEFT/RIGHT. */
    default int horizontal() {
        int h = 0;
        if (isDown(GameAction.LEFT)) h--;
        if (isDown(GameAction.RIGHT)) h++;
        return h;
    }
}
