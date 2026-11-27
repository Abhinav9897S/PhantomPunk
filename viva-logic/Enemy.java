/**
 * Base class of every enemy and boss.
 *
 * OOP: ABSTRACT CLASS + INHERITANCE - Enemy IS-A Character.
 * OOP: TEMPLATE METHOD - takeTurn() fixes the steps (skip if dead, run the state machine,
 *      hit the player when attacking). Subclasses only fill in nextState(), the "hook".
 * OOP: PROTECTED - hooks are visible to subclasses but not to the rest of the program.
 */
public abstract class Enemy extends Character {
    private final int contactDamage;
    private final int shardReward;
    protected EnemyState state = EnemyState.PATROL;

    protected Enemy(String name, int health, int contactDamage, int shardReward) {
        super(name, health);
        this.contactDamage = contactDamage;
        this.shardReward = shardReward;
    }

    /** Decides the next state. Each enemy type has its own behaviour (overridden). */
    protected abstract EnemyState nextState();

    /** TEMPLATE METHOD: the same steps for every enemy. */
    public final String takeTurn(Player player) {
        if (!isAlive()) return null;
        state = nextState();
        if (state == EnemyState.ATTACK) {
            boolean hit = player.takeDamage(damageFor(player));
            return hit ? getName() + " hits you for " + damageFor(player) + "!"
                       : getName() + " attacks, but you are not there!";
        }
        if (state == EnemyState.WINDUP) return getName() + " " + windupText();
        return null;
    }

    /** How much this attack does. Bosses override this for their stronger phases. */
    protected int damageFor(Player player) {
        return contactDamage;
    }

    /** Warning text before an attack. Overridable. */
    protected String windupText() {
        return "gets ready to attack...";
    }

    /** Overriding a method from Character: getting hit stuns the enemy. */
    @Override
    public boolean takeDamage(int amount) {
        boolean hurt = super.takeDamage(amount);
        if (hurt && isAlive()) onHurt();
        return hurt;
    }

    /** Hook with a default body; subclasses may override it. */
    protected void onHurt() {
        state = EnemyState.STUNNED;
    }

    /** True when the enemy will attack on its next turn (the player can dash away). */
    public boolean isAboutToAttack() {
        return state == EnemyState.WINDUP;
    }

    /** Heals back to full (used when the player dies in this room). */
    public void reset() {
        getHealth().healFull();
        state = EnemyState.PATROL;
    }

    public int getShardReward() { return shardReward; }

    @Override
    public String describe() {
        return getName() + " (" + getHealth().getCurrent() + "/" + getHealth().getMax() + " hp, " + state + ")";
    }
}
