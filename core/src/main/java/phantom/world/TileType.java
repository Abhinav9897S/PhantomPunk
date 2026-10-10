package phantom.world;

/**
 * Kinds of map tile and the character used for each in room files.
 * <p>OOP concept: enum with behaviour ({@link #isSolid()}, {@link #isHazard()}).</p>
 */
public enum TileType {
    EMPTY('.'),
    SOLID('#'),
    /** Can be jumped through from below and stood on from above. */
    ONE_WAY('='),
    SPIKES('^'),
    /** Solid until struck enough times (see {@code BreakableWall}). */
    BREAKABLE('B'),
    /** Boss-arena gate: open normally, solid while an arena is locked. */
    GATE('g'),
    WATER('~'),
    /** Pool of Hollow Mist; hurts like spikes. */
    MIST('m');

    private final char symbol;

    TileType(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() { return symbol; }

    /** Solid regardless of arena state. Gates are handled by {@link TileMap}. */
    public boolean isSolid() {
        return this == SOLID || this == BREAKABLE;
    }

    public boolean isHazard() {
        return this == SPIKES || this == MIST;
    }

    /** Returns the tile for a room-file character, or {@code null} if it is not a tile symbol. */
    public static TileType fromSymbol(char c) {
        for (TileType t : values()) {
            if (t.symbol == c) return t;
        }
        return null;
    }
}
