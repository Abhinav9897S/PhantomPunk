package phantom.entities;

import com.badlogic.gdx.graphics.Color;

/**
 * Projectile varieties with their size, colour and special behaviour flags.
 * <p>OOP concept: enum with data, so one {@link Projectile} class covers every kind.</p>
 */
public enum ProjectileKind {
    LIGHT_BOLT(30, 16, new Color(0.85f, 0.95f, 1f, 1f), true, false, 1.2f),
    ORB(14, 14, new Color(1f, 0.62f, 0.3f, 1f), false, false, 3.5f),
    GEAR(22, 22, new Color(1f, 0.78f, 0.38f, 1f), false, false, 4f),
    WISP(14, 14, new Color(0.78f, 0.6f, 1f, 1f), false, true, 4.5f),
    SHOCKWAVE(26, 30, new Color(1f, 0.85f, 0.6f, 1f), false, false, 1.6f),
    VOID_ORB(16, 16, new Color(0.7f, 0.5f, 1f, 1f), false, false, 4f);

    private final float width;
    private final float height;
    private final Color color;
    private final boolean piercing;
    private final boolean homing;
    private final float lifetime;

    ProjectileKind(float width, float height, Color color, boolean piercing, boolean homing, float lifetime) {
        this.width = width;
        this.height = height;
        this.color = color;
        this.piercing = piercing;
        this.homing = homing;
        this.lifetime = lifetime;
    }

    public float getWidth() { return width; }
    public float getHeight() { return height; }
    /** Shared colour; never modify it. */
    public Color getColor() { return color; }
    public boolean isPiercing() { return piercing; }
    public boolean isHoming() { return homing; }
    public float getLifetime() { return lifetime; }
}
