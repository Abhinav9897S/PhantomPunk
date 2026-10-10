package phantom.entities.props;

import com.badlogic.gdx.graphics.Color;

import phantom.render.RenderContext;
import phantom.world.Region;
import phantom.world.WorldContext;

/**
 * A decorative light source: glowing fungus, brass brazier or floating crystal, by region.
 * <p>OOP concept: a purely visual {@code GameObject}; still updated and drawn polymorphically.</p>
 */
public final class DecorLight extends Prop {
    private static final Color FUNGUS = new Color(0.4f, 1f, 0.8f, 1f);
    private static final Color BRAZIER = new Color(1f, 0.65f, 0.3f, 1f);
    private static final Color CRYSTAL = new Color(0.85f, 0.75f, 1f, 1f);
    private final Region region;
    private final float seed;

    public DecorLight(float x, float y, Region region) {
        super(x - 16f, y, 32f, 32f);
        this.region = region;
        this.seed = x * 0.37f;
    }

    private Color color() {
        switch (region) {
            case WHISPERING_ROOTS: return FUNGUS;
            case GILDED_CISTERN: return BRAZIER;
            default: return CRYSTAL;
        }
    }

    @Override
    public void update(float dt, WorldContext world) {
        float rate = region == Region.GILDED_CISTERN ? 0.25f : 0.08f;
        if (world.getRandom().nextFloat() < rate) world.getEffects().wisp(centerX(), getY() + 24f, color());
    }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float t = ctx.time() + seed;
        float flicker = region == Region.GILDED_CISTERN
                ? 0.85f + 0.15f * (float) Math.sin(t * 13f) * (float) Math.sin(t * 7.3f)
                : 0.9f + 0.1f * (float) Math.sin(t * 1.5f);
        float bob = region == Region.UMBRAL_HEART ? (float) Math.sin(t * 1.2f) * 6f : 0f;
        ctx.sprite(ctx.art().sheet("decor_" + region.name().toLowerCase()), 0, centerX(), getY() + bob, false);
        ctx.glow(centerX(), getY() + 22f + bob, 18f * flicker, color(), 0.55f);
        ctx.light(centerX(), getY() + 22f + bob, 230f * flicker, color(), 0.9f);
    }

    @Override
    public int getRenderLayer() { return 1; }
}
