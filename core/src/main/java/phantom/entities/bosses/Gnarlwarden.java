package phantom.entities.bosses;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

import phantom.render.RenderContext;
import phantom.util.GameMath;
import phantom.world.WorldContext;

/**
 * Boss 1 - the Gnarlwarden, Keeper of the Roots: a hulking beast of living roots.
 * Phase 1 charges, leaps and raises root spikes; phase 2 adds shockwaves and falling rocks.
 * <p>OOP concept: concrete {@link Boss}; behaviour comes from composing attack strategies.</p>
 */
public final class Gnarlwarden extends Boss {
    private static final Color GLOW = new Color(0.55f, 1f, 0.7f, 1f);

    private final List<BossAttack> phase1 = List.of(
            new ChargeAttack(0.65f, 520f, 1.6f),
            new LeapAttack(1050f, false),
            new EruptionAttack(3, 95f));
    private final List<BossAttack> phase2 = List.of(
            new ChargeAttack(0.45f, 620f, 1.6f),
            new LeapAttack(1150f, true),
            new EruptionAttack(5, 80f),
            new DebrisRainAttack(7));

    public Gnarlwarden(float x, float y) {
        super("gnarlwarden", "Gnarlwarden", "Keeper of the Roots", x - 60f, y, 120f, 96f, 150, 80, 0.5f);
    }

    @Override
    protected List<BossAttack> attacksFor(int phase) {
        return phase == 0 ? phase1 : phase2;
    }

    @Override
    protected void reposition(WorldContext world, float dt) {
        float dx = world.getPlayer().centerX() - centerX();
        face(world.getPlayer().centerX());
        float target = Math.abs(dx) > 220f ? Math.signum(dx) * 80f : 0f;
        setVelocityX(GameMath.approach(getVelocityX(), target, 600f * dt));
        stepPhysics(world, dt);
    }

    @Override
    protected float cooldownFor(int phase) {
        return phase == 0 ? 1.1f : 0.7f;
    }

    @Override
    public boolean isFinal() { return false; }

    @Override
    protected Color getGlowColor() { return GLOW; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        var sheet = ctx.art().sheet("gnarlwarden");
        String pose = getPose();
        int frame;
        float sx = 1f;
        float sy = 1f;
        switch (pose) {
            case "windup": frame = 2; sx = 1.08f; sy = 0.92f; break;
            case "charge": frame = 3; break;
            case "leap": frame = 4; sx = 0.92f; sy = 1.1f; break;
            case "cast": case "roar": frame = 5; break;
            default: frame = getAnimator().frame(2, 2.5f, true); break;
        }
        float breathe = 1f + 0.02f * (float) Math.sin(ctx.time() * 2.4f);
        drawSprite(ctx, alpha, sheet, frame, sx, sy * breathe, 0f);
        if (!isAlive()) return;
        float dir = getFacing().dx();
        boolean charging = pose.equals("charge");
        float cx = renderCenterX(alpha) + dir * (charging ? 54f : 46f);
        float cy = renderY(alpha) + (charging ? 52f : 66f);
        float glow = isTelegraphing() ? 1f : 0.7f;
        ctx.glow(cx - 9f, cy, 9f, GLOW, glow);
        ctx.glow(cx + 9f, cy, 9f, GLOW, glow);
        ctx.glow(cx, cy + 13f, 7f, GLOW, glow);
        ctx.light(cx, cy, isTelegraphing() ? 260f : 170f, GLOW, 0.8f);
    }
}
