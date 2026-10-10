package phantom.entities;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.util.Constants;
import phantom.world.WorldContext;

/**
 * A shard of currency that bounces out of defeated enemies and is drawn to the player.
 * <p>OOP concept: small {@link Entity} subclass with physics and a magnet behaviour.</p>
 */
public final class ShardPickup extends Entity {
    private static final Color SHINE = new Color(0.75f, 0.95f, 1f, 1f);
    private final int value;
    private float age;

    public ShardPickup(float cx, float cy, int value, float vx, float vy) {
        super(cx - 5f, cy - 6f, 10f, 12f);
        this.value = value;
        setVelocity(vx, vy);
    }

    @Override
    public void update(float dt, WorldContext world) {
        age += dt;
        if (age > 45f) {
            remove();
            return;
        }
        Player p = world.getPlayer();
        float dx = p.centerX() - centerX();
        float dy = p.centerY() - centerY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (age > 0.5f && dist < 110f && !p.isDead()) {
            float pull = 900f / Math.max(dist, 20f);
            setVelocity(getVelocityX() * 0.85f + dx * pull * dt * 4f, getVelocityY() * 0.85f + dy * pull * dt * 4f);
            moveTo(getX() + getVelocityX() * dt, getY() + getVelocityY() * dt);
        } else {
            float before = getVelocityY();
            moveWithPhysics(world, dt, Constants.GRAVITY, Constants.MAX_FALL_SPEED, false);
            if (isOnGround()) {
                setVelocity(getVelocityX() * 0.6f, before < -150f ? -before * 0.35f : 0f);
            }
        }
        if (age > 0.3f && overlaps(p) && !p.isDead()) collect(world, p);
    }

    private void collect(WorldContext world, Player p) {
        p.getInventory().addShards(value);
        world.getEffects().shardSparkle(centerX(), centerY());
        world.getEvents().publish(new Events.ShardsCollected(value, p.getInventory().getShards()));
        world.getEvents().publish(new Events.Sfx(SoundId.SHARD));
        remove();
    }

    @Override
    public void onDeath(WorldContext world) {
        remove();
    }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float spin = (float) Math.sin(age * 6f);
        ctx.sprite(ctx.art().sheet("shard"), 0, renderCenterX(alpha), renderY(alpha), false, Math.abs(spin) * 0.6f + 0.4f, 1f, 0f, 0f, 1f);
        ctx.glow(renderCenterX(alpha), renderY(alpha) + 6f, 9f, SHINE, 0.5f + 0.3f * Math.abs(spin));
    }

    @Override
    public int getRenderLayer() { return 8; }
}
