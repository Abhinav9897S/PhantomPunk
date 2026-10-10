package phantom.ai;

import java.util.EnumMap;
import java.util.Map;

import phantom.world.WorldContext;

/**
 * A finite state machine: maps each {@link EnemyState} to an {@link EnemyBehaviour}
 * and tracks how long the current state has been active.
 * <p>OOP concept: <b>State pattern</b> with <b>generics</b> ({@code StateMachine<Crawler>}).</p>
 *
 * @param <E> the owner type
 */
public final class StateMachine<E> {
    private final E owner;
    private final Map<EnemyState, EnemyBehaviour<E>> behaviours = new EnumMap<>(EnemyState.class);
    private EnemyState current;
    private EnemyState previous;
    private EnemyState pending;
    private float timeInState;

    public StateMachine(E owner, EnemyState initial) {
        this.owner = owner;
        this.current = initial;
        this.previous = initial;
    }

    /** Registers the behaviour for a state (fluent). */
    public StateMachine<E> on(EnemyState state, EnemyBehaviour<E> behaviour) {
        behaviours.put(state, behaviour);
        return this;
    }

    /** Requests a state change; it takes effect immediately and runs the new state's enter hook on the next update. */
    public void change(EnemyState next) {
        previous = current;
        current = next;
        pending = next;
        timeInState = 0f;
    }

    public void update(WorldContext world, float dt) {
        EnemyBehaviour<E> behaviour = behaviours.get(current);
        if (pending != null) {
            pending = null;
            if (behaviour != null) behaviour.enter(owner, world);
        }
        timeInState += dt;
        if (behaviour != null) behaviour.update(owner, world, dt);
    }

    public EnemyState getState() { return current; }
    public EnemyState getPrevious() { return previous; }
    public boolean is(EnemyState state) { return current == state; }
    public float getTimeInState() { return timeInState; }
    public boolean hasBehaviour(EnemyState state) { return behaviours.containsKey(state); }
}
