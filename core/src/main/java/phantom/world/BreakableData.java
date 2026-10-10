package phantom.world;

/**
 * A rectangle of breakable tiles found in a room file (tile coords, bottom-left origin).
 * <p>OOP concept: immutable record.</p>
 */
public record BreakableData(String id, int col, int row, int width, int height) {
}
