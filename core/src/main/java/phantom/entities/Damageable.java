package phantom.entities;

import phantom.world.WorldContext;

/**
 * Something that can be hit: the player, enemies, breakable walls, shard caches.
 * <p>OOP concept: <b>interface polymorphism</b> - the combat system hits any {@code Damageable}
 * without knowing whether it is a beetle or a wall.</p>
 */
public interface Damageable extends Collidable {
    /** Applies the hit. Returns true if it had an effect (not invincible, not already dead). */
    boolean takeDamage(DamageInfo info, WorldContext world);

    boolean isAlive();

    Team getTeam();

    /** Whether striking this gives the player Light energy. */
    default boolean givesSoul() {
        return true;
    }

    /** Whether a down-slash on this bounces the player upwards. */
    default boolean isPogoTarget() {
        return true;
    }
}
