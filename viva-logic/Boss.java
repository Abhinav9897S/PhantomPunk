/**
 * A guardian with two phases. Below half health it enters phase 2 and hits harder.
 *
 * OOP: ABSTRACT CLASS below another abstract class (Enemy) - a multilevel hierarchy:
 *      GameObject -> Character -> Enemy -> Boss -> Gnarlwarden.
 * Subclasses must give their reward and their attack names (abstract methods).
 */
public abstract class Boss extends Enemy {
    private int phase = 1;

    protected Boss(String name, int health, int shardReward) {
        super(name, health, 1, shardReward);
    }

    /** The ability given to the player when this boss falls (null = none). */
    public abstract AbilityType getReward();

    /** Name of the big attack in a phase, e.g. "charges across the den". */
    protected abstract String attackName(int phase);

    /** True only for the last boss: beating it ends the game. */
    public boolean isFinal() {
        return false;
    }

    /** Bosses always telegraph: WINDUP, then ATTACK, and they are never stunned. */
    @Override
    protected EnemyState nextState() {
        return state == EnemyState.WINDUP ? EnemyState.ATTACK : EnemyState.WINDUP;
    }

    @Override
    protected void onHurt() {
        if (phase == 1 && getHealth().getCurrent() * 2 <= getHealth().getMax()) phase = 2;
    }

    @Override
    protected int damageFor(Player player) {
        return phase == 1 ? 1 : 2;
    }

    @Override
    protected String windupText() {
        return attackName(phase) + "!";
    }

    /** Overrides reset() and still runs the parent's version with super.reset(). */
    @Override
    public void reset() {
        super.reset();
        phase = 1;
    }

    public int getPhase() { return phase; }

    @Override
    public String describe() {
        return "[BOSS] " + super.describe() + " phase " + phase;
    }
}
