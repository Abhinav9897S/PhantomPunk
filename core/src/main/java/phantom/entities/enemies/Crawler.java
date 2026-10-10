package phantom.entities.enemies;

import com.badlogic.gdx.graphics.Color;

import phantom.ai.Behaviours;
import phantom.ai.EnemyState;
import phantom.ai.StateMachine;
import phantom.entities.DamageInfo;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * Mosscrawler: a shelled grub that patrols its platform and turns at walls and ledges.
 * <p>OOP concept: concrete subclass of {@link Enemy}; its AI is two states (PATROL, STUNNED).</p>
 */
public final class Crawler extends Enemy {
    private static final Color GLOW = new Color(0.4f, 1f, 0.75f, 1f);
    private static final float SPEED = 62f;

    private final StateMachine<Crawler> ai = new StateMachine<>(this, EnemyState.PATROL);

    public Crawler(float x, float y) {
        super("Mosscrawler", x, y, 30, 20, 15, 1, 4);
        ai.on(EnemyState.PATROL, Crawler::patrol)
          .on(EnemyState.STUNNED, Behaviours.stunned(0.25f, EnemyState.PATROL));
    }

    private void patrol(WorldContext world, float dt) {
        turnAtEdges(world);
        setVelocityX(getFacing().dx() * SPEED);
        stepPhysics(world, dt);
    }

    @Override
    protected void onHurt(DamageInfo info, WorldContext world) {
        ai.change(EnemyState.STUNNED);
    }

    @Override
    public StateMachine<Crawler> getAi() { return ai; }

    @Override
    protected Color getGlowColor() { return GLOW; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        int frame = getAnimator().frame(ctx.art().sheet("crawler").count(), 7f, true);
        drawSprite(ctx, alpha, ctx.art().sheet("crawler"), frame, 1f, 1f, 0f);
        if (isAlive()) {
            float ex = renderCenterX(alpha) + getFacing().dx() * 15f;
            ctx.glow(ex, renderY(alpha) + 8f, 7f, GLOW, 0.9f);
            ctx.light(ex, renderY(alpha) + 8f, 70f, GLOW, 0.5f);
        }
    }
}
