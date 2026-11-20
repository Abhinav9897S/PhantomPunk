package phantom.autoplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import phantom.abilities.AbilityType;
import phantom.entities.player.Player;
import phantom.util.Constants;
import phantom.world.TileMap;

/**
 * Navigation graph of one room for one set of abilities. Nodes are <b>segments</b> (runs of floor the
 * player can walk along); edges are manoeuvres that were tried in the {@link Sandbox} and really land
 * on another segment. Edges are discovered lazily, the first time a search reaches a segment.
 * <p>OOP concept: an algorithm object; records for the graph parts; Dijkstra over a {@link PriorityQueue}.</p>
 */
final class RoomNav {
    private static final int T = Constants.TILE;

    /** A walkable run of floor: feet row {@code row}, columns {@code c0..c1}. */
    record Segment(int id, int row, int c0, int c1) {
        boolean contains(int c) { return c >= c0 && c <= c1; }
        float feetY() { return row * T; }
        int width() { return c1 - c0 + 1; }
    }

    /** A tested manoeuvre from one segment to another; it starts with the player standing at {@code takeoffX}. */
    record Edge(Segment from, float takeoffX, Maneuver plan, Segment to, float landX, int ticks) { }

    /** A manoeuvre plus the column it must start from. */
    private record Plan(int startCol, Maneuver m) { }

    /** One step of a route: walk to {@code takeoffX}, then perform the edge (null edge = final walk). */
    record Step(Edge edge, float walkToX) { }

    private final TileMap map;
    private final int cols;
    private final int rows;
    private final Sandbox sandbox;
    private final Player abilities;
    private final boolean canDash;
    private final boolean canDouble;
    private final boolean canClimb;
    private final List<Segment> segments = new ArrayList<>();
    private final int[] segAt;
    private final Map<Integer, List<Edge>> edges = new HashMap<>();
    private final Set<Long> banned = new java.util.HashSet<>();
    private int simulations;

    RoomNav(TileMap map, Sandbox sandbox, Player abilities) {
        this.map = map;
        this.cols = map.getCols();
        this.rows = map.getRows();
        this.sandbox = sandbox;
        this.abilities = abilities;
        this.canDash = abilities.getAbilities().has(AbilityType.DASH);
        this.canDouble = abilities.getAbilities().has(AbilityType.DOUBLE_JUMP);
        this.canClimb = abilities.getAbilities().has(AbilityType.WALL_CLING);
        segAt = new int[cols * rows];
        Arrays.fill(segAt, -1);
        buildSegments();
    }

    // ------------------------------------------------------------------ tiles

    boolean standable(int c, int r) {
        return bodyClear(c, r) && (map.isSolid(c, r - 1) || map.isOneWay(c, r - 1));
    }

    private boolean bodyClear(int c, int r) {
        return passable(c, r) && passable(c, r + 1);
    }

    private boolean passable(int c, int r) {
        return map.inBounds(c, r) && !map.isSolid(c, r) && !map.get(c, r).isHazard();
    }

    private void buildSegments() {
        for (int r = 0; r < rows; r++) {
            int c = 0;
            while (c < cols) {
                if (!standable(c, r)) {
                    c++;
                    continue;
                }
                int start = c;
                while (c + 1 < cols && standable(c + 1, r)) c++;
                Segment s = new Segment(segments.size(), r, start, c);
                segments.add(s);
                for (int k = start; k <= c; k++) segAt[r * cols + k] = s.id();
                c++;
            }
        }
    }

    Segment segmentAt(int c, int r) {
        if (c < 0 || r < 0 || c >= cols || r >= rows) return null;
        int id = segAt[r * cols + c];
        return id < 0 ? null : segments.get(id);
    }

    /** The segment a grounded body at (centre x, feet y) stands on, or null. */
    Segment locate(float centerX, float feetY, float width) {
        int r = Math.round(feetY / T);
        if (Math.abs(feetY - r * T) > 3f) return null;
        Segment s = segmentAt(TileMap.col(centerX), r);
        if (s == null) s = segmentAt(TileMap.col(centerX - width / 2f + 1f), r);
        if (s == null) s = segmentAt(TileMap.col(centerX + width / 2f - 1f), r);
        return s;
    }

    List<Segment> segments() { return segments; }
    int simulations() { return simulations; }

    static float centerOf(int col) {
        return col * T + T / 2f;
    }

    // ------------------------------------------------------------------ edges

    /** Forbids an edge that failed in the real game (e.g. a jump an enemy keeps interrupting). */
    void ban(Edge e) {
        banned.add(key(e.from().id(), e.to().id()));
        List<Edge> list = edges.get(e.from().id());
        if (list != null) list.removeIf(x -> x.to() == e.to());
    }

    boolean hasBans() { return !banned.isEmpty(); }

    /** Forgets every ban; edges are rediscovered on demand. */
    void clearBans() {
        banned.clear();
        edges.clear();
    }

    private static long key(int a, int b) {
        return ((long) a << 32) | (b & 0xffffffffL);
    }

    /** Works out every edge of the room up front (done while the screen is black between rooms). */
    void discoverAll() {
        for (Segment s : segments) edgesFrom(s);
    }

    List<Edge> edgesFrom(Segment s) {
        List<Edge> list = edges.get(s.id());
        if (list == null) {
            list = discover(s);
            edges.put(s.id(), list);
        }
        return list;
    }

    private List<Edge> discover(Segment s) {
        Map<Integer, Edge> found = new HashMap<>();
        for (Segment target : segments) {
            if (target == s || found.containsKey(target.id()) || banned.contains(key(s.id(), target.id()))) continue;
            if (!plausible(s, target)) continue;
            tryPair(s, target, found);
        }
        return new ArrayList<>(found.values());
    }

    private boolean plausible(Segment s, Segment t) {
        int dy = t.row() - s.row();
        int gap = gapCols(s, t);
        int maxRise = canDouble ? 7 : 3;
        if (dy > maxRise) {
            if (!canClimb) return false;
            return gap <= 4 && dy <= 40;
        }
        int maxGap = (canDash ? 12 : 7) + (canDouble ? 3 : 0) + Math.max(0, -dy) / 2;
        return gap <= maxGap;
    }

    private static int gapCols(Segment a, Segment b) {
        if (b.c1() < a.c0()) return a.c0() - b.c1();
        if (b.c0() > a.c1()) return b.c0() - a.c1();
        return 0;
    }

    private void tryPair(Segment s, Segment t, Map<Integer, Edge> found) {
        int dy = t.row() - s.row();
        Set<Integer> takeoffs = new LinkedHashSet<>();
        int near = clamp(t.c0() > s.c1() ? t.c0() : (t.c1() < s.c0() ? t.c1() : (s.c0() + s.c1()) / 2), s.c0(), s.c1());
        takeoffs.add(near);
        if (dy > 0 && gapCols(s, t) == 0) {
            // target overhead: take off just beside it so the ledge does not bump our head
            addIfIn(takeoffs, s, t.c0() - 1);
            addIfIn(takeoffs, s, t.c1() + 1);
            addIfIn(takeoffs, s, t.c0() - 2);
            addIfIn(takeoffs, s, t.c1() + 2);
        }
        takeoffs.add(s.c0());
        takeoffs.add(s.c1());
        for (int c0 : takeoffs) {
            List<Plan> plans = plansFor(s, t, c0, dy);
            for (Plan p : plans) {
                Edge e = simulate(s, p.startCol(), p.m());
                if (e == null) continue;
                if (!found.containsKey(e.to().id()) && !banned.contains(key(s.id(), e.to().id()))) found.put(e.to().id(), e);
                if (e.to() == t) return;
            }
        }
    }

    private static void addIfIn(Set<Integer> set, Segment s, int c) {
        if (s.contains(c)) set.add(c);
    }

    private List<Plan> plansFor(Segment s, Segment t, int c0, int dy) {
        List<Maneuver> out = new ArrayList<>();
        List<Plan> plans = new ArrayList<>();
        int aimCol = t.width() >= 3 ? clamp(c0, t.c0() + 1, t.c1() - 1) : clamp(c0, t.c0(), t.c1());
        int midCol = (t.c0() + t.c1()) / 2;
        float y = t.feetY();
        float aim = centerOf(aimCol);
        float mid = centerOf(midCol);
        boolean edgeLeft = c0 == s.c0() && aimCol < c0;
        boolean edgeRight = c0 == s.c1() && aimCol > c0;
        if (dy <= 0 && (edgeLeft || edgeRight)) out.add(Maneuver.walk(aim, y));
        if (dy < 0 && map.isOneWay(c0, s.row() - 1) && !map.isSolid(c0, s.row() - 1)) {
            out.add(Maneuver.drop(aim, y));
            out.add(Maneuver.drop(mid, y));
        }
        int dir = aimCol > c0 ? 1 : (aimCol < c0 ? -1 : 0);
        int runStart = dir == 0 ? c0 : clamp(c0 - dir * 3, s.c0(), s.c1());
        boolean canRun = dir != 0 && runStart != c0;
        for (float offset : new float[] {4f, -10f}) {
            float jumpAt = centerOf(c0) + dir * offset;
            if (canRun && dy <= 3) plans.add(new Plan(runStart, Maneuver.runningJump(aim, y, dir, jumpAt, false, false)));
            if (canRun && canDash && dy <= 3) plans.add(new Plan(runStart, Maneuver.runningJump(aim, y, dir, jumpAt, false, true)));
            if (canRun && canDouble) plans.add(new Plan(runStart, Maneuver.runningJump(aim, y, dir, jumpAt, true, false)));
            if (canRun && canDouble && canDash) plans.add(new Plan(runStart, Maneuver.runningJump(aim, y, dir, jumpAt, true, true)));
        }
        if (dy <= 3) {
            out.add(Maneuver.jump(aim, y, 0, false, false));
            if (dy > 0) out.add(Maneuver.jump(aim, y, 14, false, false));
            out.add(Maneuver.jump(mid, y, 0, false, false));
        }
        if (canDash && dy <= 3) out.add(Maneuver.jump(aim, y, 0, false, true));
        if (canDouble) {
            out.add(Maneuver.jump(aim, y, 0, true, false));
            out.add(Maneuver.jump(aim, y, 14, true, false));
            out.add(Maneuver.jump(mid, y, 0, true, false));
            if (canDash) out.add(Maneuver.jump(aim, y, 0, true, true));
        }
        if (canClimb && dy >= 2) {
            for (int wall = -1; wall <= 1; wall += 2) {
                out.add(Maneuver.climb(aim, y, wall, 2 * T));
                out.add(Maneuver.climb(mid, y, wall, T));
            }
        }
        List<Plan> all = new ArrayList<>();
        for (Maneuver m : out) all.add(new Plan(c0, m));
        if (dy > 0) {
            all.addAll(plans); // rising: standing jumps first (they bump into fewer ledges)
            return all;
        }
        plans.addAll(all);
        return plans;
    }

    private Edge simulate(Segment s, int c0, Maneuver m) {
        float x = c0 * T + (T - Constants.PLAYER_WIDTH) / 2f;
        Sandbox.Outcome o = landing(s, x, m);
        if (o == null) return null;
        Segment land = locate(o.centerX(), o.y(), Constants.PLAYER_WIDTH);
        if (land == null || land == s) return null;
        // only keep manoeuvres that still work when we start a few pixels off
        for (float off : new float[] {-3f, 3f}) {
            Sandbox.Outcome p = landing(s, x + off, m);
            if (p == null || locate(p.centerX(), p.y(), Constants.PLAYER_WIDTH) != land) return null;
        }
        return new Edge(s, centerOf(c0), m, land, o.centerX(), o.ticks());
    }

    private Sandbox.Outcome landing(Segment s, float x, Maneuver m) {
        simulations++;
        sandbox.resetStanding(abilities, x, s.feetY());
        Sandbox.Outcome o = sandbox.run(m.fresh(), 720);
        return o.ok() ? o : null;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ------------------------------------------------------------------ search

    /** Goal test for a route: which columns of a segment count as "arrived". */
    interface Goal {
        /** Column on the segment to walk to, or -1 if this segment is not a goal. */
        int goalColumn(Segment s, float fromX);
    }

    /** Goal reached by standing in any of the given cells. */
    static Goal cells(List<int[]> cells) {
        return (s, fromX) -> {
            int best = -1;
            float bestD = Float.MAX_VALUE;
            for (int[] cell : cells) {
                if (cell[1] != s.row() || !s.contains(cell[0])) continue;
                float d = Math.abs(centerOf(cell[0]) - fromX);
                if (d < bestD) {
                    bestD = d;
                    best = cell[0];
                }
            }
            return best;
        };
    }

    private record Node(Segment seg, float x, double cost, Node parent, Edge via) { }

    /**
     * Cheapest route (in ticks) from a segment and x position to a goal. Returns the list of steps,
     * an empty list if already at the goal segment column, or null if unreachable.
     */
    List<Step> route(Segment start, float x, Goal goal, int maxExpansions) {
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.cost(), b.cost()));
        open.add(new Node(start, x, 0, null, null));
        Set<Object> closed = new java.util.HashSet<>();
        int expansions = 0;
        while (!open.isEmpty() && expansions < maxExpansions) {
            Node n = open.poll();
            Object k = n.via() == null ? "start" : n.via();
            if (!closed.add(k)) continue;
            expansions++;
            int gc = goal.goalColumn(n.seg(), n.x());
            if (gc >= 0) return build(n, centerOf(gc));
            for (Edge e : edgesFrom(n.seg())) {
                if (closed.contains(e)) continue;
                double walk = Math.abs(e.takeoffX() - n.x()) / Constants.RUN_SPEED * 60f;
                open.add(new Node(e.to(), e.landX(), n.cost() + walk + e.ticks() + 8, n, e));
            }
        }
        return null;
    }

    private List<Step> build(Node goalNode, float finalX) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(null, finalX));
        for (Node n = goalNode; n.via() != null; n = n.parent()) {
            steps.add(0, new Step(n.via(), n.via().takeoffX()));
        }
        return steps;
    }
}
