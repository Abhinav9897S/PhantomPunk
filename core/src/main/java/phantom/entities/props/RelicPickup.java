package phantom.entities.props;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.components.Inventory;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * An Ember Relic hidden in a secret room. Collecting all three unlocks the hidden ending.
 * <p>OOP concept: a collectible prop updating {@link Inventory} and world progress.</p>
 */
public final class RelicPickup extends Prop {
    private static final Color EMBER = new Color(1f, 0.55f, 0.25f, 1f);
    private final String relicId;

    public RelicPickup(float x, float y, String relicId) {
        super(x - 12f, y + 8f, 24f, 28f);
        this.relicId = relicId;
    }

    @Override
    public void update(float dt, WorldContext world) {
        if (world.getRandom().nextFloat() < 0.2f) world.getEffects().wisp(centerX(), centerY(), EMBER);
        if (!world.getPlayer().overlaps(this)) return;
        Inventory inv = world.getPlayer().getInventory();
        inv.addRelic(relicId);
        world.getProgress().markCollected(relicId);
        world.getEffects().lightBurst(centerX(), centerY(), EMBER, 30);
        world.flash(0.4f);
        world.getEvents().publish(new Events.ItemCollected(relicId, "Ember Relic"));
        world.getEvents().publish(new Events.Toast("Ember Relic  (" + inv.getRelicCount() + " / " + Inventory.TOTAL_RELICS + ")"));
        world.getEvents().publish(new Events.Sfx(SoundId.ABILITY));
        remove();
    }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float bob = (float) Math.sin(ctx.time() * 2.5f) * 4f;
        ctx.sprite(ctx.art().sheet("relic"), 0, centerX(), getY() + bob, false);
        ctx.glow(centerX(), centerY() + bob, 22f, EMBER, 0.8f);
        ctx.light(centerX(), centerY() + bob, 220f, EMBER, 1f);
    }
}
