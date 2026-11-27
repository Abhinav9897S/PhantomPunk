import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Awards achievements by LISTENING to events (an Observer). The enemy and boss classes
 * have no idea this class exists.
 * OOP: COLLECTIONS - LinkedHashSet: no duplicates, keeps insertion order.
 */
public class Achievements {
    private final Set<String> unlocked = new LinkedHashSet<>();
    private int kills;

    public Achievements(EventBus bus, Game game) {
        bus.subscribe(GameEvent.EnemyKilled.class, e -> {
            kills++;
            if (kills == 10) award("Lantern Bearer - 10 enemies defeated", game);
        });
        bus.subscribe(GameEvent.BossDefeated.class, e -> award("Guardian-breaker - defeated " + e.bossName(), game));
    }

    private void award(String title, Game game) {
        if (unlocked.add(title)) game.log("*** Achievement: " + title + " ***");
    }

    public Set<String> getUnlocked() {
        return Set.copyOf(unlocked);
    }
}
