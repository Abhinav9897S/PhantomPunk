package phantom.util;

/**
 * Game-wide tuning constants (window size, physics, combat numbers).
 * <p>OOP concept: constants are the only static globals in the project; everything mutable is an object.</p>
 */
public final class Constants {
    private Constants() { }

    public static final String TITLE = "Phantom: The Umbral Descent";
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    /** World units visible on screen (the game camera is zoomed in relative to the window). */
    public static final float VIEW_WIDTH = 960f;
    public static final float VIEW_HEIGHT = 540f;
    /** Virtual resolution for the UI camera. */
    public static final float UI_WIDTH = 1280f;
    public static final float UI_HEIGHT = 720f;

    public static final int TILE = 32;
    /** Fixed simulation step (60 Hz). */
    public static final float STEP = 1f / 60f;
    /** Longest frame we try to catch up on, prevents the "spiral of death". */
    public static final float MAX_FRAME_TIME = 0.25f;

    // ---- Player movement ----
    public static final float PLAYER_WIDTH = 22f;
    public static final float PLAYER_HEIGHT = 40f;
    public static final float GRAVITY = 2400f;
    public static final float MAX_FALL_SPEED = 1000f;
    public static final float RUN_SPEED = 330f;
    public static final float GROUND_ACCEL = 2800f;
    public static final float GROUND_FRICTION = 3400f;
    public static final float AIR_ACCEL = 2100f;
    public static final float JUMP_VELOCITY = 780f;
    /** Velocity multiplier applied when the jump key is released early (variable jump height). */
    public static final float JUMP_CUT = 0.42f;
    public static final float COYOTE_TIME = 0.10f;
    public static final float JUMP_BUFFER = 0.12f;
    public static final float DOUBLE_JUMP_VELOCITY = 700f;
    public static final float DASH_SPEED = 880f;
    public static final float DASH_TIME = 0.17f;
    public static final float DASH_COOLDOWN = 0.45f;
    public static final float WALL_SLIDE_SPEED = 140f;
    public static final float WALL_JUMP_X = 360f;
    public static final float WALL_JUMP_Y = 760f;
    public static final float WALL_JUMP_LOCK = 0.14f;
    public static final float POGO_VELOCITY = 720f;

    // ---- Combat ----
    public static final int PLAYER_BASE_MASKS = 5;
    public static final int NAIL_DAMAGE = 5;
    public static final int KEEN_EDGE_BONUS = 2;
    public static final float ATTACK_COOLDOWN = 0.34f;
    public static final float ATTACK_ACTIVE = 0.12f;
    public static final float PLAYER_IFRAMES = 1.2f;
    public static final float KNOCKBACK_X = 340f;
    public static final float KNOCKBACK_Y = 360f;
    public static final int SOUL_MAX = 99;
    public static final int SOUL_PER_HIT = 11;
    public static final int SOUL_COST = 33;
    public static final float FOCUS_TIME = 0.9f;
    public static final int BOLT_DAMAGE = 14;
    public static final float BOLT_SPEED = 820f;

    // ---- Feel ----
    public static final float HITSTOP_LIGHT = 0.045f;
    public static final float HITSTOP_HEAVY = 0.12f;
}
