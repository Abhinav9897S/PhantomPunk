package phantom.entities;

import phantom.components.HealthComponent;
import phantom.world.WorldContext;

/**
 * A living {@link Entity} with health, hit flash and knockback: the base of the player and enemies.
 * <p>OOP concept: <b>inheritance</b> (shared combat behaviour) + <b>composition</b>
 * ({@link HealthComponent}) + implements {@link Damageable}.</p>
 */
public abstract class Character extends Entity implements Damageable {
    private final HealthComponent health;
    private float flashTimer;

    protected Character(float x, float y, float width, float height, int maxHealth) {
        super(x, y, width, height);
        health = new HealthComponent(maxHealth);
    }

    /** Shared hit handling: health, flash and knockback. Subclasses add their own reactions. */
    protected boolean applyHit(DamageInfo info, float iframes, float knockX, float knockY) {
        if (!health.damage(info.amount(), iframes)) return false;
        flashTimer = 0.12f;
        float dir = info.awayFrom(centerX());
        if (knockX > 0) setVelocityX(dir * knockX);
        if (knockY > 0) setVelocityY(Math.max(getVelocityY(), knockY));
        return true;
    }

    /** Ticks health timers and the hit flash. */
    protected void updateCharacter(float dt) {
        health.update(dt);
        if (flashTimer > 0) flashTimer -= dt;
    }

    @Override
    public boolean isAlive() {
        return !health.isDead();
    }

    @Override
    public abstract boolean takeDamage(DamageInfo info, WorldContext world);

    public HealthComponent getHealth() { return health; }

    /** 0..1 white flash strength for rendering. */
    public float getFlashAmount() {
        return Math.max(0f, flashTimer / 0.12f);
    }
}
