/**
 * Health points of a character.
 *
 * OOP: ENCAPSULATION - the fields are private. Nobody can write "health = -5".
 *      Health changes ONLY through damage() and heal(), which enforce the rules:
 *      never below 0, never above max.
 * OOP: COMPOSITION - a Character HAS-A HealthComponent (it does not inherit from it).
 */
public class HealthComponent {
    private final int max;
    private int current;

    public HealthComponent(int max) {
        this.max = max;
        this.current = max;
    }

    /** Returns true if the damage was applied. */
    public boolean damage(int amount) {
        if (amount <= 0 || isDead()) return false;
        current = Math.max(0, current - amount);
        return true;
    }

    public void heal(int amount) {
        if (isDead()) return;
        current = Math.min(max, current + amount);
    }

    public void healFull() {
        current = max;
    }

    // read-only access (getters only)
    public int getCurrent() { return current; }
    public int getMax() { return max; }
    public boolean isDead() { return current <= 0; }
    public boolean isFull() { return current >= max; }
}
