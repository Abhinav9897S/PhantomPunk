package phantom.autoplay;

import java.util.ArrayList;
import java.util.List;

import phantom.ai.EnemyState;
import phantom.entities.GameObject;
import phantom.entities.Projectile;
import phantom.entities.Team;
import phantom.entities.bosses.Boss;
import phantom.entities.bosses.BossAttack;
import phantom.entities.bosses.ChargeAttack;
import phantom.entities.bosses.LeapAttack;
import phantom.entities.enemies.Brute;
import phantom.entities.enemies.Enemy;
import phantom.entities.enemies.Flutterer;
import phantom.entities.player.Player;
import phantom.entities.props.FallingRock;
import phantom.entities.props.GroundEruption;
import phantom.util.Constants;
import phantom.world.GameWorld;
import phantom.world.TileMap;

/**
 * Short-term danger forecast for the autoplay AI: where every enemy body, projectile, eruption and
 * falling rock will be over the next second or so, extrapolated from what can be seen now (velocities,
 * an attack's wind-up, a boss leap aimed at Phantom's current spot).
 * <p>OOP concept: a small model object shared by the {@link Fighter} and the {@link Navigator}.</p>
 */
final class Dangers {
    /** A predicted moving box that hurts on contact. */
    static final class Threat {
        float x, y, w, h, vx, vy, gravity, delay, from, to, floorY, minX, maxX;

        Threat(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.to = 99f;
            this.floorY = -1e6f;
            this.minX = -1e6f;
            this.maxX = 1e6f;
        }

        boolean hits(float t, float px, float py, float pw, float ph) {
            if (t < from || t > to) return false;
            float tau = Math.max(0f, t - delay);
            float bx = Math.max(minX, Math.min(maxX, x + vx * tau));
            float by = Math.max(floorY, y + vy * tau - 0.5f * gravity * tau * tau);
            return px < bx + w && px + pw > bx && py < by + h && py + ph > by;
        }
    }

    private final List<Threat> threats = new ArrayList<>();

    /** Re-predicts every danger in the room as seen from player {@code p} right now. */
    void predict(GameWorld world, Player p) {
        threats.clear();
        TileMap map = world.getTileMap();
        for (GameObject o : world.getEntities().view()) {
            if (o.isRemoved()) continue;
            if (o instanceof Boss b) {
                bossThreat(b, p, map);
            } else if (o instanceof Enemy e) {
                if (e.isAlive() && e.isHarmful()) enemyThreat(e, p);
            } else if (o instanceof Projectile pr) {
                if (pr.getTeam() == Team.ENEMY && !pr.isDying()) {
                    Threat th = new Threat(pr.getX() - 3f, pr.getY() - 3f, pr.getWidth() + 6f, pr.getHeight() + 6f);
                    th.vx = pr.getVelocityX();
                    th.vy = pr.getVelocityY();
                    if (pr.getKind().isHoming()) {
                        th.x -= 10f;
                        th.y -= 10f;
                        th.w += 20f;
                        th.h += 20f;
                    }
                    threats.add(th);
                }
            } else if (o instanceof GroundEruption ge) {
                Threat th = new Threat(ge.getX() - 4f, ge.getY(), ge.getHitbox().getWidth() + 8f, ge.getHitbox().getHeight());
                th.from = ge.getTimeUntilActive() - 0.05f;
                th.to = ge.getTimeUntilHarmless();
                threats.add(th);
            } else if (o instanceof FallingRock rock) {
                if (rock.isFalling() || rock.getTimeUntilFall() >= 0f) {
                    Threat th = new Threat(rock.getX() - 4f, rock.getY(), rock.getHitbox().getWidth() + 8f, rock.getHitbox().getHeight());
                    th.delay = rock.isFalling() ? 0f : rock.getTimeUntilFall();
                    th.vy = rock.getVelocityY();
                    th.gravity = Constants.GRAVITY;
                    th.floorY = -200f;
                    threats.add(th);
                }
            }
        }
    }

    private void enemyThreat(Enemy e, Player p) {
        Threat th = new Threat(e.getX() - 2f, e.getY() - 2f, e.getWidth() + 4f, e.getHeight() + 4f);
        th.vx = e.getVelocityX();
        th.vy = e.isFlying() ? e.getVelocityY() : 0f;
        if (e instanceof Brute br && br.getAi().is(EnemyState.ALERT)) {
            th.delay = Math.max(0f, 0.55f - br.getAi().getTimeInState());
            th.vx = br.getFacing().dx() * 360f;
            th.vy = 0f;
        } else if (e instanceof Flutterer f && f.getAi().is(EnemyState.ALERT)) {
            th.delay = Math.max(0f, 0.45f - f.getAi().getTimeInState());
            float dx = p.centerX() - e.centerX();
            float dy = p.centerY() - e.centerY();
            float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
            th.vx = dx / len * 400f;
            th.vy = dy / len * 400f;
            th.to = th.delay + 0.85f;
            threats.add(th);
            Threat hover = new Threat(e.getX() - 2f, e.getY() - 2f, e.getWidth() + 4f, e.getHeight() + 4f);
            hover.to = th.delay;
            threats.add(hover);
            return;
        }
        if (e.isFlying()) th.to = 0.5f; // flight paths curve; only trust the near future
        threats.add(th);
    }

    private void bossThreat(Boss b, Player p, TileMap map) {
        if (!b.isAlive() || !b.isAwake()) return;
        float inset = 3f;
        Threat body = new Threat(b.getX() + inset, b.getY(), b.getWidth() - 2 * inset, b.getHeight());
        // the arena walls stop charges
        body.minX = arenaWall(map, b, -1);
        body.maxX = arenaWall(map, b, 1) - b.getWidth();
        BossAttack a = b.getCurrentAttack();
        if (b.isHarmful() || b.getFade() > 0.2f) {
            if (a instanceof ChargeAttack ch) {
                body.delay = ch.getWindupLeft();
                body.vx = ch.getDirection() * ch.getSpeed();
            } else if (a instanceof LeapAttack leap) {
                if (!leap.isAirborne() && leap.getWindupLeft() > 0f) {
                    // it will jump to where we stand now and land there
                    float airTime = 2f * leap.getJumpVelocity() / Constants.GRAVITY;
                    Threat land = new Threat(p.centerX() - b.getWidth() / 2f, b.getY(), b.getWidth(), b.getHeight());
                    land.from = leap.getWindupLeft() + airTime * 0.6f;
                    threats.add(land);
                    if (leap.hasShockwaves()) shockwaves(land.x, land.y, b.getWidth(), land.from + airTime * 0.4f);
                    body.to = leap.getWindupLeft() + 0.1f;
                } else if (leap.isAirborne()) {
                    body.vx = b.getVelocityX();
                    body.vy = b.getVelocityY();
                    body.gravity = Constants.GRAVITY;
                    body.floorY = floorBelow(map, b);
                    if (leap.hasShockwaves()) {
                        float tLand = timeToLand(b.getY(), b.getVelocityY(), body.floorY);
                        float lx = b.getX() + b.getVelocityX() * tLand;
                        shockwaves(lx, body.floorY, b.getWidth(), tLand);
                    }
                }
            } else if (!b.isFlying()) {
                body.vx = b.getVelocityX();
            } else {
                body.vx = b.getVelocityX();
                body.vy = b.getVelocityY();
                body.to = 0.6f;
                body.x -= 6f;
                body.w += 12f;
                body.y -= 6f;
                body.h += 12f;
            }
            threats.add(body);
        }
    }

    private void shockwaves(float bossX, float floorY, float bossW, float at) {
        for (int side = -1; side <= 1; side += 2) {
            float sx = side < 0 ? bossX - 10f - 13f : bossX + bossW + 10f - 13f;
            Threat w = new Threat(sx - 4f, floorY + 15f - 15f, 26f + 8f, 30f);
            w.delay = at;
            w.from = at;
            w.to = at + 1.6f;
            w.vx = side * 420f;
            threats.add(w);
        }
    }

    private static float timeToLand(float y, float vy, float floorY) {
        float g = Constants.GRAVITY;
        float disc = vy * vy + 2f * g * (y - floorY);
        if (disc < 0) return 0f;
        return (vy + (float) Math.sqrt(disc)) / g;
    }

    private static float floorBelow(TileMap map, Boss b) {
        int c = TileMap.col(b.centerX());
        for (int r = TileMap.row(b.getY()); r > 0; r--) {
            if (map.isSolid(c, r - 1)) return r * 32f;
        }
        return 0f;
    }

    private static float arenaWall(TileMap map, Boss b, int dir) {
        int r = TileMap.row(b.getY() + 4f);
        int c = TileMap.col(b.centerX());
        while (c > 0 && c < map.getCols() - 1 && !map.isSolid(c, r)) c += dir;
        return dir < 0 ? (c + 1) * 32f : c * 32f;
    }

    /** True if a body at (x, y, w, h) would touch any predicted danger {@code t} seconds from now. */
    boolean hits(float t, float x, float y, float w, float h) {
        for (Threat th : threats) {
            if (th.hits(t, x, y, w, h)) return true;
        }
        return false;
    }

    /** True if the ghost's body (slightly shrunk) touches a danger at time {@code t}. */
    boolean hits(float t, Player g) {
        return hits(t, g.getX() + 2f, g.getY() + 2f, g.getWidth() - 4f, g.getHeight() - 4f);
    }
}
