package phantom.abilities;

import phantom.entities.player.Player;
import phantom.input.InputSource;
import phantom.world.WorldContext;

/**
 * One player ability. The player owns a set of these and calls the hooks each tick;
 * each ability decides for itself what to do.
 * <p>OOP concept: <b>interface + Strategy pattern</b> - Dash, WallCling, DoubleJump, LightBolt and
 * Focus are interchangeable objects, and default methods keep each one small.</p>
 */
public interface Ability {
    AbilityType getType();

    /** Called every tick while the ability is unlocked. */
    void update(Player player, InputSource input, WorldContext world, float dt);

    /** Called when Jump is pressed in mid-air. Return true to consume the press. */
    default boolean onJumpPressed(Player player, WorldContext world) {
        return false;
    }

    /** Called on landing, pogo or wall jump: restore air moves. */
    default void refresh(Player player) { }

    /** Called when the player takes damage. */
    default void onDamaged(Player player) { }

    /** True while the ability fully controls movement (gravity and running are suspended). */
    default boolean controlsMovement() {
        return false;
    }

    /** True while gravity should be switched off (only the dash; Focus keeps you grounded). */
    default boolean suspendsGravity() {
        return false;
    }

    /** Lets an ability limit the fall speed (wall sliding). */
    default float modifyMaxFall(float normal) {
        return normal;
    }

    /** Clears transient state, e.g. after a room change. */
    default void reset() { }
}
