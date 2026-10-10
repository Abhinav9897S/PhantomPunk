package phantom.entities.enemies;

import com.badlogic.gdx.graphics.Color;

import phantom.ai.Behaviours;
import phantom.ai.EnemyState;
import phantom.ai.StateMachine;
import phantom.entities.DamageInfo;
import phantom.render.RenderContext;
import phantom.util.GameMath;
import phantom.world.WorldContext;

/**
 * Duskmoth: hovers on a sine-wave path, then telegraphs and dives at the player.
 * <p>OOP concept: subclass of {@link Enemy} with a five-state machine
 * (IDLE, ALERT, ATTACK, RECOVER, STUNNED); overrides {@link #isFlying()}.</p>
 */
public final class Flutterer extends Enemy {
    private static final Color GLOW = new Color(0.95f, 0.75f, 1f, 1f);
    private static final float SIGHT = 280f;
    private static final float DIVE_SPEED = 400f;

    private final StateMachine<Flutterer> ai = new StateMachine<>(this, EnemyState.IDLE);
    private final float homeX;
    private final float homeY;
    private final float phase;

    public Flutterer(float x, float y) {
        super("Duskmoth", x, y + 40f, 26, 22, 10, 1, 3);
        homeX = x;
        homeY = y + 40f;
        phase = (x * 0.13f) % 6.28f;
        ai.on(EnemyState.IDLE, Flutterer::hover)
          .on(EnemyState.ALERT, Flutterer::telegraph)
          .on(EnemyState.ATTACK, Flutterer::dive)
          .on(EnemyState.RECOVER, Flutterer::returnHome)
          .on(EnemyState.STUNNED, Flutterer::reel);
    }

    private void hover(WorldContext world, float dt) {
        float t = world.getTime() + phase;
        float tx = homeX + (float) Math.sin(t * 0.9f) * 48f;
        float ty = homeY + (float) Math.sin(t * 2.2f) * 18f;
        seek(tx, ty, 120f, dt);
        stepPhysics(world, dt);
        if (Behaviours.canSeePlayer(this, world, SIGHT)) ai.change(EnemyState.ALERT);
    }

    private void telegraph(WorldContext world, float dt) {
        face(world.getPlayer().centerX());
        setVelocity(GameMath.approach(getVelocityX(), 0, 600 * dt), GameMath.approach(getVelocityY(), 60f, 600 * dt));
        stepPhysics(world, dt);
        if (ai.getTimeInState() > 0.45f) {
            float dx = world.getPlayer().centerX() - centerX();
            float dy = world.getPlayer().centerY() - centerY();
            float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
            setVelocity(dx / len * DIVE_SPEED, dy / len * DIVE_SPEED);
            ai.change(EnemyState.ATTACK);
        }
    }

    private void dive(WorldContext world, float dt) {
        stepPhysics(world, dt);
        if (getContacts().isHitWall() || getContacts().isOnGround() || getContacts().isHitCeiling()
                || ai.getTimeInState() > 0.85f) {
            ai.change(EnemyState.RECOVER);
        }
    }

    private void returnHome(WorldContext world, float dt) {
        seek(homeX, homeY, 110f, dt);
        stepPhysics(world, dt);
        if (ai.getTimeInState() > 1.3f) ai.change(EnemyState.IDLE);
    }

    private void reel(WorldContext world, float dt) {
        setVelocity(getVelocityX() * 0.9f, getVelocityY() * 0.9f);
        stepPhysics(world, dt);
        if (ai.getTimeInState() > 0.3f) ai.change(EnemyState.RECOVER);
    }

    private void seek(float tx, float ty, float speed, float dt) {
        float dx = tx - centerX();
        float dy = ty - centerY();
        float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
        float want = Math.min(speed, len * 3f);
        setVelocity(GameMath.approach(getVelocityX(), dx / len * want, 500 * dt),
                GameMath.approach(getVelocityY(), dy / len * want, 500 * dt));
        if (Math.abs(dx) > 4) face(tx);
    }

    @Override
    protected void onHurt(DamageInfo info, WorldContext world) {
        float away = info.awayFrom(centerX());
        setVelocity(away * 260f, (centerY() - info.sourceY()) > 0 ? 160f : -160f);
        ai.change(EnemyState.STUNNED);
    }

    @Override
    public boolean isFlying() { return true; }

    @Override
    public StateMachine<Flutterer> getAi() { return ai; }

    @Override
    protected Color getGlowColor() { return GLOW; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        boolean frantic = ai.is(EnemyState.ALERT) || ai.is(EnemyState.ATTACK);
        int frames = ctx.art().sheet("flutterer").count();
        int frame = getAnimator().frame(frames, frantic ? 22f : 11f, true);
        float shake = ai.is(EnemyState.ALERT) ? (float) Math.sin(ctx.time() * 70f) * 2f : 0f;
        drawSprite(ctx, alpha, ctx.art().sheet("flutterer"), frame, 1f, 1f, shake * 2f);
        if (isAlive()) {
            float cx = renderCenterX(alpha) + getFacing().dx() * 2f;
            float cy = renderY(alpha) + 14f;
            float glow = frantic ? 1f : 0.7f;
            ctx.glow(cx - 3f, cy, 5f, GLOW, glow);
            ctx.glow(cx + 3f, cy, 5f, GLOW, glow);
            ctx.light(cx, cy, 80f, GLOW, 0.4f);
        }
    }
}
