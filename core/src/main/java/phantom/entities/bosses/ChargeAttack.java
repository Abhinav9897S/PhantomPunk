package phantom.entities.bosses;

import phantom.audio.SoundId;
import phantom.events.Events;
import phantom.world.WorldContext;

/**
 * Telegraphed horizontal charge; slamming into a wall shakes the arena.
 * <p>OOP concept: a {@link BossAttack} strategy, configured by constructor parameters.</p>
 */
public final class ChargeAttack implements BossAttack {
    private final float telegraph;
    private final float speed;
    private final float maxTime;
    private float time;
    private float dir;

    public ChargeAttack(float telegraph, float speed, float maxTime) {
        this.telegraph = telegraph;
        this.speed = speed;
        this.maxTime = maxTime;
    }

    @Override
    public void begin(Boss boss, WorldContext world) {
        time = 0;
        boss.face(world.getPlayer().centerX());
        dir = boss.getFacing().dx();
    }

    @Override
    public boolean update(Boss boss, WorldContext world, float dt) {
        time += dt;
        if (time < telegraph) {
            boss.setVelocityX(0);
            boss.stepPhysics(world, dt);
            return false;
        }
        boss.setVelocityX(dir * speed);
        boss.stepPhysics(world, dt);
        if (world.getRandom().nextFloat() < 0.6f) world.getEffects().dust(boss.centerX() - dir * boss.getWidth() * 0.4f, boss.getY(), 1);
        if (boss.getContacts().isHitWall()) {
            world.shake(0.7f);
            world.getEffects().debris(dir > 0 ? boss.getX() + boss.getWidth() : boss.getX(), boss.getY() + 20, 12,
                    boss.getHeight() * 0.6f, com.badlogic.gdx.graphics.Color.GRAY, 12);
            world.getEvents().publish(new Events.Sfx(SoundId.BOSS_SLAM));
            boss.setVelocityX(-dir * 160f);
            return true;
        }
        return time > telegraph + maxTime;
    }

    @Override
    public String getPose() {
        return time < telegraph ? "windup" : "charge";
    }

    @Override
    public boolean isTelegraphing() {
        return time < telegraph;
    }
}
