package phantom.input;

import java.util.EnumMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;

/**
 * Keyboard plus optional gamepad input. A missing or broken controller backend is ignored.
 * <p>OOP concept: inheritance from {@link ActionInput}; {@link EnumMap} from action to keys.</p>
 */
public final class KeyboardInput extends ActionInput {
    private static final float STICK_DEADZONE = 0.45f;

    private final Map<GameAction, int[]> keys = new EnumMap<>(GameAction.class);
    private boolean controllersAvailable = true;

    public KeyboardInput() {
        keys.put(GameAction.LEFT, new int[] {Keys.A, Keys.LEFT});
        keys.put(GameAction.RIGHT, new int[] {Keys.D, Keys.RIGHT});
        keys.put(GameAction.UP, new int[] {Keys.W, Keys.UP});
        keys.put(GameAction.DOWN, new int[] {Keys.S, Keys.DOWN});
        keys.put(GameAction.JUMP, new int[] {Keys.SPACE, Keys.Z});
        keys.put(GameAction.ATTACK, new int[] {Keys.J, Keys.X});
        keys.put(GameAction.DASH, new int[] {Keys.K, Keys.C, Keys.SHIFT_LEFT});
        keys.put(GameAction.BOLT, new int[] {Keys.L, Keys.Q});
        keys.put(GameAction.FOCUS, new int[] {Keys.F, Keys.V});
        keys.put(GameAction.INTERACT, new int[] {Keys.W, Keys.UP, Keys.E});
        keys.put(GameAction.MAP, new int[] {Keys.TAB, Keys.M});
        keys.put(GameAction.PAUSE, new int[] {Keys.ESCAPE, Keys.P});
        keys.put(GameAction.CONFIRM, new int[] {Keys.ENTER, Keys.SPACE, Keys.J});
        keys.put(GameAction.BACK, new int[] {Keys.ESCAPE, Keys.BACKSPACE});
    }

    @Override
    protected boolean readRaw(GameAction action) {
        for (int key : keys.get(action)) {
            if (key >= 0 && key <= Keys.MAX_KEYCODE && Gdx.input.isKeyPressed(key)) return true;
        }
        return readController(action);
    }

    private boolean readController(GameAction action) {
        if (!controllersAvailable) return false;
        try {
            Controller c = Controllers.getCurrent();
            if (c == null) return false;
            ControllerMapping m = c.getMapping();
            float lx = c.getAxis(m.axisLeftX);
            float ly = c.getAxis(m.axisLeftY);
            switch (action) {
                case LEFT: return lx < -STICK_DEADZONE || c.getButton(m.buttonDpadLeft);
                case RIGHT: return lx > STICK_DEADZONE || c.getButton(m.buttonDpadRight);
                case UP: case INTERACT: return ly < -STICK_DEADZONE || c.getButton(m.buttonDpadUp);
                case DOWN: return ly > STICK_DEADZONE || c.getButton(m.buttonDpadDown);
                case JUMP: case CONFIRM: return c.getButton(m.buttonA);
                case ATTACK: return c.getButton(m.buttonX);
                case DASH: return c.getButton(m.buttonR1) || c.getButton(m.buttonB);
                case BOLT: return c.getButton(m.buttonY);
                case FOCUS: return c.getButton(m.buttonL1);
                case MAP: return c.getButton(m.buttonBack);
                case PAUSE: return c.getButton(m.buttonStart);
                case BACK: return c.getButton(m.buttonB);
                default: return false;
            }
        } catch (Throwable t) {
            // No controller backend on this platform: keep playing with the keyboard.
            controllersAvailable = false;
            return false;
        }
    }
}
