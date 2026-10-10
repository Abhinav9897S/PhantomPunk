package phantom.components;

/**
 * Health points plus invincibility frames.
 * <p>OOP concept: <b>composition</b> (characters "have a" health component) and
 * <b>encapsulation</b> (no public health field; use {@link #damage(int, float)}).</p>
 */
public final class HealthComponent {
    private int max;
    private int current;
    private float invincibleTimer;

    public HealthComponent(int max) {
        this.max = Math.max(1, max);
        this.current = this.max;
    }

    /** Applies damage unless invincible or already dead. Returns true if it was applied. */
    public boolean damage(int amount, float invincibility) {
        if (amount <= 0 || isDead() || isInvincible()) return false;
        current = Math.max(0, current - amount);
        invincibleTimer = invincibility;
        return true;
    }

    public void heal(int amount) {
        if (isDead()) return;
        current = Math.min(max, current + Math.max(0, amount));
    }

    public void healFull() {
        current = max;
    }

    public void setMax(int newMax, boolean refill) {
        max = Math.max(1, newMax);
        current = refill ? max : Math.min(current, max);
    }

    public void makeInvincible(float seconds) {
        invincibleTimer = Math.max(invincibleTimer, seconds);
    }

    public void update(float dt) {
        if (invincibleTimer > 0) invincibleTimer -= dt;
    }

    public boolean isDead() { return current <= 0; }
    public boolean isInvincible() { return invincibleTimer > 0; }
    public float getInvincibleTime() { return Math.max(0f, invincibleTimer); }
    public int getCurrent() { return current; }
    public int getMax() { return max; }
    public boolean isFull() { return current >= max; }

    public float fraction() {
        return current / (float) max;
    }
}
