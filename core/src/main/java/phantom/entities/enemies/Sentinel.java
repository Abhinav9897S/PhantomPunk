package phantom.entities.enemies;

import com.badlogic.gdx.graphics.Color;

import phantom.ai.Behaviours;
import phantom.ai.EnemyState;
import phantom.ai.StateMachine;
import phantom.audio.SoundId;
import phantom.entities.DamageInfo;
import phantom.entities.ProjectileKind;
import phantom.entities.Team;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * Lantern Sentinel: a rooted stone totem that charges up and fires a glowing orb
 * whenever it has line of sight to the player.
 * <p>OOP concept: subclass of {@link Enemy}; overrides {@link #getKnockback()} to stay put.</p>
 */
public final class Sentinel extends Enemy {
    private static final Color GLOW = new Color(1f, 0.6f, 0.3f, 1f);
    private static final float RANGE = 460f;
    private static final float ORB_SPEED = 300f;

    private final StateMachine<Sentinel> ai = new StateMachine<>(this, EnemyState.IDLE);

    public Sentinel(float x, float y) {
        super("Lantern Sentinel", x, y, 30, 50, 20, 1, 6);
        ai.on(EnemyState.IDLE, Sentinel::watch)
          .on(EnemyState.ALERT, Sentinel::charge)
          .on(EnemyState.RECOVER, Behaviours.pause(1.5f, EnemyState.IDLE))
          .on(EnemyState.STUNNED, Behaviours.stunned(0.2f, EnemyState.RECOVER));
    }

    private void watch(WorldContext world, float dt) {
        stepPhysics(world, dt);
        if (Behaviours.canSeePlayer(this, world, RANGE)) {
            face(world.getPlayer().centerX());
            ai.change(EnemyState.ALERT);
        }
    }

    private void charge(WorldContext world, float dt) {
        stepPhysics(world, dt);
        face(world.getPlayer().centerX());
        if (ai.getTimeInState() < 0.75f) return;
        float sx = centerX() + getFacing().dx() * 6f;
        float sy = getY() + 35f;
        float dx = world.getPlayer().centerX() - sx;
        float dy = world.getPlayer().centerY() - sy;
        float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
        world.spawnProjectile(ProjectileKind.ORB, Team.ENEMY, sx, sy, dx / len * ORB_SPEED, dy / len * ORB_SPEED, 1);
        world.getEvents().publish(new Events.Sfx(SoundId.SHOOT));
        ai.change(EnemyState.RECOVER);
    }

    @Override
    protected void onHurt(DamageInfo info, WorldContext world) {
        if (!ai.is(EnemyState.ALERT)) ai.change(EnemyState.STUNNED);
    }

    @Override
    protected float getKnockback() { return 0f; }

    @Override
    public StateMachine<Sentinel> getAi() { return ai; }

    @Override
    protected Color getGlowColor() { return GLOW; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        drawSprite(ctx, alpha, ctx.art().sheet("sentinel"), ai.is(EnemyState.ALERT) ? 1 : 0, 1f, 1f, 0f);
        if (!isAlive()) return;
        float charge = ai.is(EnemyState.ALERT) ? Math.min(1f, ai.getTimeInState() / 0.75f) : 0f;
        float cx = renderCenterX(alpha) + getFacing().dx() * 3f;
        float cy = renderY(alpha) + 35f;
        ctx.glow(cx, cy, 8f + 14f * charge, GLOW, 0.6f + 0.4f * charge);
        ctx.light(cx, cy, 90f + 120f * charge, GLOW, 0.7f);
    }
}
