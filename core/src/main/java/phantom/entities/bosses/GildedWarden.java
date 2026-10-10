package phantom.entities.bosses;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

import phantom.entities.ProjectileKind;
import phantom.render.RenderContext;
import phantom.util.GameMath;
import phantom.world.WorldContext;

/**
 * Boss 2 - the Gilded Warden, Last Engine of Veyra: a brass automaton knight.
 * Three phases: lunges and gear volleys, then steam vents, then everything faster.
 * <p>OOP concept: concrete {@link Boss}; reuses the same attack classes as the Gnarlwarden
 * with different parameters (polymorphism + composition, no duplicated code).</p>
 */
public final class GildedWarden extends Boss {
    private static final Color GLOW = new Color(1f, 0.75f, 0.35f, 1f);

    private final List<BossAttack> phase1 = List.of(
            new ChargeAttack(0.4f, 700f, 0.5f),
            new LeapAttack(1100f, true),
            new ProjectileBurstAttack(ProjectileKind.GEAR, 3, 36f, 340f, true, 1, 0.3f));
    private final List<BossAttack> phase2 = List.of(
            new ChargeAttack(0.35f, 760f, 0.55f),
            new LeapAttack(1150f, true),
            new ProjectileBurstAttack(ProjectileKind.GEAR, 5, 60f, 360f, true, 2, 0.45f),
            new EruptionAttack(4, 120f));
    private final List<BossAttack> phase3 = List.of(
            new ChargeAttack(0.28f, 820f, 0.6f),
            new LeapAttack(1200f, true),
            new ProjectileBurstAttack(ProjectileKind.GEAR, 7, 80f, 380f, true, 2, 0.35f),
            new EruptionAttack(6, 100f));

    public GildedWarden(float x, float y) {
        super("gilded_warden", "Gilded Warden", "Last Engine of Veyra", x - 28f, y, 56f, 112f, 200, 120, 0.6f, 0.3f);
    }

    @Override
    protected List<BossAttack> attacksFor(int phase) {
        if (phase == 0) return phase1;
        return phase == 1 ? phase2 : phase3;
    }

    @Override
    protected void reposition(WorldContext world, float dt) {
        float dx = world.getPlayer().centerX() - centerX();
        face(world.getPlayer().centerX());
        // keeps a duelling distance: steps in when far, steps back when crowded
        float target = Math.abs(dx) > 280f ? Math.signum(dx) * 120f : (Math.abs(dx) < 120f ? -Math.signum(dx) * 90f : 0f);
        setVelocityX(GameMath.approach(getVelocityX(), target, 700f * dt));
        stepPhysics(world, dt);
    }

    @Override
    protected float cooldownFor(int phase) {
        return phase == 0 ? 0.9f : (phase == 1 ? 0.65f : 0.45f);
    }

    @Override
    public boolean isFinal() { return false; }

    @Override
    protected Color getGlowColor() { return GLOW; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        var sheet = ctx.art().sheet("warden");
        String pose = getPose();
        int frame;
        float sy = 1f;
        switch (pose) {
            case "windup": frame = 1; sy = 0.95f; break;
            case "charge": frame = 2; break;
            case "leap": frame = 3; break;
            case "cast": case "roar": frame = 4; break;
            default: frame = 0; break;
        }
        float bob = (float) Math.sin(ctx.time() * 3f) * 0.015f;
        drawSprite(ctx, alpha, sheet, frame, 1f, sy + bob, 0f);
        if (!isAlive()) return;
        float dir = getFacing().dx();
        float vx = renderCenterX(alpha) + dir * 6f;
        float vy = renderY(alpha) + 98f;
        float glow = isTelegraphing() ? 1f : 0.65f;
        ctx.glow(vx, vy, 10f + (isTelegraphing() ? 6f : 0f), GLOW, glow);
        ctx.light(vx, vy, 200f, GLOW, 0.8f);
        ctx.glow(renderCenterX(alpha), renderY(alpha) + 62f, 14f, GLOW, 0.4f + 0.1f * getPhase());
    }
}
