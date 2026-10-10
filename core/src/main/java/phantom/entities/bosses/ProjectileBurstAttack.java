package phantom.entities.bosses;

import phantom.audio.SoundId;
import phantom.entities.ProjectileKind;
import phantom.entities.Team;
import phantom.events.Events;
import phantom.world.WorldContext;

/**
 * Fires waves of projectiles: either aimed fans at the player or full rings.
 * <p>OOP concept: {@link BossAttack} strategy reused by two bosses with different projectile kinds.</p>
 */
public final class ProjectileBurstAttack implements BossAttack {
    private static final float TELEGRAPH = 0.55f;

    private final ProjectileKind kind;
    private final int count;
    private final float spreadDegrees;
    private final float speed;
    private final boolean aimed;
    private final int waves;
    private final float interval;
    private float time;
    private int fired;

    public ProjectileBurstAttack(ProjectileKind kind, int count, float spreadDegrees, float speed,
                                 boolean aimed, int waves, float interval) {
        this.kind = kind;
        this.count = count;
        this.spreadDegrees = spreadDegrees;
        this.speed = speed;
        this.aimed = aimed;
        this.waves = waves;
        this.interval = interval;
    }

    @Override
    public void begin(Boss boss, WorldContext world) {
        time = 0;
        fired = 0;
        boss.face(world.getPlayer().centerX());
    }

    @Override
    public boolean update(Boss boss, WorldContext world, float dt) {
        time += dt;
        boss.setVelocityX(0);
        if (!boss.isFlying()) boss.stepPhysics(world, dt);
        else boss.setVelocity(0, 0);
        if (fired < waves && time >= TELEGRAPH + fired * interval) {
            fire(boss, world, fired);
            fired++;
        }
        return fired >= waves && time >= TELEGRAPH + waves * interval + 0.35f;
    }

    private void fire(Boss boss, WorldContext world, int wave) {
        float sx = boss.centerX();
        float sy = boss.getY() + boss.getHeight() * 0.62f;
        double base;
        double step;
        if (aimed) {
            base = Math.atan2(world.getPlayer().centerY() - sy, world.getPlayer().centerX() - sx)
                    - Math.toRadians(spreadDegrees) / 2;
            step = count > 1 ? Math.toRadians(spreadDegrees) / (count - 1) : 0;
            if (count == 1) base += Math.toRadians(spreadDegrees) / 2;
        } else {
            step = Math.PI * 2 / count;
            base = wave * step * 0.5;
        }
        for (int i = 0; i < count; i++) {
            double a = base + step * i;
            world.spawnProjectile(kind, Team.ENEMY, sx, sy, (float) Math.cos(a) * speed, (float) Math.sin(a) * speed, 1);
        }
        world.getEvents().publish(new Events.Sfx(SoundId.SHOOT));
    }

    @Override
    public String getPose() {
        return "cast";
    }

    @Override
    public boolean isTelegraphing() {
        return time < TELEGRAPH;
    }
}
