package phantom.autoplay;

import java.util.EnumSet;
import java.util.Set;

import phantom.abilities.AbilityType;
import phantom.abilities.WallCling;
import phantom.entities.player.Player;
import phantom.input.GameAction;
import phantom.util.Constants;
import phantom.util.Direction;

/**
 * One closed-loop movement manoeuvre: walk somewhere, jump (optionally with Wisp Leap and Umbral Dash),
 * drop through a ledge, or zig-zag up a shaft with Moss Grip. Each tick it reads the player's state and
 * decides which buttons to hold, exactly like a person reacting to what they see.
 * <p>The same object type drives both the ghost in the {@link Sandbox} (to test a manoeuvre) and the
 * real player (to perform it), so a manoeuvre that worked in the sandbox works in the game.</p>
 * <p>OOP concept: a small state machine object; immutable parameters plus private runtime state.</p>
 */
final class Maneuver {
    enum Kind { WALK, JUMP, DROP, CLIMB }

    enum Status { RUNNING, DONE, FAILED }

    private static final float DOUBLE_JUMP_VY = 60f;
    private static final float DASH_VY = 160f;

    final Kind kind;
    /** Desired centre x when the manoeuvre ends. */
    final float aimX;
    /** Feet height of the landing spot. */
    final float aimY;
    final int steerDelay;
    final boolean doubleJump;
    final boolean dash;
    /** CLIMB: the wall to jump at first (-1 left, +1 right). */
    final int firstWall;
    /** CLIMB: how far below the exit ledge the final leap may start. */
    final float leapMargin;
    /** JUMP with a run-up: run in {@code runDir} and take off when the centre passes {@code jumpAtX}. */
    final int runDir;
    final float jumpAtX;

    private int t;
    /** Ticks since take-off (the run-up does not count). */
    private int air = -1;
    private boolean airborne;
    private boolean jumpWasHeld;
    private boolean doubleDone;
    private boolean dashDone;
    private boolean leaping;
    private int wallDir;
    private float lastX = Float.NaN;
    private int stillTicks;

    private Maneuver(Kind kind, float aimX, float aimY, int steerDelay, boolean doubleJump, boolean dash,
                     int firstWall, float leapMargin, int runDir, float jumpAtX) {
        this.kind = kind;
        this.aimX = aimX;
        this.aimY = aimY;
        this.steerDelay = steerDelay;
        this.doubleJump = doubleJump;
        this.dash = dash;
        this.firstWall = firstWall;
        this.leapMargin = leapMargin;
        this.wallDir = firstWall;
        this.runDir = runDir;
        this.jumpAtX = jumpAtX;
    }

    static Maneuver walk(float aimX, float aimY) {
        return new Maneuver(Kind.WALK, aimX, aimY, 0, false, false, 0, 0, 0, 0);
    }

    static Maneuver jump(float aimX, float aimY, int steerDelay, boolean doubleJump, boolean dash) {
        return new Maneuver(Kind.JUMP, aimX, aimY, steerDelay, doubleJump, dash, 0, 0, 0, 0);
    }

    /** A running jump: sprint in {@code runDir}, leave the ground at {@code jumpAtX}. */
    static Maneuver runningJump(float aimX, float aimY, int runDir, float jumpAtX, boolean doubleJump, boolean dash) {
        return new Maneuver(Kind.JUMP, aimX, aimY, 0, doubleJump, dash, 0, 0, runDir, jumpAtX);
    }

    static Maneuver drop(float aimX, float aimY) {
        return new Maneuver(Kind.DROP, aimX, aimY, 0, false, false, 0, 0, 0, 0);
    }

    static Maneuver climb(float aimX, float aimY, int firstWall, float leapMargin) {
        return new Maneuver(Kind.CLIMB, aimX, aimY, 0, false, false, firstWall, leapMargin, 0, 0);
    }

    /** The same manoeuvre aimed at a different landing x. */
    Maneuver withAim(float newAimX) {
        return new Maneuver(kind, newAimX, aimY, steerDelay, doubleJump, dash, firstWall, leapMargin, runDir, jumpAtX);
    }

    /** A fresh copy with the same parameters, ready to run from the start. */
    Maneuver fresh() {
        return new Maneuver(kind, aimX, aimY, steerDelay, doubleJump, dash, firstWall, leapMargin, runDir, jumpAtX);
    }

    /** Decides this tick's buttons. Call before the world advances. */
    Status step(Player p, Set<GameAction> out) {
        out.clear();
        if (!p.isOnGround()) airborne = true;
        Status s;
        switch (kind) {
            case WALK: s = walk(p, out); break;
            case JUMP: s = jump(p, out); break;
            case DROP: s = drop(p, out); break;
            default: s = climb(p, out); break;
        }
        jumpWasHeld = out.contains(GameAction.JUMP);
        t++;
        return s;
    }

    private Status walk(Player p, Set<GameAction> out) {
        if (airborne && p.isOnGround()) return Status.DONE;
        float dx = aimX - p.centerX();
        boolean moving = steer(p, out, dx, p.isOnGround() ? Constants.GROUND_FRICTION : Constants.AIR_ACCEL, 3f);
        if (!moving && p.isOnGround() && Math.abs(p.getVelocityX()) < 25f) return Status.DONE;
        if (p.isOnGround() && t % 20 == 0) {
            if (!Float.isNaN(lastX) && Math.abs(p.getX() - lastX) < 2f && Math.abs(dx) > 8f) return Status.FAILED;
            lastX = p.getX();
        }
        return t > 900 ? Status.FAILED : Status.RUNNING;
    }

    private Status jump(Player p, Set<GameAction> out) {
        if (air < 0) {
            boolean ready = runDir == 0 || (p.centerX() - jumpAtX) * runDir >= 0;
            if (!ready) {
                // run-up: sprint towards the take-off point
                if (airborne) return Status.FAILED; // fell off before jumping
                out.add(dirAction(runDir));
                return t > 300 ? Status.FAILED : Status.RUNNING;
            }
            air = 0;
            out.add(GameAction.JUMP);
            if (runDir != 0) out.add(dirAction(runDir));
            air++;
            return Status.RUNNING;
        }
        if (airborne && p.isOnGround()) return Status.DONE;
        if (!airborne && air > 4) return Status.FAILED; // never left the ground
        holdJumpWhileRising(p, out);
        airMoves(p, out);
        if (air >= steerDelay) steer(p, out, aimX - p.centerX(), Constants.AIR_ACCEL, 4f);
        air++;
        return t > 400 ? Status.FAILED : Status.RUNNING;
    }

    private Status drop(Player p, Set<GameAction> out) {
        if (airborne && p.isOnGround()) return Status.DONE;
        if (!airborne && t > 4) return Status.FAILED;
        if (t == 0) {
            out.add(GameAction.DOWN);
            out.add(GameAction.JUMP);
            return Status.RUNNING;
        }
        if (t > 6) steer(p, out, aimX - p.centerX(), Constants.AIR_ACCEL, 4f);
        return t > 300 ? Status.FAILED : Status.RUNNING;
    }

    private Status climb(Player p, Set<GameAction> out) {
        if (airborne && p.isOnGround()) return Status.DONE;
        if (!airborne && t > 4) return Status.FAILED;
        if (t == 0) {
            out.add(GameAction.JUMP);
            out.add(dirAction(wallDir));
            return Status.RUNNING;
        }
        if (leaping) {
            holdJumpWhileRising(p, out);
            steer(p, out, aimX - p.centerX(), Constants.AIR_ACCEL, 4f);
            return t > 900 ? Status.FAILED : Status.RUNNING;
        }
        if (p.isClinging()) {
            int side = p.getAbilities().get(AbilityType.WALL_CLING, WallCling.class).getWallSide() == Direction.LEFT ? -1 : 1;
            int away = -side;
            if (jumpWasHeld) {
                // let go of jump for one tick so the wall jump registers as a fresh press
                out.add(dirAction(side));
                return Status.RUNNING;
            }
            out.add(GameAction.JUMP);
            out.add(dirAction(away));
            wallDir = away;
            float dx = aimX - p.centerX();
            if (p.getY() >= aimY - leapMargin && Math.signum(dx) == away) leaping = true;
            return Status.RUNNING;
        }
        holdJumpWhileRising(p, out);
        out.add(dirAction(wallDir));
        return t > 900 ? Status.FAILED : Status.RUNNING;
    }

    private void holdJumpWhileRising(Player p, Set<GameAction> out) {
        if (p.getVelocityY() > 0 && jumpWasHeld) out.add(GameAction.JUMP);
    }

    /** Wisp Leap near the top of the jump, Umbral Dash towards the target. */
    private void airMoves(Player p, Set<GameAction> out) {
        if (!airborne) return;
        if (doubleJump && !doubleDone && air > 3 && p.getVelocityY() < DOUBLE_JUMP_VY) {
            if (jumpWasHeld) {
                out.remove(GameAction.JUMP);
            } else {
                out.add(GameAction.JUMP);
                doubleDone = true;
            }
        }
        float dx = aimX - p.centerX();
        if (dash && !dashDone && air >= steerDelay && p.getVelocityY() < DASH_VY && Math.abs(dx) > 48f
                && Math.signum(dx) == p.getFacing().dx()) {
            out.add(GameAction.DASH);
            dashDone = true;
        }
    }

    /** Holds towards the target, letting go early enough to stop on it. Returns true if a direction is held. */
    private static boolean steer(Player p, Set<GameAction> out, float dx, float decel, float deadZone) {
        float vx = p.getVelocityX();
        float brake = vx * vx / (2f * decel);
        boolean towards = Math.signum(vx) == Math.signum(dx);
        if (Math.abs(dx) <= deadZone || (towards && Math.abs(dx) <= brake + 1f)) return false;
        out.add(dx > 0 ? GameAction.RIGHT : GameAction.LEFT);
        return true;
    }

    private static GameAction dirAction(int dir) {
        return dir < 0 ? GameAction.LEFT : GameAction.RIGHT;
    }

    /** The buttons as a fresh set (helper for callers). */
    static Set<GameAction> newButtons() {
        return EnumSet.noneOf(GameAction.class);
    }

    @Override
    public String toString() {
        return kind + (runDir != 0 ? "run" + (runDir > 0 ? ">" : "<") : "") + "(" + (int) (aimX / Constants.TILE) + "," + (int) (aimY / Constants.TILE)
                + (steerDelay > 0 ? " d" + steerDelay : "") + (doubleJump ? " dbl" : "") + (dash ? " dash" : "")
                + (kind == Kind.CLIMB ? " w" + firstWall + " m" + (int) leapMargin : "") + ")";
    }
}
