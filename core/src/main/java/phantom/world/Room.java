package phantom.world;

import phantom.abilities.AbilityType;
import phantom.entities.bosses.Boss;
import phantom.entities.enemies.Enemy;
import phantom.entities.props.AbilityShrine;
import phantom.entities.props.Bench;
import phantom.entities.props.BreakableWall;
import phantom.entities.props.DecorLight;
import phantom.entities.props.FallingRock;
import phantom.entities.props.LoreNpc;
import phantom.entities.props.RelicPickup;
import phantom.entities.props.ShardCache;
import phantom.entities.props.Shopkeeper;
import phantom.util.Constants;

/**
 * The runtime instance of a room: its own copy of the tile map plus the objects spawned into it.
 * Already-collected items, broken walls and defeated bosses are skipped using {@link GameProgress}.
 * <p>OOP concept: composition (has a {@link TileMap} and {@link RoomData}); uses the {@link EnemyFactory}.</p>
 */
public final class Room {
    private final RoomData data;
    private final TileMap tiles;

    public Room(RoomData data, GameProgress progress) {
        this.data = data;
        this.tiles = data.createTileMap();
        for (BreakableData b : data.getBreakables()) {
            if (progress.isWallBroken(b.id())) clear(b);
        }
    }

    private void clear(BreakableData b) {
        for (int r = b.row(); r < b.row() + b.height(); r++) {
            for (int c = b.col(); c < b.col() + b.width(); c++) {
                if (tiles.get(c, r) == TileType.BREAKABLE) tiles.set(c, r, TileType.EMPTY);
            }
        }
    }

    /** Spawns this room's objects; returns the boss arena if an undefeated boss lives here. */
    public BossArena populate(EntityManager entities, EnemyFactory factory, GameProgress progress) {
        BossArena arena = null;
        for (BreakableData b : data.getBreakables()) {
            if (!progress.isWallBroken(b.id())) entities.add(new BreakableWall(b));
        }
        for (SpawnData s : data.getSpawns()) {
            float x = (s.col() + 0.5f) * Constants.TILE;
            float y = s.row() * Constants.TILE;
            String id = s.id(data.getId());
            switch (s.type()) {
                case "bench": entities.add(new Bench(x, y, id)); break;
                case "npc": entities.add(new LoreNpc(x, y, s.prop("id", "ossian"))); break;
                case "shop": entities.add(new Shopkeeper(x, y, s.prop("id", "quill"))); break;
                case "light": entities.add(new DecorLight(x, y, data.getRegion())); break;
                case "rock": entities.add(FallingRock.trap(tiles, s.col(), s.row())); break;
                case "shrine":
                    entities.add(new AbilityShrine(x, y, parseAbility(s.prop("ability", "LIGHT_BOLT")), id,
                            progress.isCollected(id)));
                    break;
                case "relic":
                    if (!progress.isCollected(id)) entities.add(new RelicPickup(x, y, id));
                    break;
                case "cache":
                    if (!progress.isCollected(id)) entities.add(new ShardCache(x, y, id, intProp(s, "value", 30)));
                    break;
                default:
                    arena = spawnEnemy(s, x, y, entities, factory, progress, arena);
                    break;
            }
        }
        return arena;
    }

    private BossArena spawnEnemy(SpawnData s, float x, float y, EntityManager entities, EnemyFactory factory,
                                 GameProgress progress, BossArena arena) {
        if (s.type().equals("boss") && progress.isBossDefeated(s.prop("boss", ""))) return arena;
        try {
            Enemy enemy = factory.create(s, x, y);
            entities.add(enemy);
            if (enemy instanceof Boss boss) {
                String reward = s.prop("grants", "");
                return new BossArena(boss, reward.isEmpty() ? null : parseAbility(reward));
            }
        } catch (LevelFormatException e) {
            // Validated at load time; if a bad spawn slips through, skip it instead of crashing.
            System.err.println("[Room " + data.getId() + "] " + e.getMessage());
        }
        return arena;
    }

    private static AbilityType parseAbility(String name) {
        try {
            return AbilityType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AbilityType.LIGHT_BOLT;
        }
    }

    private static int intProp(SpawnData s, String key, int fallback) {
        try {
            return Integer.parseInt(s.prop(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public RoomData getData() { return data; }
    public TileMap getTileMap() { return tiles; }
    public String getId() { return data.getId(); }
    public Region getRegion() { return data.getRegion(); }
}
