package phantom.entities.bosses;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

import phantom.entities.ProjectileKind;
import phantom.render.RenderContext;
import phantom.util.GameMath;
import phantom.world.WorldContext;

/**
 * Final encounter - the Hollow Heart, Source of the Mist: a floating core wrapped in tendrils.
 * Rings of void orbs, homing wisps, teleports, summons and mist pillars over three phases.
 * <p>OOP concept: concrete {@link Boss} that flies ({@link #isFlying()} override).</p>
 */
public final class HollowHeart extends Boss {
    private static final Color GLOW = new Color(0.78f, 0.62f, 1f, 1f);
    private static final Color CORE = new Color(1f, 0.95f, 1f, 1f);

    private final List<BossAttack> phase1 = List.of(
            new ProjectileBurstAttack(ProjectileKind.VOID_ORB, 10, 0f, 210f, false, 1, 0.4f),
            new ProjectileBurstAttack(ProjectileKind.WISP, 3, 50f, 200f, true, 1, 0.3f),
            new TeleportAttack());
    private final List<BossAttack> phase2 = List.of(
            new ProjectileBurstAttack(ProjectileKind.VOID_ORB, 12, 0f, 230f, false, 2, 0.5f),
            new ProjectileBurstAttack(ProjectileKind.WISP, 4, 70f, 210f, true, 1, 0.3f),
            new SummonAttack(2),
            new EruptionAttack(4, 130f),
            new TeleportAttack());
    private final List<BossAttack> phase3 = List.of(
            new ProjectileBurstAttack(ProjectileKind.VOID_ORB, 14, 0f, 250f, false, 3, 0.45f),
            new ProjectileBurstAttack(ProjectileKind.WISP, 5, 90f, 220f, true, 2, 0.5f),
            new EruptionAttack(6, 110f),
            new TeleportAttack());
    private float hoverSide = 1f;

    public HollowHeart(float x, float y) {
        super("hollow_heart", "The Hollow Heart", "Source of the Mist", x - 50f, y + 120f, 100f, 100f, 260, 0, 0.66f, 0.33f);
    }

    @Override
    protected List<BossAttack> attacksFor(int phase) {
        if (phase == 0) return phase1;
        return phase == 1 ? phase2 : phase3;
    }

    @Override
    protected void reposition(WorldContext world, float dt) {
        float px = world.getPlayer().centerX();
        if (Math.abs(px - centerX()) < 60f) hoverSide = -hoverSide;
        float tx = px + hoverSide * 230f;
        tx = GameMath.clamp(tx, 140f, world.getTileMap().widthPx() - 140f);
        float ty = world.getPlayer().getY() + 190f + (float) Math.sin(world.getTime() * 1.7f) * 30f;
        float dx = tx - centerX();
        float dy = ty - centerY();
        setVelocity(GameMath.approach(getVelocityX(), GameMath.clamp(dx * 2f, -260f, 260f), 500f * dt),
                GameMath.approach(getVelocityY(), GameMath.clamp(dy * 2f, -200f, 200f), 500f * dt));
        face(px);
        stepPhysics(world, dt);
    }

    @Override
    protected float cooldownFor(int phase) {
        return phase == 0 ? 1.2f : (phase == 1 ? 0.85f : 0.55f);
    }

    @Override
    public boolean isFlying() { return true; }

    @Override
    public boolean isFinal() { return true; }

    @Override
    protected Color getGlowColor() { return GLOW; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float fade = getFade() * (isAlive() ? 1f : Math.max(0f, 1f - getDeathTimer() / deathLinger()));
        float cx = renderCenterX(alpha);
        float cy = renderY(alpha) + getHeight() / 2f;
        float t = ctx.time();
        var tendrils = ctx.art().sheet("heart_tendrils");
        ctx.spriteCentered(tendrils, 0, cx, cy, false, 1f + 0.04f * (float) Math.sin(t * 2f), 1f, t * 12f, fade);
        ctx.spriteCentered(tendrils, 0, cx, cy, true, 0.8f, 0.8f, -t * 18f, fade * 0.7f);
        int frame = Math.min(2, getPhase());
        float pulse = 1f + 0.05f * (float) Math.sin(t * (isTelegraphing() ? 14f : 4f));
        ctx.spriteCentered(ctx.art().sheet("heart_core"), frame, cx, cy, false, pulse, pulse, 0f, fade);
        if (getFlashAmount() > 0) {
            ctx.spriteFlashCentered(ctx.art().sheet("heart_core"), frame, cx, cy, pulse, getFlashAmount() * fade);
        }
        float eye = isTelegraphing() ? 1f : 0.75f;
        ctx.glow(cx, cy + 4f, 16f * pulse, CORE, eye * fade);
        ctx.glow(cx, cy, 90f, GLOW, 0.35f * fade);
        ctx.light(cx, cy, 380f, GLOW, 0.9f * fade);
    }
}
