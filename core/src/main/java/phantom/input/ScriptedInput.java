package phantom.input;

import java.util.EnumSet;
import java.util.Set;

/**
 * Input driven by code instead of a device. Used by the headless simulation tests.
 * <p>OOP concept: polymorphism - a drop-in replacement for {@link KeyboardInput}.</p>
 */
public final class ScriptedInput extends ActionInput {
    private final Set<GameAction> held = EnumSet.noneOf(GameAction.class);

    public void press(GameAction action) {
        held.add(action);
    }

    public void release(GameAction action) {
        held.remove(action);
    }

    public void releaseAll() {
        held.clear();
    }

    public void set(GameAction action, boolean down) {
        if (down) press(action); else release(action);
    }

    @Override
    protected boolean readRaw(GameAction action) {
        return held.contains(action);
    }
}
