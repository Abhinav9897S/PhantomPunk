/**
 * Things that happen in the game, announced through the EventBus.
 *
 * OOP: RECORDS (Java 16+) - small immutable data classes. The compiler writes the
 *      constructor, getters, equals() and toString() for us.
 * Each record implements the GameEvent interface, so all of them can travel on one bus.
 */
public interface GameEvent {
    record EnemyKilled(String enemyName, int shards) implements GameEvent { }

    record BossDefeated(String bossName, boolean finalBoss) implements GameEvent { }

    record AbilityUnlocked(AbilityType ability) implements GameEvent { }

    record BenchRested(String roomName) implements GameEvent { }

    record PlayerDied(String roomName) implements GameEvent { }
}
