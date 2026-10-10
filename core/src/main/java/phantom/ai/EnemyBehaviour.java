package phantom.ai;

import phantom.world.WorldContext;

/**
 * What an enemy does while it is in one state.
 * <p>OOP concept: <b>State pattern</b> - each state's logic is a separate object (often a method
 * reference such as {@code Crawler::patrol}); <b>generics</b> let it receive the concrete enemy type.</p>
 *
 * @param <E> the enemy class this behaviour drives
 */
@FunctionalInterface
public interface EnemyBehaviour<E> {
    void update(E owner, WorldContext world, float dt);

    /** Called once when the state is entered. */
    default void enter(E owner, WorldContext world) { }
}
