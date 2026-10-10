package phantom.entities.bosses;

import phantom.audio.SoundId;
import phantom.entities.ProjectileKind;
import phantom.entities.Team;
import phantom.events.Events;
import phantom.util.Constants;
import phantom.world.WorldContext;

/**
 * Crouch, leap towards the player and slam down, optionally sending shockwaves along the floor.
 * <p>OOP concept: {@link BossAttack} strategy with an internal mini state (windup, air, landed).</p>
 */
public final class LeapAttack implements BossAttack {
    private static final float WINDUP = 0.45f;
    private final float jumpVelocity;
    private final boolean shockwaves;
    private float time;
    private boolean airborne;
    private boolean landed;
    private float landTime;

    public LeapAttack(float jumpVelocity, boolean shockwaves) {
        this.jumpVelocity = jumpVelocity;
        this.shockwaves = shockwaves;
    }

    @Override
    public void begin(Boss boss, WorldContext world) {
        time = 0;
        airborne = false;
        landed = false;
        boss.face(world.getPlayer().centerX());
    }

    @Override
    public boolean update(Boss boss, WorldContext world, float dt) {
        time += dt;
        if (!airborne) {
            boss.setVelocityX(0);
            boss.stepPhysics(world, dt);
            if (time >= WINDUP) launch(boss, world);
            return false;
        }
        if (!landed) {
            boss.stepPhysics(world, dt);
            if (boss.isOnGround() && time > WINDUP + 0.1f) land(boss, world);
            return false;
        }
        boss.setVelocityX(0);
        boss.stepPhysics(world, dt);
        return time - landTime > 0.35f;
    }

    private void launch(Boss boss, WorldContext world) {
        airborne = true;
        float airTime = 2f * jumpVelocity / Constants.GRAVITY;
        float dx = world.getPlayer().centerX() - boss.centerX();
        boss.setVelocity(dx / airTime, jumpVelocity);
    }

    private void land(Boss boss, WorldContext world) {
        landed = true;
        landTime = time;
        world.shake(0.8f);
        world.getEffects().dust(boss.centerX(), boss.getY(), 24);
        world.getEvents().publish(new Events.Sfx(SoundId.BOSS_SLAM));
        if (shockwaves) {
            float y = boss.getY() + 15f;
            world.spawnProjectile(ProjectileKind.SHOCKWAVE, Team.ENEMY, boss.getX() - 10, y, -420f, 0f, 1);
            world.spawnProjectile(ProjectileKind.SHOCKWAVE, Team.ENEMY, boss.getX() + boss.getWidth() + 10, y, 420f, 0f, 1);
        }
    }

    @Override
    public String getPose() {
        if (!airborne) return "windup";
        return landed ? "idle" : "leap";
    }

    @Override
    public boolean isTelegraphing() {
        return !airborne;
    }
}
