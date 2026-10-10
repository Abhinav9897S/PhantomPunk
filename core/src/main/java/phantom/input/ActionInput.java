package phantom.input;

import java.util.EnumSet;
import java.util.Set;

/**
 * Base class that turns raw "is this held?" queries into held / just-pressed state.
 * Subclasses only answer {@link #readRaw(GameAction)}.
 * <p>OOP concept: <b>template method</b> + inheritance; {@link EnumSet} for compact state.</p>
 */
public abstract class ActionInput implements InputSource {
    private final Set<GameAction> current = EnumSet.noneOf(GameAction.class);
    private final Set<GameAction> previous = EnumSet.noneOf(GameAction.class);
    /** Keys held during a screen change; ignored until released so presses do not leak through. */
    private final Set<GameAction> consumed = EnumSet.noneOf(GameAction.class);

    /** Samples the device once per simulation tick. */
    public final void poll() {
        previous.clear();
        previous.addAll(current);
        current.clear();
        for (GameAction action : GameAction.values()) {
            if (readRaw(action)) current.add(action);
        }
        consumed.retainAll(current);
    }

    /** Ignores every currently held key until it is released (called on screen changes). */
    public final void consumeAll() {
        consumed.addAll(current);
    }

    protected abstract boolean readRaw(GameAction action);

    @Override
    public boolean isDown(GameAction action) {
        return current.contains(action) && !consumed.contains(action);
    }

    @Override
    public boolean isJustPressed(GameAction action) {
        return isDown(action) && !previous.contains(action);
    }
}
