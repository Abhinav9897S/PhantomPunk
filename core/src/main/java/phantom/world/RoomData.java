package phantom.world;

import java.util.List;

/**
 * Immutable description of a room as loaded from {@code assets/rooms/<id>.json}.
 * A fresh {@link TileMap} is created from it every time the room is entered.
 * <p>OOP concept: separation of <b>data</b> (this class) from <b>runtime state</b> ({@link Room}).</p>
 */
public final class RoomData {
    private final String id;
    private final String name;
    private final Region region;
    private final int mapX;
    private final int mapY;
    private final TileMap baseTiles;
    private final List<DoorData> doors;
    private final List<SpawnData> spawns;
    private final List<BreakableData> breakables;
    private final int[] start;

    public RoomData(String id, String name, Region region, int mapX, int mapY, TileMap baseTiles,
                    List<DoorData> doors, List<SpawnData> spawns, List<BreakableData> breakables, int[] start) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.mapX = mapX;
        this.mapY = mapY;
        this.baseTiles = baseTiles;
        this.doors = List.copyOf(doors);
        this.spawns = List.copyOf(spawns);
        this.breakables = List.copyOf(breakables);
        this.start = start == null ? null : start.clone();
    }

    public TileMap createTileMap() { return baseTiles.copy(); }
    public String getId() { return id; }
    public String getName() { return name; }
    public Region getRegion() { return region; }
    public int getMapX() { return mapX; }
    public int getMapY() { return mapY; }
    public int getCols() { return baseTiles.getCols(); }
    public int getRows() { return baseTiles.getRows(); }
    public List<DoorData> getDoors() { return doors; }
    public List<SpawnData> getSpawns() { return spawns; }
    public List<BreakableData> getBreakables() { return breakables; }
    public boolean hasStart() { return start != null; }
    public int getStartCol() { return start == null ? 1 : start[0]; }
    public int getStartRow() { return start == null ? 1 : start[1]; }

    public DoorData findDoor(String doorId) {
        for (DoorData d : doors) {
            if (d.id().equals(doorId)) return d;
        }
        return null;
    }

    public boolean hasSpawn(String type) {
        for (SpawnData s : spawns) {
            if (s.type().equals(type)) return true;
        }
        return false;
    }
}
