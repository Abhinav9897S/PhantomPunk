package phantom.systems;

import phantom.util.GameMath;

/**
 * Smooth-follow camera with look-ahead, room clamping and trauma-based screen shake.
 * Works purely with numbers so it can be tested headless; the renderer copies the result
 * into a libGDX {@code OrthographicCamera}.
 * <p>OOP concept: encapsulated system object; interpolation state for smooth rendering.</p>
 */
public final class CameraController {
    private static final float FOLLOW_SPEED = 6.5f;
    private static final float LOOK_AHEAD = 70f;
    private static final float MAX_SHAKE = 14f;

    private final float viewWidth;
    private final float viewHeight;
    private float x;
    private float y;
    private float prevX;
    private float prevY;
    private float lookAhead;
    private float trauma;
    private float time;
    private boolean shakeEnabled = true;

    public CameraController(float viewWidth, float viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    /** Follows a target (player centre) inside a room of the given size. */
    public void update(float targetX, float targetY, float facing, float lookY, float roomW, float roomH, float dt) {
        prevX = x;
        prevY = y;
        time += dt;
        lookAhead = GameMath.damp(lookAhead, facing * LOOK_AHEAD, 2.5f, dt);
        float tx = clampAxis(targetX + lookAhead, viewWidth, roomW);
        float ty = clampAxis(targetY + 40f + lookY, viewHeight, roomH);
        x = GameMath.damp(x, tx, FOLLOW_SPEED, dt);
        y = GameMath.damp(y, ty, FOLLOW_SPEED * 1.2f, dt);
        x = clampAxis(x, viewWidth, roomW);
        y = clampAxis(y, viewHeight, roomH);
        trauma = Math.max(0f, trauma - dt * 1.6f);
    }

    /** Jumps straight to the target (after a room change). */
    public void snap(float targetX, float targetY, float roomW, float roomH) {
        x = clampAxis(targetX, viewWidth, roomW);
        y = clampAxis(targetY + 40f, viewHeight, roomH);
        prevX = x;
        prevY = y;
        lookAhead = 0f;
    }

    /** Adds shake; 0.3 is a light hit, 1.0 a boss slam. */
    public void addTrauma(float amount) {
        if (shakeEnabled) trauma = Math.min(1f, trauma + amount);
    }

    private static float clampAxis(float value, float view, float room) {
        if (room <= view) return room * 0.5f;
        return GameMath.clamp(value, view * 0.5f, room - view * 0.5f);
    }

    public float renderX(float alpha) {
        return GameMath.lerp(prevX, x, alpha) + shakeOffset(13.1f);
    }

    public float renderY(float alpha) {
        return GameMath.lerp(prevY, y, alpha) + shakeOffset(17.7f);
    }

    private float shakeOffset(float freq) {
        float s = trauma * trauma;
        return (float) (Math.sin(time * freq * 6.0) * Math.cos(time * freq * 2.3)) * MAX_SHAKE * s;
    }

    public void setShakeEnabled(boolean enabled) {
        shakeEnabled = enabled;
        if (!enabled) trauma = 0f;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getTrauma() { return trauma; }
    public float getViewWidth() { return viewWidth; }
    public float getViewHeight() { return viewHeight; }
}
