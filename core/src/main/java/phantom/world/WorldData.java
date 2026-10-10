package phantom.world;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * All rooms of the game, looked up by id, plus the starting room.
 * <p>OOP concept: {@link java.util.HashMap} for O(1) room lookup by id.</p>
 */
public final class WorldData {
    private final Map<String, RoomData> rooms;
    private final String startRoom;

    public WorldData(Map<String, RoomData> rooms, String startRoom) {
        this.rooms = rooms;
        this.startRoom = startRoom;
    }

    public RoomData get(String id) { return rooms.get(id); }
    public boolean has(String id) { return rooms.containsKey(id); }
    public Collection<RoomData> all() { return Collections.unmodifiableCollection(rooms.values()); }
    public String getStartRoom() { return startRoom; }
    public int size() { return rooms.size(); }
}
