package phantom.world;

import phantom.components.Hitbox;
import phantom.util.Constants;

/**
 * Grid of tiles for one room. Row 0 is the bottom row (y-up, like the world).
 * Out-of-bounds cells count as solid so nothing can leave the room except through doors.
 * <p>OOP concept: encapsulation - the grid array is private; callers ask questions such as
 * {@link #isSolid(int, int)}.</p>
 */
public final class TileMap {
    private static final int T = Constants.TILE;

    private final int cols;
    private final int rows;
    private final TileType[] tiles;
    private boolean gatesClosed;

    public TileMap(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.tiles = new TileType[cols * rows];
        java.util.Arrays.fill(tiles, TileType.EMPTY);
    }

    public TileMap copy() {
        TileMap m = new TileMap(cols, rows);
        System.arraycopy(tiles, 0, m.tiles, 0, tiles.length);
        m.gatesClosed = gatesClosed;
        return m;
    }

    public boolean inBounds(int c, int r) {
        return c >= 0 && r >= 0 && c < cols && r < rows;
    }

    public TileType get(int c, int r) {
        return inBounds(c, r) ? tiles[r * cols + c] : TileType.SOLID;
    }

    public void set(int c, int r, TileType type) {
        if (inBounds(c, r)) tiles[r * cols + c] = type;
    }

    public boolean isSolid(int c, int r) {
        TileType t = get(c, r);
        return t.isSolid() || (t == TileType.GATE && gatesClosed);
    }

    public boolean isOneWay(int c, int r) {
        return get(c, r) == TileType.ONE_WAY;
    }

    public boolean isHazard(int c, int r) {
        return inBounds(c, r) && get(c, r).isHazard();
    }

    public boolean isSolidAt(float wx, float wy) {
        return isSolid(col(wx), row(wy));
    }

    /** Solid or one-way: something you can stand on. */
    public boolean isStandableAt(float wx, float wy) {
        int c = col(wx);
        int r = row(wy);
        return isSolid(c, r) || isOneWay(c, r);
    }

    /** True if the box overlaps the dangerous part (lower 60%) of any spike or mist tile. */
    public boolean overlapsHazard(Hitbox box) {
        int c0 = col(box.getX() + 2);
        int c1 = col(box.right() - 2);
        int r0 = row(box.getY() + 1);
        int r1 = row(box.top() - 1);
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                if (isHazard(c, r) && box.overlaps(c * T + 4, r * T, T - 8, T * 0.6f)) return true;
            }
        }
        return false;
    }

    /** True if any cell of the given type overlaps the box. */
    public boolean overlapsType(Hitbox box, TileType type) {
        int c0 = col(box.getX());
        int c1 = col(box.right() - 0.01f);
        int r0 = row(box.getY());
        int r1 = row(box.top() - 0.01f);
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                if (inBounds(c, r) && get(c, r) == type) return true;
            }
        }
        return false;
    }

    /** Ray-marches between two points and reports whether no solid tile blocks it. */
    public boolean hasLineOfSight(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, (int) (len / 8f));
        for (int i = 1; i < steps; i++) {
            float t = i / (float) steps;
            if (isSolidAt(x0 + dx * t, y0 + dy * t)) return false;
        }
        return true;
    }

    public void setGatesClosed(boolean closed) { gatesClosed = closed; }
    public boolean areGatesClosed() { return gatesClosed; }
    public int getCols() { return cols; }
    public int getRows() { return rows; }
    public float widthPx() { return cols * T; }
    public float heightPx() { return rows * T; }

    public static int col(float wx) { return (int) Math.floor(wx / T); }
    public static int row(float wy) { return (int) Math.floor(wy / T); }
}
