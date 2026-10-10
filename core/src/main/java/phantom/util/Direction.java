package phantom.util;

/**
 * The four cardinal directions with their unit vectors.
 * <p>OOP concept: enum with fields and behaviour (not just named integers).</p>
 */
public enum Direction {
    LEFT(-1, 0), RIGHT(1, 0), UP(0, 1), DOWN(0, -1);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() { return dx; }

    public int dy() { return dy; }

    public boolean isHorizontal() { return dy == 0; }

    public Direction opposite() {
        switch (this) {
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            case UP: return DOWN;
            default: return UP;
        }
    }

    /** Horizontal direction for a signed value; zero maps to {@code fallback}. */
    public static Direction horizontal(float sign, Direction fallback) {
        if (sign > 0) return RIGHT;
        if (sign < 0) return LEFT;
        return fallback;
    }
}
