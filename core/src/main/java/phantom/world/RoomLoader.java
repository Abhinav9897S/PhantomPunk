package phantom.world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;

import phantom.util.Direction;

/**
 * Builds {@link RoomData} objects from JSON room files.
 * Tile rows are written top-first in the file; markers such as {@code c} (crawler) or
 * legend characters (doors, NPCs) are turned into spawns and replaced by empty tiles.
 * <p>OOP concept: <b>Factory</b> - creates fully validated objects from data, throwing
 * {@link LevelFormatException} instead of crashing on bad files.</p>
 */
public final class RoomLoader {
    private static final Map<Character, String> DEFAULT_MARKERS = Map.of(
            'P', "start", 'K', "bench", 'c', "crawler", 'f', "flutterer",
            's', "sentinel", 'b', "brute", 'r', "rock", 'L', "light");

    private final JsonReader reader = new JsonReader();

    /** Loads {@code rooms/world.json} and every room it lists. */
    public WorldData loadWorld(RoomSource source) throws LevelFormatException {
        JsonValue index = parseJson(readText(source, "rooms/world.json"), "rooms/world.json");
        String start = index.getString("start", null);
        JsonValue list = index.get("rooms");
        if (start == null || list == null) {
            throw new LevelFormatException("rooms/world.json", "needs \"start\" and \"rooms\"");
        }
        Map<String, RoomData> rooms = new HashMap<>();
        for (JsonValue entry = list.child; entry != null; entry = entry.next) {
            String path = "rooms/" + entry.asString() + ".json";
            RoomData room = parse(readText(source, path), path);
            rooms.put(room.getId(), room);
        }
        if (!rooms.containsKey(start)) {
            throw new LevelFormatException("rooms/world.json", "start room '" + start + "' is not listed");
        }
        return new WorldData(rooms, start);
    }

    /** Parses one room file. */
    public RoomData parse(String text, String sourceName) throws LevelFormatException {
        JsonValue root = parseJson(text, sourceName);
        String id = require(root, "id", sourceName);
        String name = root.getString("name", id);
        Region region = parseRegion(require(root, "region", sourceName), sourceName);
        JsonValue map = root.get("map");
        int mapX = map != null && map.size >= 2 ? map.getInt(0) : 0;
        int mapY = map != null && map.size >= 2 ? map.getInt(1) : 0;
        Map<Character, JsonValue> legend = readLegend(root.get("legend"));

        JsonValue rowsJson = root.get("tiles");
        if (rowsJson == null || rowsJson.size == 0) throw new LevelFormatException(sourceName, "missing \"tiles\"");
        String[] lines = rowsJson.asStringArray();
        int cols = lines[0].length();
        int rows = lines.length;
        TileMap tiles = new TileMap(cols, rows);
        List<SpawnData> spawns = new ArrayList<>();
        List<DoorData> doors = new ArrayList<>();
        int[] start = null;

        for (int line = 0; line < rows; line++) {
            if (lines[line].length() != cols) {
                throw new LevelFormatException(sourceName, "row " + line + " has length " + lines[line].length()
                        + " but row 0 has " + cols);
            }
            int r = rows - 1 - line; // flip: file is top-first, world is bottom-first
            for (int c = 0; c < cols; c++) {
                char ch = lines[line].charAt(c);
                TileType tile = TileType.fromSymbol(ch);
                if (tile != null && !legend.containsKey(ch)) {
                    tiles.set(c, r, tile);
                    continue;
                }
                tiles.set(c, r, TileType.EMPTY);
                if (legend.containsKey(ch)) {
                    JsonValue def = legend.get(ch);
                    if ("door".equals(def.getString("type", ""))) {
                        doors.add(readDoor(def, c, r, cols, sourceName));
                    } else {
                        spawns.add(new SpawnData(def.getString("type", "?"), c, r, readProps(def)));
                    }
                } else if (DEFAULT_MARKERS.containsKey(ch)) {
                    String type = DEFAULT_MARKERS.get(ch);
                    if (type.equals("start")) start = new int[] {c, r};
                    else spawns.add(new SpawnData(type, c, r, Map.of()));
                } else {
                    throw new LevelFormatException(sourceName, "unknown symbol '" + ch + "' at row " + line + ", col " + c);
                }
            }
        }
        if (doors.isEmpty() && start == null) {
            throw new LevelFormatException(sourceName, "room has no doors and no start point");
        }
        return new RoomData(id, name, region, mapX, mapY, tiles, doors, spawns, findBreakables(id, tiles), start);
    }

    private DoorData readDoor(JsonValue def, int c, int r, int cols, String src) throws LevelFormatException {
        Direction side;
        if (c == 0) side = Direction.LEFT;
        else if (c == cols - 1) side = Direction.RIGHT;
        else throw new LevelFormatException(src, "door '" + def.getString("id", "?") + "' must be on the left or right edge");
        String id = def.getString("id", null);
        String to = def.getString("to", null);
        String toDoor = def.getString("toDoor", null);
        if (id == null || to == null || toDoor == null) {
            throw new LevelFormatException(src, "doors need id, to and toDoor");
        }
        return new DoorData(id, side, c, r, to, toDoor);
    }

    /** Groups connected breakable tiles into walls (flood fill). */
    private List<BreakableData> findBreakables(String roomId, TileMap tiles) {
        List<BreakableData> result = new ArrayList<>();
        boolean[] seen = new boolean[tiles.getCols() * tiles.getRows()];
        for (int r = 0; r < tiles.getRows(); r++) {
            for (int c = 0; c < tiles.getCols(); c++) {
                if (tiles.get(c, r) != TileType.BREAKABLE || seen[r * tiles.getCols() + c]) continue;
                int[] box = {c, r, c, r};
                flood(tiles, c, r, seen, box);
                result.add(new BreakableData(roomId + ":wall:" + box[0] + ":" + box[1],
                        box[0], box[1], box[2] - box[0] + 1, box[3] - box[1] + 1));
            }
        }
        return result;
    }

    private void flood(TileMap tiles, int c0, int r0, boolean[] seen, int[] box) {
        java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
        stack.push(new int[] {c0, r0});
        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int c = p[0];
            int r = p[1];
            if (!tiles.inBounds(c, r) || tiles.get(c, r) != TileType.BREAKABLE || seen[r * tiles.getCols() + c]) continue;
            seen[r * tiles.getCols() + c] = true;
            box[0] = Math.min(box[0], c);
            box[1] = Math.min(box[1], r);
            box[2] = Math.max(box[2], c);
            box[3] = Math.max(box[3], r);
            stack.push(new int[] {c + 1, r});
            stack.push(new int[] {c - 1, r});
            stack.push(new int[] {c, r + 1});
            stack.push(new int[] {c, r - 1});
        }
    }

    private Map<Character, JsonValue> readLegend(JsonValue legend) {
        Map<Character, JsonValue> map = new HashMap<>();
        if (legend == null) return map;
        for (JsonValue e = legend.child; e != null; e = e.next) {
            if (e.name != null && e.name.length() == 1) map.put(e.name.charAt(0), e);
        }
        return map;
    }

    private Map<String, String> readProps(JsonValue def) {
        Map<String, String> props = new HashMap<>();
        for (JsonValue e = def.child; e != null; e = e.next) {
            if (!"type".equals(e.name)) props.put(e.name, e.asString());
        }
        return props;
    }

    private Region parseRegion(String value, String src) throws LevelFormatException {
        try {
            return Region.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new LevelFormatException(src, "unknown region '" + value + "'", e);
        }
    }

    private String require(JsonValue root, String key, String src) throws LevelFormatException {
        String v = root.getString(key, null);
        if (v == null || v.isEmpty()) throw new LevelFormatException(src, "missing \"" + key + "\"");
        return v;
    }

    private JsonValue parseJson(String text, String src) throws LevelFormatException {
        try {
            JsonValue v = reader.parse(text);
            if (v == null || !v.isObject()) throw new LevelFormatException(src, "top level must be a JSON object");
            return v;
        } catch (SerializationException e) {
            throw new LevelFormatException(src, "invalid JSON: " + e.getMessage(), e);
        }
    }

    private String readText(RoomSource source, String path) throws LevelFormatException {
        try {
            return source.read(path);
        } catch (IOException e) {
            throw new LevelFormatException(path, "cannot read file (" + e.getMessage() + ")", e);
        }
    }
}
