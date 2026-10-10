package phantom.util;

/**
 * Small maths helpers used by physics, camera and art code.
 * <p>OOP concept: utility class with a private constructor (stateless helpers, no globals).</p>
 */
public final class GameMath {
    private GameMath() { }

    /** Moves {@code current} towards {@code target} by at most {@code delta}. */
    public static float approach(float current, float target, float delta) {
        if (current < target) return Math.min(current + delta, target);
        return Math.max(current - delta, target);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    /** Frame-rate independent exponential smoothing. */
    public static float damp(float current, float target, float lambda, float dt) {
        return lerp(current, target, 1f - (float) Math.exp(-lambda * dt));
    }

    public static float sign(float v) {
        return v > 0 ? 1f : (v < 0 ? -1f : 0f);
    }

    /** Deterministic pseudo random value in [0,1) for a pair of integers (used for tile variation). */
    public static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0x7fffffff) / (float) 0x7fffffff;
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}
