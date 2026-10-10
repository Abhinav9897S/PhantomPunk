package phantom.entities.props;

import com.badlogic.gdx.graphics.Color;

import phantom.entities.DamageInfo;
import phantom.entities.DamageKind;
import phantom.render.RenderContext;
import phantom.util.Constants;
import phantom.world.Region;
import phantom.world.TileMap;
import phantom.world.WorldContext;

/**
 * A telegraphed pillar that bursts out of the floor: roots, steam or mist depending on the region.
 * <p>OOP concept: a small timed hazard object created by boss attacks.</p>
 */
public final class GroundEruption extends Prop {
    private static final Color ROOTS = new Color(0.5f, 1f, 0.7f, 1f);
    private static final Color STEAM = new Color(1f, 0.8f, 0.5f, 1f);
    private static final Color MIST = new Color(0.8f, 0.6f, 1f, 1f);
    private static final float ACTIVE = 0.45f;
    private static final float RETRACT = 0.25f;
    private final float telegraph;
    private float time;
    private Color tint = Color.WHITE;

    public GroundEruption(float centerX, float floorY, float telegraph) {
        super(centerX - 16f, floorY, 32f, 96f);
        this.telegraph = telegraph;
    }

    /** Finds the top of the floor at or below {@code y}. */
    public static float findFloor(TileMap map, float x, float y) {
        int c = TileMap.col(x);
        for (int r = TileMap.row(y); r >= 0; r--) {
            if (map.isSolid(c, r - 1) || map.isOneWay(c, r - 1)) return r * Constants.TILE;
        }
        return y;
    }

    @Override
    public void update(float dt, WorldContext world) {
        time += dt;
        tint = colorFor(world.getRegion());
        if (time < telegraph) {
            if (world.getRandom().nextFloat() < 0.4f) world.getEffects().wisp(centerX(), getY() + 4, tint);
            return;
        }
        if (time < telegraph + ACTIVE) {
            if (time - dt < telegraph) {
                world.shake(0.2f);
                world.getEffects().debris(getX(), getY(), getWidth(), 10, tint, 6);
            }
            if (world.getPlayer().overlaps(this)) {
                world.getPlayer().takeDamage(new DamageInfo(1, centerX(), getY(), DamageKind.CONTACT), world);
            }
            return;
        }
        if (time > telegraph + ACTIVE + RETRACT) remove();
    }

    private static Color colorFor(Region region) {
        switch (region) {
            case WHISPERING_ROOTS: return ROOTS;
            case GILDED_CISTERN: return STEAM;
            default: return MIST;
        }
    }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float cx = centerX();
        if (time < telegraph) {
            float k = time / telegraph;
            ctx.glow(cx, getY() + 4f, 20f + 20f * k, tint, 0.3f + 0.6f * k);
            ctx.light(cx, getY() + 10f, 120f, tint, 0.6f * k);
            return;
        }
        float t = time - telegraph;
        float rise = t < 0.08f ? t / 0.08f : (t > ACTIVE ? Math.max(0f, 1f - (t - ACTIVE) / RETRACT) : 1f);
        ctx.sprite(ctx.art().sheet("eruption_" + ctx.region().name().toLowerCase()), 0, cx, getY(), false, 1f, rise, 0f, 0f, 1f);
        ctx.light(cx, getY() + 48f * rise, 160f, tint, 0.8f);
    }

    @Override
    public int getRenderLayer() { return 15; }
}
