/**
 * Lantern Sentinel: charges up (WINDUP) for one turn, then fires.
 * OOP: same parent, different behaviour = POLYMORPHISM through overriding.
 */
public final class Sentinel extends Enemy {
    public Sentinel() {
        super("Lantern Sentinel", 20, 1, 6);
    }

    @Override
    protected EnemyState nextState() {
        return state == EnemyState.WINDUP ? EnemyState.ATTACK : EnemyState.WINDUP;
    }

    @Override
    protected String windupText() {
        return "glows brighter - it is about to fire!";
    }

    /** A rooted totem is never stunned: this override replaces the default hook. */
    @Override
    protected void onHurt() {
        // stays in its current state
    }
}
