package phantom.world;

import java.util.Map;

/**
 * One object placed in a room file: its type, tile position and extra properties.
 * <p>OOP concept: immutable record consumed by the factories.</p>
 */
public record SpawnData(String type, int col, int row, Map<String, String> props) {
    public SpawnData {
        props = Map.copyOf(props);
    }

    public String prop(String key, String fallback) {
        return props.getOrDefault(key, fallback);
    }

    public String id(String roomId) {
        return prop("id", roomId + ":" + type + ":" + col + ":" + row);
    }
}
