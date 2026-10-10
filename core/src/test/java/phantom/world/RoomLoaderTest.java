package phantom.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import phantom.util.Direction;

/** The room factory turns text into rooms and rejects broken files with clear messages. */
class RoomLoaderTest {
    private final RoomLoader loader = new RoomLoader();

    @Test
    void parsesTilesMarkersAndDoors() throws LevelFormatException {
        String json = "{\"id\":\"a\",\"region\":\"GILDED_CISTERN\",\"legend\":{\"E\":{\"type\":\"door\",\"id\":\"e\",\"to\":\"b\",\"toDoor\":\"w\"}},"
                + "\"tiles\":[\"#####\",\"#..c.\",\"#P=.E\",\"#####\"]}";
        RoomData room = loader.parse(json, "a.json");
        assertEquals(Region.GILDED_CISTERN, room.getRegion());
        assertEquals(5, room.getCols());
        assertEquals(4, room.getRows());
        assertEquals(TileType.ONE_WAY, room.createTileMap().get(2, 1), "rows are flipped: file top is world top");
        assertEquals(1, room.getStartCol());
        assertEquals(1, room.getStartRow());
        assertTrue(room.hasSpawn("crawler"));
        DoorData door = room.findDoor("e");
        assertEquals(Direction.RIGHT, door.side());
        assertEquals(1, door.row());
    }

    @Test
    void groupsBreakableTiles() throws LevelFormatException {
        RoomData room = loader.parse("{\"id\":\"a\",\"region\":\"UMBRAL_HEART\",\"tiles\":[\"#####\",\"#PBB#\",\"#.BB#\",\"#####\"]}", "a");
        assertEquals(1, room.getBreakables().size());
        BreakableData b = room.getBreakables().get(0);
        assertEquals(2, b.width());
        assertEquals(2, b.height());
    }

    @Test
    void rejectsInvalidJson() {
        LevelFormatException e = assertThrows(LevelFormatException.class, () -> loader.parse("{ nope", "bad.json"));
        assertTrue(e.getMessage().startsWith("bad.json"));
    }

    @Test
    void rejectsRaggedRows() {
        assertThrows(LevelFormatException.class, () -> loader.parse(
                "{\"id\":\"a\",\"region\":\"UMBRAL_HEART\",\"tiles\":[\"####\",\"#P.\",\"####\"]}", "a"));
    }

    @Test
    void rejectsUnknownSymbolsAndRegions() {
        assertThrows(LevelFormatException.class, () -> loader.parse(
                "{\"id\":\"a\",\"region\":\"UMBRAL_HEART\",\"tiles\":[\"###\",\"#P?\",\"###\"]}", "a"));
        assertThrows(LevelFormatException.class, () -> loader.parse(
                "{\"id\":\"a\",\"region\":\"MOON\",\"tiles\":[\"###\",\"#P#\",\"###\"]}", "a"));
    }

    @Test
    void rejectsDoorsAwayFromTheEdge() {
        assertThrows(LevelFormatException.class, () -> loader.parse(
                "{\"id\":\"a\",\"region\":\"UMBRAL_HEART\",\"legend\":{\"D\":{\"type\":\"door\",\"id\":\"d\",\"to\":\"b\",\"toDoor\":\"c\"}},"
                        + "\"tiles\":[\"#####\",\"#PD.#\",\"#####\"]}", "a"));
    }

    @Test
    void missingWorldFileIsALevelFormatException() {
        assertThrows(LevelFormatException.class, () -> loader.loadWorld(path -> {
            throw new java.io.IOException("not here");
        }));
    }
}
