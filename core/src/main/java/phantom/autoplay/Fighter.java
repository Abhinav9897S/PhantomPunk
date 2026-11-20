package phantom.autoplay;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import phantom.abilities.AbilityType;
import phantom.abilities.DoubleJump;
import phantom.components.Hitbox;
import phantom.entities.GameObject;
import phantom.entities.Projectile;
import phantom.entities.Team;
import phantom.entities.bosses.Boss;
import phantom.entities.enemies.Enemy;
import phantom.entities.player.Player;
import phantom.input.GameAction;
import phantom.util.Constants;
import phantom.world.DoorData;
import phantom.world.GameWorld;

/**
 * Combat for the autoplay pilot. Every few ticks it predicts where every danger will be over the
 * next ~0.7 seconds (charging bosses, leaps, eruptions, projectiles, diving moths, falling rocks),
 * then plays a dozen candidate moves - stand, walk, jump, dash, double jump, focus - on the
 * {@link Sandbox} ghost and keeps the one that avoids every hit while staying in striking range.
 * Slashes, up-slashes and Light Bolts are thrown whenever the real target is inside their reach.
 * <p>OOP concept: a "rollout" planner - simple simulation instead of hand-written rules per boss.</p>
 */
final class Fighter {
    private static final float DT = Constants.STEP;
    private static final int HORIZON = 42;
    private static final int FOCUS_TICKS = 60;
    private static final int REPLAN_EVERY = 3;
    private static final int RECOVER_HORIZON = 80;

    /** What a plan is for: a guardian, ordinary enemies, or getting back to safe ground after a hit. */
    private enum Mode { BOSS, SKIRMISH, RECOVER }

    /** A short scripted move tried in the sandbox. */
    private record Candidate(int dir1, int dir2, int switchAt, int jumpAt, boolean doubleJump, int dashAt, int dashDir,
                             boolean focus) {
        void buttons(int k, Player g, boolean jumpHeld, Set<GameAction> out) {
            out.clear();
            if (focus) {
                out.add(GameAction.FOCUS);
                return;
            }
            int dir = k < switchAt ? dir1 : dir2;
            if (dashAt >= 0 && k == dashAt) dir = dashDir;
            if (dir < 0) out.add(GameAction.LEFT);
            if (dir > 0) out.add(GameAction.RIGHT);
            if (jumpAt >= 0) {
                if (k == jumpAt) {
                    out.add(GameAction.JUMP);
                } else if (k > jumpAt) {
                    if (doubleJump && g.getVelocityY() < 60f && k > jumpAt + 3 && jumpHeld && !g.isOnGround()) {
                        // release so the second press registers
                    } else if (jumpHeld && g.getVelocityY() > 0) {
                        out.add(GameAction.JUMP);
                    } else if (doubleJump && !jumpHeld && !g.isOnGround() && g.getVelocityY() < 60f
                            && g.getAbilities().has(AbilityType.DOUBLE_JUMP)
                            && g.getAbilities().get(AbilityType.DOUBLE_JUMP, DoubleJump.class).isAvailable()) {
                        out.add(GameAction.JUMP);
                    }
                }
            }
            if (dashAt >= 0 && k == dashAt) out.add(GameAction.DASH);
        }
    }

    private final Navigator navigator;
    private final Dangers dangers;
    private final List<float[]> doors = new ArrayList<>();
    private final Set<GameAction> buttons = Maneuver.newButtons();
    private Candidate plan;
    private int planTick;
    private int replanIn;
    private final Map<Enemy, Float> ignoreUntil = new IdentityHashMap<>();
    private String ignoreRoom = "";
    private Enemy lastTarget;
    private float targetTime;
    private boolean attackHeld;
    private boolean recovering;
    private boolean boltHeld;

    Fighter(Navigator navigator, Dangers dangers) {
        this.navigator = navigator;
        this.dangers = dangers;
    }

    // ------------------------------------------------------------------ when to fight

    private static Boss boss(GameWorld world) {
        Boss b = world.getEntities().first(Boss.class);
        return b != null && b.isAlive() ? b : null;
    }

    boolean inBossFight(GameWorld world) {
        Boss b = boss(world);
        return b != null && (b.isAwake() || world.getTileMap().areGatesClosed());
    }

    String bossName(GameWorld world) {
        Boss b = boss(world);
        return b == null ? "" : b.getTitle();
    }

    void bossFight(GameWorld world, Set<GameAction> out) {
        Boss b = boss(world);
        fight(world, b, Mode.BOSS, out);
    }

    /**
     * Knocked into the air with no plan (a hit in mid-jump): steer, Wisp Leap or dash so the fall ends on
     * safe ground instead of spikes or mist. Returns false if a plain fall is already safe.
     */
    boolean recover(GameWorld world, Set<GameAction> out) {
        Player p = world.getPlayer();
        if (p.isOnGround() || navigator.isManeuvering() || p.isDead()) {
            if (recovering) plan = null;
            recovering = false;
            return false;
        }
        if (!recovering) {
            Sandbox sb = navigator.sandbox();
            Player g = sb.reset(p);
            sb.input().releaseAll();
            boolean safe = true;
            for (int k = 0; k < RECOVER_HORIZON && !g.isOnGround(); k++) {
                if (!sb.step()) {
                    safe = false;
                    break;
                }
            }
            if (safe) return false;
            recovering = true;
            plan = null;
        }
        fight(world, null, Mode.RECOVER, out);
        return true;
    }

    /** Engages nearby regular enemies; returns false if nothing needs fighting. */
    boolean skirmish(GameWorld world, Set<GameAction> out) {
        Player p = world.getPlayer();
        if (navigator.isManeuvering()) return false;
        if (!world.getRoomId().equals(ignoreRoom)) {
            ignoreRoom = world.getRoomId();
            ignoreUntil.clear();
            lastTarget = null;
        }
        Enemy target = null;
        float best = Float.MAX_VALUE;
        RoomNav.Segment seg = navigator.nav() == null ? null : navigator.here(p);
        for (Enemy e : world.getEntities().getAll(Enemy.class)) {
            if (!e.isAlive() || e instanceof Boss || !e.isHarmful()) continue;
            float dx = e.centerX() - p.centerX();
            float dy = e.centerY() - p.centerY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            boolean relevant;
            Float ignored = ignoreUntil.get(e);
            if (ignored != null && ignored > world.getTime() && dist > 150f) continue;
            if (e.isFlying()) {
                // moths dive at anyone jumping past them: deal with them before leaping anywhere near
                relevant = dist < 330f;
            } else {
                // a jump (to dodge) must not make us forget the enemy we are fighting
                float reach = e == lastTarget && !p.isOnGround() ? 140f : 40f;
                boolean sameFloor = Math.abs(e.getY() - p.getY()) < reach;
                boolean onSegment = (seg == null && e == lastTarget)
                        || (seg != null && e.centerX() > seg.c0() * 32f - 70f && e.centerX() < (seg.c1() + 1) * 32f + 70f);
                relevant = sameFloor && onSegment && Math.abs(dx) < 230f;
            }
            if (!relevant || !world.getTileMap().hasLineOfSight(p.centerX(), p.centerY(), e.centerX(), e.centerY())) continue;
            if (dist < best) {
                best = dist;
                target = e;
            }
        }
        boolean incoming = projectileIncoming(world, p);
        if (target != lastTarget) {
            lastTarget = target;
            targetTime = 0f;
        } else if (target != null) {
            targetTime += Constants.STEP;
            if (targetTime > 5f && target.getHealth().getCurrent() >= target.getHealth().getMax()) {
                // it will not come to us and we cannot reach it: leave it alone for a while
                ignoreUntil.put(target, world.getTime() + 12f);
                lastTarget = null;
                target = null;
            }
        }
        if (target == null && !incoming) {
            plan = null;
            return false;
        }
        if (!p.isOnGround() && target == null) return false;
        fight(world, target, Mode.SKIRMISH, out);
        return true;
    }

    private boolean projectileIncoming(GameWorld world, Player p) {
        for (GameObject o : world.getEntities().view()) {
            if (!(o instanceof Projectile pr) || pr.getTeam() != Team.ENEMY || pr.isRemoved()) continue;
            for (float t = 0; t < 0.45f; t += 0.05f) {
                float x = pr.getX() + pr.getVelocityX() * t;
                float y = pr.getY() + pr.getVelocityY() * t;
                if (x < p.getX() + p.getWidth() + 6 && x + pr.getWidth() > p.getX() - 6
                        && y < p.getY() + p.getHeight() + 6 && y + pr.getHeight() > p.getY() - 6) return true;
            }
        }
        return false;
    }

    /** Focus to heal when hurt and nothing is around. */
    boolean heal(GameWorld world, Set<GameAction> out) {
        Player p = world.getPlayer();
        boolean can = p.isOnGround() && !p.getHealth().isFull() && p.getSoul().has(Constants.SOUL_COST)
                && !navigator.isManeuvering();
        if (!can) return false;
        for (Enemy e : world.getEntities().getAll(Enemy.class)) {
            if (!e.isAlive()) continue;
            float dx = e.centerX() - p.centerX();
            float dy = e.centerY() - p.centerY();
            if (dx * dx + dy * dy < 420f * 420f) return false;
        }
        if (projectileIncoming(world, p)) return false;
        out.add(GameAction.FOCUS);
        return true;
    }

    // ------------------------------------------------------------------ the fight itself

    private void fight(GameWorld world, Enemy target, Mode mode, Set<GameAction> out) {
        Player p = world.getPlayer();
        out.clear();
        dangers.predict(world, p);
        doors.clear();
        for (DoorData d : world.getRoom().getData().getDoors()) {
            doors.add(new float[] {d.col() * 32f, d.row() * 32f, 32f, DoorData.HEIGHT * 32f});
        }
        if (plan == null || --replanIn <= 0 || planTick >= HORIZON - 6) {
            plan = choose(world, p, target, mode);
            planTick = 0;
            replanIn = REPLAN_EVERY;
        }
        boolean jumpHeld = p.getInput() != null && p.getInput().isDown(GameAction.JUMP);
        plan.buttons(planTick++, p, jumpHeld, out);
        if (target != null && !plan.focus()) strike(world, p, target, out);
    }

    private Candidate choose(GameWorld world, Player p, Enemy target, Mode mode) {
        List<Candidate> cands = mode == Mode.RECOVER ? recoveryCandidates(p) : candidates(p);
        Sandbox sb = navigator.sandbox();
        RoomNav.Segment seg = navigator.here(p);
        Candidate best = null;
        float bestScore = -Float.MAX_VALUE;
        for (Candidate c : cands) {
            float s = evaluate(sb, p, c, target, mode, seg);
            if (plan != null && sameAs(c, plan)) s += 15f; // a little commitment avoids dithering
            if (s > bestScore) {
                bestScore = s;
                best = c;
            }
        }
        return best;
    }

    private static boolean sameAs(Candidate a, Candidate b) {
        return a.dir1() == b.dir1() && a.dir2() == b.dir2() && a.jumpAt() == b.jumpAt() && a.dashAt() == b.dashAt()
                && a.focus() == b.focus();
    }

    private List<Candidate> candidates(Player p) {
        List<Candidate> list = new ArrayList<>();
        boolean dash = p.getAbilities().has(AbilityType.DASH);
        boolean dbl = p.getAbilities().has(AbilityType.DOUBLE_JUMP);
        for (int d = -1; d <= 1; d++) {
            list.add(new Candidate(d, d, 0, -1, false, -1, 0, false));
            list.add(new Candidate(d, d, 0, 0, false, -1, 0, false));
            if (dbl) list.add(new Candidate(d, d, 0, 0, true, -1, 0, false));
            if (d != 0) {
                list.add(new Candidate(d, -d, 14, -1, false, -1, 0, false));
                list.add(new Candidate(0, d, 12, -1, false, -1, 0, false));
                list.add(new Candidate(d, 0, 10, -1, false, -1, 0, false));
                list.add(new Candidate(0, d, 0, 8, false, -1, 0, false));
                if (dash) {
                    list.add(new Candidate(d, d, 0, -1, false, 0, d, false));
                    list.add(new Candidate(d, d, 0, 0, false, 8, d, false));
                }
            }
        }
        if (p.isOnGround() && !p.getHealth().isFull() && p.getSoul().has(Constants.SOUL_COST)) {
            list.add(new Candidate(0, 0, 0, -1, false, -1, 0, true));
        }
        return list;
    }

    private List<Candidate> recoveryCandidates(Player p) {
        List<Candidate> list = new ArrayList<>();
        boolean dash = p.getAbilities().has(AbilityType.DASH);
        for (int d = -1; d <= 1; d++) {
            for (int jump : new int[] {-1, 0, 8, 16}) {
                list.add(new Candidate(d, d, 0, jump, false, -1, 0, false));
                if (dash && d != 0) {
                    list.add(new Candidate(d, d, 0, jump, false, 0, d, false));
                    list.add(new Candidate(d, d, 0, jump, false, 10, d, false));
                }
            }
        }
        return list;
    }

    private float evaluate(Sandbox sb, Player p, Candidate c, Enemy target, Mode mode, RoomNav.Segment seg) {
        Player g = sb.reset(p);
        float invulnerable = p.getHealth().getInvincibleTime() - 0.05f;
        int ticks = c.focus() ? FOCUS_TICKS : (mode == Mode.RECOVER ? RECOVER_HORIZON : HORIZON);
        float score = 0f;
        boolean jumpHeld = false;
        boolean hit = false;
        for (int k = 0; k < ticks; k++) {
            c.buttons(k, g, jumpHeld, buttons);
            jumpHeld = buttons.contains(GameAction.JUMP);
            sb.input().releaseAll();
            for (GameAction a : buttons) sb.input().press(a);
            if (!sb.step()) {
                score -= 3000f;
                hit = true;
                break;
            }
            float t = (k + 1) * DT;
            if (t > invulnerable && dangers.hits(t, g)) {
                score -= 1000f + 25f * (ticks - k);
                hit = true;
                break;
            }
            if (touchesDoor(g)) {
                score -= 2500f; // walking out of the room mid-fight
                break;
            }
            if (mode == Mode.RECOVER && g.isOnGround()) break;
            if (target != null && k < HORIZON && inReach(g, target, t)) score += 5f;
        }
        if (c.focus() && !hit) score += 220f * (p.getHealth().getMax() - p.getHealth().getCurrent());
        if (target != null) {
            float desired = desiredX(g, target);
            score -= Math.abs(g.centerX() - desired) * 0.5f;
        }
        if (mode == Mode.RECOVER) {
            if (!hit && g.isOnGround()) {
                score += 400f;
                if (navigator.nav() != null && navigator.nav().locate(g.centerX(), g.getY(), g.getWidth()) != null) score += 100f;
            }
            score -= Math.abs(g.centerX() - p.centerX()) * 0.1f;
        }
        if (mode == Mode.SKIRMISH && seg != null) {
            float left = seg.c0() * 32f;
            float right = (seg.c1() + 1) * 32f;
            if (g.centerX() < left - 4f || g.centerX() > right + 4f || g.getY() < seg.feetY() - 8f) score -= 600f;
        }
        return score;
    }

    private boolean touchesDoor(Player g) {
        for (float[] d : doors) {
            if (overlap(g.getX(), g.getY(), g.getWidth(), g.getHeight(), d[0], d[1], d[2], d[3])) return true;
        }
        return false;
    }

    /** Where to stand relative to the target: just inside slash range on our side, or under a flyer. */
    private static float desiredX(Player g, Enemy target) {
        if (target.isFlying() && target.centerY() > g.getY() + 60f) return target.centerX();
        float side = g.centerX() < target.centerX() ? -1f : 1f;
        return target.centerX() + side * (target.getWidth() / 2f + 40f);
    }

    /** True if a slash from the ghost's position would reach where the target is predicted to be. */
    private static boolean inReach(Player g, Enemy target, float t) {
        float tx = target.getX() + target.getVelocityX() * Math.min(t, 0.3f);
        float ty = target.getY() + (target.isFlying() ? target.getVelocityY() * Math.min(t, 0.3f) : 0f);
        float tw = target.getWidth();
        float th = target.getHeight();
        float dir = tx + tw / 2f >= g.centerX() ? 1f : -1f;
        float fx = dir > 0 ? g.getX() + g.getWidth() - 6f : g.getX() - 64f;
        if (overlap(fx, g.getY() - 2f, 70f, 48f, tx, ty, tw, th)) return true;
        return overlap(g.centerX() - 30f, g.getY() + g.getHeight() - 10f, 60f, 74f, tx, ty, tw, th);
    }

    private static boolean overlap(float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    /** Slashes (forward, up or down) or casts a Light Bolt when the real target is in reach. */
    private void strike(GameWorld world, Player p, Enemy target, Set<GameAction> out) {
        boolean attackDown = attackHeld;
        attackHeld = false;
        boolean boltDown = boltHeld;
        boltHeld = false;
        if (target instanceof Boss b && (!b.isAwake() || b.getPose().equals("roar"))) return;
        Hitbox t = target.getHitbox();
        int dir = t.centerX() >= p.centerX() ? 1 : -1;
        int planDir = out.contains(GameAction.LEFT) ? -1 : (out.contains(GameAction.RIGHT) ? 1 : 0);
        float fx = dir > 0 ? p.getX() + p.getWidth() - 6f : p.getX() - 64f;
        boolean forward = overlap(fx, p.getY() - 2f, 70f, 48f, t.getX(), t.getY(), t.getWidth(), t.getHeight());
        boolean up = overlap(p.centerX() - 30f, p.getY() + p.getHeight() - 10f, 60f, 74f, t.getX(), t.getY(), t.getWidth(), t.getHeight());
        boolean down = !p.isOnGround()
                && overlap(p.centerX() - 30f, p.getY() - 66f, 60f, 74f, t.getX(), t.getY(), t.getWidth(), t.getHeight());
        boolean ready = p.getAttack().isReady() && !attackDown && p.canAttack();
        if (ready && (up || down || forward)) {
            if (up && !forward) {
                out.add(GameAction.UP);
            } else if (down && !forward) {
                out.add(GameAction.DOWN);
            } else {
                if (planDir == -dir) return;
                if (p.getFacing().dx() != dir) {
                    out.remove(dir > 0 ? GameAction.LEFT : GameAction.RIGHT);
                    out.add(dir > 0 ? GameAction.RIGHT : GameAction.LEFT);
                }
            }
            out.add(GameAction.ATTACK);
            attackHeld = true;
            return;
        }
        // Light Bolt: worth it when healing is not needed and the target is level with us
        boolean bolt = p.getAbilities().has(AbilityType.LIGHT_BOLT) && p.getSoul().has(Constants.SOUL_COST)
                && (p.getHealth().getCurrent() >= p.getHealth().getMax() - 1 || p.getSoul().has(Constants.SOUL_COST * 2))
                && p.getFacing().dx() == dir && !boltDown && target.getHealth().getCurrent() > p.getNailDamage();
        if (bolt) {
            float by = p.getY() + p.getHeight() * 0.55f;
            boolean level = by + 8f > t.getY() && by - 8f < t.getY() + t.getHeight();
            boolean clear = world.getTileMap().hasLineOfSight(p.centerX(), by, t.centerX(), by);
            if (level && clear && Math.abs(t.centerX() - p.centerX()) < 520f) {
                out.add(GameAction.BOLT);
                boltHeld = true;
            }
        }
    }

    /** Name of the enemy being fought, or "" when only dodging. */
    String targetName() {
        return lastTarget == null ? "" : lastTarget.getDisplayName();
    }
}
