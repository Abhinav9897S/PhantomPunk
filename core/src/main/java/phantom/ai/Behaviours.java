package phantom.ai;

import phantom.entities.enemies.Enemy;
import phantom.util.GameMath;
import phantom.world.WorldContext;

/**
 * Reusable behaviours shared by several enemy types, so the same logic is not duplicated.
 * <p>OOP concept: generic static factory methods returning {@link EnemyBehaviour} objects.</p>
 */
public final class Behaviours {
    private Behaviours() { }

    /** Slides to a halt after a hit, then switches to {@code next} after {@code duration}. */
    public static <E extends Enemy> EnemyBehaviour<E> stunned(float duration, EnemyState next) {
        return (enemy, world, dt) -> {
            enemy.setVelocityX(GameMath.approach(enemy.getVelocityX(), 0f, 900f * dt));
            enemy.stepPhysics(world, dt);
            if (enemy.getAi().getTimeInState() >= duration) enemy.getAi().change(next);
        };
    }

    /** Waits in place (with gravity) until {@code duration} has passed. */
    public static <E extends Enemy> EnemyBehaviour<E> pause(float duration, EnemyState next) {
        return (enemy, world, dt) -> {
            enemy.setVelocityX(GameMath.approach(enemy.getVelocityX(), 0f, 1200f * dt));
            enemy.stepPhysics(world, dt);
            if (enemy.getAi().getTimeInState() >= duration) enemy.getAi().change(next);
        };
    }

    /** Distance from the enemy's centre to the player's centre, or infinity if there is no player. */
    public static float distanceToPlayer(Enemy enemy, WorldContext world) {
        if (world.getPlayer() == null || world.getPlayer().isDead()) return Float.MAX_VALUE;
        float dx = world.getPlayer().centerX() - enemy.centerX();
        float dy = world.getPlayer().centerY() - enemy.centerY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** True if the player is within range and not hidden behind walls. */
    public static boolean canSeePlayer(Enemy enemy, WorldContext world, float range) {
        if (distanceToPlayer(enemy, world) > range) return false;
        return world.getTileMap().hasLineOfSight(enemy.centerX(), enemy.centerY(),
                world.getPlayer().centerX(), world.getPlayer().centerY());
    }
}
