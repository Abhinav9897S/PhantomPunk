package phantom.world;

import java.util.Set;

import phantom.entities.bosses.Gnarlwarden;
import phantom.entities.bosses.GildedWarden;
import phantom.entities.bosses.HollowHeart;
import phantom.entities.enemies.Brute;
import phantom.entities.enemies.Crawler;
import phantom.entities.enemies.Enemy;
import phantom.entities.enemies.Flutterer;
import phantom.entities.enemies.Sentinel;

/**
 * Creates enemies and bosses from room-file spawn entries.
 * <p>OOP concept: <b>Factory pattern</b> - callers ask for "crawler" and get an {@link Enemy};
 * they never use the concrete constructors. Unknown names raise {@link LevelFormatException}.</p>
 */
public final class EnemyFactory {
    private static final Set<String> ENEMIES = Set.of("crawler", "flutterer", "sentinel", "brute", "boss");
    private static final Set<String> BOSSES = Set.of("gnarlwarden", "gilded_warden", "hollow_heart");

    public static boolean isEnemyType(String type) {
        return ENEMIES.contains(type);
    }

    public static boolean isBossId(String id) {
        return BOSSES.contains(id);
    }

    /**
     * @param x horizontal centre of the spawn cell
     * @param y bottom of the spawn cell
     */
    public Enemy create(SpawnData spawn, float x, float y) throws LevelFormatException {
        switch (spawn.type()) {
            case "crawler": return new Crawler(x - 15f, y);
            case "flutterer": return new Flutterer(x - 13f, y);
            case "sentinel": return new Sentinel(x - 15f, y);
            case "brute": return new Brute(x - 26f, y);
            case "boss": return createBoss(spawn.prop("boss", "?"), x, y);
            default: throw new LevelFormatException("spawn", "unknown enemy type '" + spawn.type() + "'");
        }
    }

    private Enemy createBoss(String id, float x, float y) throws LevelFormatException {
        switch (id) {
            case "gnarlwarden": return new Gnarlwarden(x, y);
            case "gilded_warden": return new GildedWarden(x, y);
            case "hollow_heart": return new HollowHeart(x, y);
            default: throw new LevelFormatException("spawn", "unknown boss '" + id + "'");
        }
    }
}
