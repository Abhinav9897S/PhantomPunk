/**
 * Mosscrawler: a weak enemy that alternates between patrolling and biting.
 * OOP: INHERITANCE (extends Enemy), super(...) constructor call, @Override.
 */
public final class Crawler extends Enemy {
    public Crawler() {
        super("Mosscrawler", 15, 1, 4);   // name, health, damage, shards
    }

    @Override
    protected EnemyState nextState() {
        // STUNNED or ATTACK -> PATROL, PATROL -> ATTACK
        return state == EnemyState.PATROL ? EnemyState.ATTACK : EnemyState.PATROL;
    }
}
