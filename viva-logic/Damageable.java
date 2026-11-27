/**
 * Anything that can be hit: the player and every enemy.
 *
 * OOP: INTERFACE (100% abstraction) - only WHAT can be done, not HOW.
 *      The Game can hit any Damageable without knowing if it is a Crawler or a Boss.
 * Java 8 "default" method: a ready-made body that a class may override.
 */
public interface Damageable {
    /** Applies the damage; returns true if it actually hurt. */
    boolean takeDamage(int amount);

    boolean isAlive();

    /** Hitting this gives the player soul (Light). Default: yes. */
    default boolean givesSoul() {
        return true;
    }
}
