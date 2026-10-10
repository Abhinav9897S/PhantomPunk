package phantom.entities.props;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.entities.DamageInfo;
import phantom.entities.Damageable;
import phantom.entities.ShardPickup;
import phantom.entities.Team;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * A crystal cluster that spills shards when struck; it stays broken once emptied.
 * <p>OOP concept: another {@link Damageable} the combat system handles polymorphically.</p>
 */
public final class ShardCache extends Prop implements Damageable {
    private static final Color CRYSTAL = new Color(0.75f, 0.95f, 1f, 1f);
    private final String cacheId;
    private final int value;
    private int hitsLeft = 4;
    private float shake;

    public ShardCache(float x, float y, String cacheId, int value) {
        super(x - 22f, y, 44f, 44f);
        this.cacheId = cacheId;
        this.value = value;
    }

    @Override
    public void update(float dt, WorldContext world) {
        if (shake > 0) shake -= dt;
    }

    @Override
    public boolean takeDamage(DamageInfo info, WorldContext world) {
        if (hitsLeft <= 0) return false;
        hitsLeft--;
        shake = 0.15f;
        int amount = hitsLeft == 0 ? value / 2 : value / 6;
        for (int i = 0; i < amount; i++) {
            float vx = (world.getRandom().nextFloat() - 0.5f) * 360f;
            world.spawn(new ShardPickup(centerX(), getY() + 30f, 1, vx, 260f + world.getRandom().nextFloat() * 260f));
        }
        world.getEffects().shardSparkle(centerX(), centerY());
        world.getEvents().publish(new Events.Sfx(SoundId.BREAK));
        if (hitsLeft == 0) {
            world.getProgress().markCollected(cacheId);
            world.getEffects().debris(getX(), getY(), getWidth(), getHeight(), CRYSTAL, 16);
            remove();
        }
        return true;
    }

    @Override
    public boolean isAlive() { return hitsLeft > 0; }

    @Override
    public Team getTeam() { return Team.NEUTRAL; }

    @Override
    public boolean givesSoul() { return false; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float s = shake > 0 ? (float) Math.sin(shake * 120f) * 3f : 0f;
        ctx.sprite(ctx.art().sheet("cache"), 0, centerX() + s, getY(), false);
        ctx.glow(centerX(), getY() + 22f, 26f, CRYSTAL, 0.35f);
        ctx.light(centerX(), getY() + 22f, 140f, CRYSTAL, 0.6f);
    }
}
