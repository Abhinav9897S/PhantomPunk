package phantom.world.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import phantom.world.TileMap;
import phantom.world.TileType;

/**
 * Breadth-first search over the places a player can stand (and, with Moss Grip, cling) inside
 * one room. Edges are jumps and falls checked against the {@link MovementModel} and against a
 * clear "L-shaped" path (rise, move across, descend) so walls and spikes block them.
 * <p>OOP concept: an algorithm object with a single public query, {@link #explore}.</p>
 */
public final class RoomReachability {
    private final TileMap map;
    private final MovementModel model;
    private final int cols;
    private final int rows;

    public RoomReachability(TileMap map, MovementModel model) {
        this.map = map;
        this.model = model;
        this.cols = map.getCols();
        this.rows = map.getRows();
    }

    /** Returns every node (encoded as {@code row * cols + col}) reachable from the start cell. */
    public Set<Integer> explore(int startCol, int startRow) {
        Set<Integer> visited = new HashSet<>();
        int start = settle(startCol, startRow);
        if (start < 0) return visited;
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        int reach = model.maxReachTiles();
        int rise = model.maxRiseTiles();
        while (!queue.isEmpty()) {
            int node = queue.poll();
            int c = node % cols;
            int r = node / cols;
            for (int next : neighbours(c, r, reach, rise)) {
                if (visited.add(next)) queue.add(next);
            }
        }
        return visited;
    }

    /** True if some reached node can get its body into the target cell (doors, pickups, bosses). */
    public boolean canTouch(Set<Integer> reached, int targetCol, int targetRow) {
        int reach = model.maxReachTiles();
        int rise = model.maxRiseTiles();
        for (int node : reached) {
            int c = node % cols;
            int r = node / cols;
            if (Math.abs(targetCol - c) > reach || targetRow - r > rise + 1) continue;
            if (c == targetCol && r == targetRow) return true;
            if (jumpPossible(c, r, targetCol, targetRow, rise + 1)) return true;
        }
        return false;
    }

    private List<Integer> neighbours(int c, int r, int reach, int rise) {
        List<Integer> out = new ArrayList<>();
        boolean standing = supported(c, r);
        // a wall jump always pushes away from the wall, so from a pure cling you must leave it
        int away = standing ? 0 : wallSide(c, r) * -1;
        for (int dc = -1; dc <= 1; dc += 2) {
            if (away != 0 && dc != away) continue;
            int fell = fallFrom(c + dc, r);
            if (fell >= 0) out.add(fell);
        }
        for (int ty = Math.max(0, r - 12); ty <= Math.min(rows - 2, r + rise); ty++) {
            for (int tx = Math.max(0, c - reach); tx <= Math.min(cols - 1, c + reach); tx++) {
                if (tx == c && ty == r) continue;
                if (away != 0 && Integer.signum(tx - c) != away) continue;
                if (!isNode(tx, ty)) continue;
                if (jumpPossible(c, r, tx, ty, rise)) out.add(ty * cols + tx);
            }
        }
        return out;
    }

    /** -1 if clinging to a wall on the left, +1 on the right, 0 if not next to a wall. */
    private int wallSide(int c, int r) {
        if (blocks(c - 1, r) && blocks(c - 1, r + 1)) return -1;
        if (blocks(c + 1, r) && blocks(c + 1, r + 1)) return 1;
        return 0;
    }

    private boolean jumpPossible(int c, int r, int tx, int ty, int rise) {
        float dyPx = (ty - r) * (float) phantom.util.Constants.TILE;
        float reachPx = model.horizontalReachPx(dyPx);
        if (reachPx < 0 || MovementModel.requiredTravelPx(tx - c) > reachPx) return false;
        int lowestApex = Math.max(r, ty);
        for (int apex = lowestApex; apex <= Math.min(rows - 2, r + rise); apex++) {
            if (pathClear(c, r, tx, ty, apex)) return true;
        }
        return false;
    }

    /** Rise in the start column, move across at the apex row, then drop into the target column. */
    private boolean pathClear(int c, int r, int tx, int ty, int apex) {
        for (int y = r; y <= apex; y++) if (!bodyClear(c, y)) return false;
        int step = tx >= c ? 1 : -1;
        for (int x = c; x != tx + step; x += step) if (!bodyClear(x, apex)) return false;
        for (int y = ty; y <= apex; y++) if (!bodyClear(tx, y)) return false;
        return true;
    }

    /** Follows a fall straight down from (c, r) until the body lands; -1 if it lands in a hazard. */
    private int fallFrom(int c, int r) {
        if (!map.inBounds(c, r) || !bodyClear(c, r)) return -1;
        int y = r;
        while (y > 0 && !supported(c, y) && bodyClear(c, y - 1)) y--;
        if (!supported(c, y) && !(model.canWallClimb() && isWallNode(c, y))) return -1;
        return isNode(c, y) ? y * cols + c : -1;
    }

    /** Settles a start position onto the floor below it. */
    private int settle(int c, int r) {
        if (!map.inBounds(c, r) || !bodyClear(c, r)) return -1;
        return fallFrom(c, r);
    }

    private boolean isNode(int c, int r) {
        return bodyClear(c, r) && (supported(c, r) || (model.canWallClimb() && isWallNode(c, r)));
    }

    private boolean isWallNode(int c, int r) {
        if (!bodyClear(c, r)) return false;
        boolean left = blocks(c - 1, r) && blocks(c - 1, r + 1);
        boolean right = blocks(c + 1, r) && blocks(c + 1, r + 1);
        return left || right;
    }

    private boolean supported(int c, int r) {
        if (r == 0) return true; // the room's floor edge counts as solid
        TileType below = map.get(c, r - 1);
        return below == TileType.SOLID || below == TileType.ONE_WAY || below == TileType.BREAKABLE;
    }

    /** The player's two-tile-tall body fits in this cell and the one above, away from hazards. */
    private boolean bodyClear(int c, int r) {
        return passable(c, r) && passable(c, r + 1);
    }

    private boolean passable(int c, int r) {
        if (!map.inBounds(c, r)) return false; // the room edge is solid at runtime too
        TileType t = map.get(c, r);
        return t != TileType.SOLID && !t.isHazard();
    }

    private boolean blocks(int c, int r) {
        return map.inBounds(c, r) && map.get(c, r) == TileType.SOLID;
    }

    public int getCols() { return cols; }
}
