package phantom.world;

import phantom.util.Constants;
import phantom.util.Direction;

/**
 * A door on the left or right edge of a room, linked to a door in another room.
 * The door occupies {@link #HEIGHT} tiles starting at {@code row} (its bottom cell).
 * <p>OOP concept: immutable <b>record</b>.</p>
 */
public record DoorData(String id, Direction side, int col, int row, String targetRoom, String targetDoor) {
    public static final int HEIGHT = 3;

    /** Feet position (tile coords) where a player arriving through this door is placed. */
    public int arrivalCol() {
        return side == Direction.LEFT ? col + 1 : col - 1;
    }

    /** World x of the arrival point (left edge of a player-sized box). */
    public float arrivalX(float playerWidth) {
        int t = Constants.TILE;
        return side == Direction.LEFT ? (col + 1) * t + 6 : col * t - playerWidth - 6;
    }

    public float arrivalY() {
        return row * Constants.TILE;
    }
}
