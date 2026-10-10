package phantom.entities.bosses;

import phantom.world.WorldContext;

/**
 * One telegraphed boss attack. Bosses hold a list of these per phase and pick one at a time.
 * <p>OOP concept: <b>Strategy pattern</b> - attacks are interchangeable objects, so the same
 * {@code ChargeAttack} class serves two different bosses with different numbers.</p>
 */
public interface BossAttack {
    /** Called when the attack is chosen. */
    void begin(Boss boss, WorldContext world);

    /** Advances the attack; returns true when it has finished. */
    boolean update(Boss boss, WorldContext world, float dt);

    /** Pose name the boss should be drawn in (e.g. "windup", "charge", "cast"). */
    String getPose();

    /** True during the warning part of the attack, so the boss can glow. */
    default boolean isTelegraphing() {
        return false;
    }
}
