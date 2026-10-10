package phantom.entities.enemies;

import com.badlogic.gdx.graphics.Color;

import phantom.ai.Behaviours;
import phantom.ai.EnemyState;
import phantom.ai.StateMachine;
import phantom.audio.SoundId;
import phantom.entities.DamageInfo;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * Husk Brute: a slow armoured beast that charges when it notices the player.
 * Charging into a wall, or taking three quick hits, staggers it.
 * <p>OOP concept: subclass of {@link Enemy}; the richest state machine of the regular enemies.</p>
 */
public final class Brute extends Enemy {
    private static final Color GLOW = new Color(1f, 0.45f, 0.35f, 1f);
    private static final float WALK = 45f;
    private static final float CHARGE = 360f;

    private final StateMachine<Brute> ai = new StateMachine<>(this, EnemyState.PATROL);
    private int recentHits;
    private float hitWindow;

    public Brute(float x, float y) {
        super("Husk Brute", x, y, 52, 50, 50, 1, 12);
        ai.on(EnemyState.PATROL, Brute::patrol)
          .on(EnemyState.ALERT, Brute::windUp)
          .on(EnemyState.ATTACK, Brute::charge)
          .on(EnemyState.RECOVER, Behaviours.pause(0.8f, EnemyState.PATROL))
          .on(EnemyState.STUNNED, Behaviours.stunned(1.2f, EnemyState.PATROL));
    }

    @Override
    public void update(float dt, WorldContext world) {
        if (hitWindow > 0) hitWindow -= dt; else recentHits = 0;
        super.update(dt, world);
    }

    private void patrol(WorldContext world, float dt) {
        turnAtEdges(world);
        setVelocityX(getFacing().dx() * WALK);
        stepPhysics(world, dt);
        if (noticesPlayer(world)) ai.change(EnemyState.ALERT);
    }

    private boolean noticesPlayer(WorldContext world) {
        if (world.getPlayer() == null || world.getPlayer().isDead()) return false;
        float dy = Math.abs(world.getPlayer().getY() - getY());
        float dx = Math.abs(world.getPlayer().centerX() - centerX());
        return dx < 330f && dy < 80f && Behaviours.canSeePlayer(this, world, 360f);
    }

    private void windUp(WorldContext world, float dt) {
        face(world.getPlayer().centerX());
        setVelocityX(0);
        stepPhysics(world, dt);
        if (ai.getTimeInState() > 0.55f) {
            world.shake(0.2f);
            world.getEvents().publish(new Events.Sfx(SoundId.BOSS_ROAR));
            ai.change(EnemyState.ATTACK);
        }
    }

    private void charge(WorldContext world, float dt) {
        setVelocityX(getFacing().dx() * CHARGE);
        stepPhysics(world, dt);
        if (world.getRandom().nextFloat() < 0.5f) world.getEffects().dust(centerX() - getFacing().dx() * 20, getY(), 1);
        if (getContacts().isHitWall()) {
            world.shake(0.45f);
            world.getEffects().debris(centerX() + getFacing().dx() * 26, getY() + 10, 10, 40, GLOW, 8);
            world.getEvents().publish(new Events.Sfx(SoundId.BOSS_SLAM));
            setVelocityX(-getFacing().dx() * 120f);
            ai.change(EnemyState.STUNNED);
        } else if (!world.getCollision().hasGroundAhead(this, world.getTileMap(), getFacing()) || ai.getTimeInState() > 1.6f) {
            ai.change(EnemyState.RECOVER);
        }
    }

    @Override
    protected void onHurt(DamageInfo info, WorldContext world) {
        recentHits++;
        hitWindow = 1.2f;
        if (recentHits >= 3 && !ai.is(EnemyState.STUNNED)) {
            recentHits = 0;
            ai.change(EnemyState.STUNNED);
        } else if (ai.is(EnemyState.PATROL)) {
            face(info.sourceX());
            ai.change(EnemyState.ALERT);
        }
    }

    @Override
    protected float getKnockback() {
        return ai.is(EnemyState.ATTACK) ? 0f : 70f;
    }

    public boolean isStaggered() { return ai.is(EnemyState.STUNNED); }

    @Override
    public StateMachine<Brute> getAi() { return ai; }

    @Override
    protected Color getGlowColor() { return GLOW; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        var sheet = ctx.art().sheet("brute");
        int frame;
        float sx = 1f;
        float sy = 1f;
        if (ai.is(EnemyState.ATTACK)) {
            frame = 4 + getAnimator().frame(2, 12f, true);
        } else if (ai.is(EnemyState.ALERT)) {
            frame = 4;
            float t = ai.getTimeInState() / 0.55f;
            sx = 1f + 0.12f * t;
            sy = 1f - 0.1f * t;
        } else if (ai.is(EnemyState.STUNNED)) {
            frame = 0;
            sy = 0.92f + 0.03f * (float) Math.sin(ctx.time() * 20f);
        } else {
            frame = getAnimator().frame(4, 5f, true);
        }
        drawSprite(ctx, alpha, sheet, frame, sx, sy, 0f);
        if (!isAlive()) return;
        float ex = renderCenterX(alpha) + getFacing().dx() * (ai.is(EnemyState.ATTACK) ? 32f : 27f);
        float ey = renderY(alpha) + (ai.is(EnemyState.ATTACK) ? 14f : 24f);
        float intensity = ai.is(EnemyState.STUNNED) ? 0.2f : (ai.is(EnemyState.ALERT) || ai.is(EnemyState.ATTACK) ? 1f : 0.6f);
        ctx.glow(ex, ey, 7f, GLOW, intensity);
        ctx.light(ex, ey, 90f, GLOW, 0.5f * intensity);
    }
}
