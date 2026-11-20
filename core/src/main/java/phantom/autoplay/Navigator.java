package phantom.autoplay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import phantom.abilities.AbilityType;
import phantom.entities.player.Player;
import phantom.input.GameAction;
import phantom.util.Constants;
import phantom.world.GameWorld;
import phantom.world.TileMap;

/**
 * Follows routes from {@link RoomNav}: walks to each take-off point, checks the manoeuvre once more
 * against Phantom's exact current state in the {@link Sandbox}, performs it, and replans whenever
 * reality and the plan disagree (an enemy's knockback, a missed landing, a wall that just broke).
 * <p>OOP concept: a stateful helper owned by {@link AutoPilot} (composition).</p>
 */
final class Navigator {
    /** Result of one tick of driving. */
    enum Drive { MOVING, ARRIVED, UNREACHABLE }

    private static final int T = Constants.TILE;

    private final Sandbox sandbox = new Sandbox();
    private final Dangers dangers;
    private GameWorld world;
    private int dangerWait;
    private RoomNav nav;
    private String navKey = "";
    private String navRoom = "";
    private List<RoomNav.Step> route;
    private int stepIndex;
    private RoomNav.Segment expected;
    private Object goalKey;
    private Object routeKey;
    private Maneuver live;
    private RoomNav.Edge liveEdge;
    private int liveHealth;
    private int verifyFails;
    private int lostTicks;
    private final Map<RoomNav.Edge, Integer> failures = new HashMap<>();

    Navigator(Dangers dangers) {
        this.dangers = dangers;
    }

    RoomNav nav() { return nav; }

    Sandbox sandbox() { return sandbox; }
    boolean isManeuvering() { return live != null; }

    /** Forgets the current route (it will be recomputed next tick). */
    void replan() {
        route = null;
        live = null;
    }

    /** Rebuilds the graph if the room, the abilities or the tiles changed. */
    void sync(GameWorld world) {
        Player p = world.getPlayer();
        TileMap map = world.getTileMap();
        StringBuilder key = new StringBuilder(world.getRoomId()).append('|').append(System.identityHashCode(map));
        for (AbilityType t : AbilityType.values()) key.append(p.getAbilities().has(t) ? '1' : '0');
        key.append(map.areGatesClosed() ? 'g' : '-');
        key.append(world.getProgress().getBrokenWallsCount());
        if (!key.toString().equals(navKey)) {
            navKey = key.toString();
            boolean newRoom = nav == null || !world.getRoomId().equals(navRoom);
            navRoom = world.getRoomId();
            sandbox.setRoom(map, world.getRegion(), world.getRoomId());
            nav = new RoomNav(map, sandbox, p);
            if (newRoom && world.getTransitionFade() > 0f) nav.discoverAll(); // the screen is dark: plan ahead
            failures.clear();
            replan();
        }
    }

    /** Segment the player is standing on, or null if airborne or not on a known floor. */
    RoomNav.Segment here(Player p) {
        if (!p.isOnGround()) return null;
        return nav.locate(p.centerX(), p.getY(), p.getWidth());
    }

    /** One tick of travel towards a goal. Fills {@code out} with the buttons to hold. */
    Drive drive(GameWorld world, RoomNav.Goal goal, Object key, Set<GameAction> out) {
        Player p = world.getPlayer();
        this.world = world;
        goalKey = key;
        if (live != null) {
            Drive d = continueManeuver(p, out);
            if (d != null) return d;
        }
        if (route != null && !key.equals(routeKey)) route = null;
        out.clear();
        if (!p.isOnGround()) return Drive.MOVING; // fall or knockback: wait until we land
        RoomNav.Segment seg = here(p);
        if (seg == null) {
            // standing somewhere the graph does not know (edge of a ledge): shuffle towards solid ground
            if (++lostTicks > 90) {
                out.add(GameAction.JUMP);
                lostTicks = 0;
            } else {
                out.add(lostTicks / 30 % 2 == 0 ? GameAction.LEFT : GameAction.RIGHT);
            }
            return Drive.MOVING;
        }
        lostTicks = 0;
        if (route == null || seg != expected) {
            route = nav.route(seg, p.centerX(), goal, 6000);
            if (route == null && nav.hasBans()) {
                // maybe a jump we gave up on was the only way: forgive it and try again
                nav.clearBans();
                failures.clear();
                route = nav.route(seg, p.centerX(), goal, 6000);
            }
            routeKey = key;
            stepIndex = 0;
            expected = seg;
            verifyFails = 0;
            if (route == null) return Drive.UNREACHABLE;
        }
        RoomNav.Step step = route.get(stepIndex);
        if (step.edge() == null) {
            float dx = step.walkToX() - p.centerX();
            if (Math.abs(dx) <= 7f) {
                return Math.abs(p.getVelocityX()) < 40f ? Drive.ARRIVED : Drive.MOVING;
            }
            walkTowards(p, dx, out);
            return Drive.MOVING;
        }
        approachAndLaunch(p, step.edge(), out);
        return Drive.MOVING;
    }

    private Drive continueManeuver(Player p, Set<GameAction> out) {
        if (p.getHealth().getCurrent() < liveHealth || p.isDead()) {
            // knocked out of the manoeuvre (an enemy, not the jump itself): let physics settle, then replan
            live = null;
            route = null;
            out.clear();
            return Drive.MOVING;
        }
        Maneuver.Status s = live.step(p, out);
        if (s == Maneuver.Status.RUNNING) return Drive.MOVING;
        RoomNav.Edge e = liveEdge;
        live = null;
        if (s == Maneuver.Status.DONE) {
            RoomNav.Segment landed = here(p);
            if (landed == e.to() && route != null && stepIndex + 1 < route.size()) {
                stepIndex++;
                expected = landed;
                verifyFails = 0;
            } else {
                route = null;
            }
        } else {
            fail(e);
        }
        return null;
    }

    private void fail(RoomNav.Edge e) {
        int n = failures.merge(e, 1, Integer::sum);
        if (n >= 2) nav.ban(e);
        route = null;
    }

    private void approachAndLaunch(Player p, RoomNav.Edge e, Set<GameAction> out) {
        Maneuver plan = e.plan();
        float x = p.centerX();
        float vx = p.getVelocityX();
        boolean inZone;
        if (plan.kind == Maneuver.Kind.JUMP && plan.runDir != 0) {
            int dir = plan.runDir;
            inZone = (x - e.takeoffX()) * dir >= -4f && (plan.jumpAtX - x) * dir > 1f && vx * dir >= -5f
                    && !p.isControlLocked();
        } else {
            inZone = Math.abs(x - e.takeoffX()) <= 3f && Math.abs(vx) < 15f && !p.isControlLocked();
        }
        if (inZone) {
            Maneuver go = verify(p, e);
            if (go != null) {
                live = go;
                liveEdge = e;
                liveHealth = p.getHealth().getCurrent();
                Maneuver.Status s = live.step(p, out);
                if (s != Maneuver.Status.RUNNING) {
                    live = null;
                    fail(e);
                }
                return;
            }
            if (++verifyFails > 45) {
                verifyFails = 0;
                fail(e);
                return;
            }
            if (waitingForDanger) {
                out.clear(); // hold position until the way is clear
                return;
            }
        }
        float dx = e.takeoffX() - x;
        if (Math.abs(dx) > 2f) walkTowards(p, dx, out);
    }

    private boolean waitingForDanger;

    /**
     * Re-runs the manoeuvre from the player's exact state, also checking it against predicted enemies.
     * Returns the manoeuvre to perform (possibly re-aimed to land beside an enemy instead of on it),
     * or null if it is not safe to go yet.
     */
    private Maneuver verify(Player p, RoomNav.Edge e) {
        waitingForDanger = false;
        if (!lands(p, e.plan(), e.to(), null)) return null;
        dangers.predict(world, p);
        if (lands(p, e.plan(), e.to(), dangers)) {
            dangerWait = 0;
            return e.plan().fresh();
        }
        for (int c = e.to().c0(); c <= e.to().c1(); c++) {
            Maneuver alt = e.plan().withAim(RoomNav.centerOf(c));
            if (lands(p, alt, e.to(), dangers)) {
                dangerWait = 0;
                return alt.fresh();
            }
        }
        // nothing safe right now: wait a little for the danger to move, then accept the risk
        if (++dangerWait > 150) {
            dangerWait = 0;
            return e.plan().fresh();
        }
        verifyFails--;
        waitingForDanger = true;
        return null;
    }

    private boolean lands(Player p, Maneuver m, RoomNav.Segment target, Dangers d) {
        sandbox.reset(p);
        Sandbox.Outcome o = sandbox.run(m.fresh(), 720, d, p.getHealth().getInvincibleTime());
        return o.ok() && nav.locate(o.centerX(), o.y(), p.getWidth()) == target;
    }

    /** Holds towards a target on the ground, letting go early enough to stop on it. */
    static void walkTowards(Player p, float dx, Set<GameAction> out) {
        float vx = p.getVelocityX();
        float brake = vx * vx / (2f * Constants.GROUND_FRICTION);
        boolean towards = Math.signum(vx) == Math.signum(dx);
        if (towards && Math.abs(dx) <= brake + 1.5f) return;
        out.add(dx > 0 ? GameAction.RIGHT : GameAction.LEFT);
    }

    static int col(float x) {
        return (int) Math.floor(x / T);
    }
}
