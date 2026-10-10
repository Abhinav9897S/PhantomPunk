package phantom.entities;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * The shade left where Phantom fell, holding the lost shards. Touch it to take them back.
 * <p>OOP concept: {@link Entity} subclass; its existence is driven by saved world progress.</p>
 */
public final class Shade extends Entity {
    private static final Color VOID = new Color(0.65f, 0.45f, 1f, 1f);
    private static final Color EYES = new Color(0.9f, 0.85f, 1f, 1f);
    private final int shards;
    private float time;

    public Shade(float x, float y, int shards) {
        super(x, y, 28f, 44f);
        this.shards = shards;
    }

    @Override
    public void update(float dt, WorldContext world) {
        time += dt;
        if (world.getRandom().nextFloat() < 0.3f) world.getEffects().wisp(centerX(), centerY(), VOID);
        Player p = world.getPlayer();
        if (!p.isDead() && overlaps(p)) {
            p.getInventory().addShards(shards);
            world.getProgress().clearShade();
            world.getEffects().lightBurst(centerX(), centerY(), VOID, 40);
            world.flash(0.4f);
            world.getEvents().publish(new Events.ShadeRecovered(shards));
            world.getEvents().publish(new Events.Toast("Recovered " + shards + " shards from your shade"));
            world.getEvents().publish(new Events.Sfx(SoundId.ABILITY));
            onDeath(world);
        }
    }

    @Override
    public void onDeath(WorldContext world) {
        remove();
    }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float bob = (float) Math.sin(time * 2f) * 6f;
        float cx = renderCenterX(alpha);
        float fy = renderY(alpha) + bob + 8f;
        ctx.sprite(ctx.art().sheet("shade"), 0, cx, fy, false, 1f, 1f, (float) Math.sin(time * 1.3f) * 4f, 0f, 0.85f);
        ctx.glow(cx - 3f, fy + 34f, 5f, EYES, 1f);
        ctx.glow(cx + 3f, fy + 34f, 5f, EYES, 1f);
        ctx.glow(cx, fy + 22f, 40f, VOID, 0.3f);
        ctx.light(cx, fy + 22f, 200f, VOID, 0.8f);
    }

    @Override
    public int getRenderLayer() { return 6; }

    public int getShards() { return shards; }
}
