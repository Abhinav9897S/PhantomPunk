package phantom.entities.props;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.entities.Interactable;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * A stone bench: resting heals fully, sets the respawn point and saves the game.
 * <p>OOP concept: implements {@link Interactable}; saving is triggered through an event,
 * so the bench does not depend on the save system.</p>
 */
public final class Bench extends Prop implements Interactable {
    private static final Color CANDLE = new Color(1f, 0.8f, 0.5f, 1f);
    private final String benchId;

    public Bench(float x, float y, String benchId) {
        super(x - 28f, y, 56f, 30f);
        this.benchId = benchId;
    }

    @Override
    public void update(float dt, WorldContext world) {
        // static object
    }

    @Override
    public boolean canInteract(Player player) {
        return !player.isResting();
    }

    @Override
    public void interact(Player player, WorldContext world) {
        player.rest();
        player.teleport(centerX() - player.getWidth() / 2f, getY());
        world.getProgress().setBench(world.getRoomId(), benchId);
        world.getEffects().lightBurst(centerX(), getY() + 30f, CANDLE, 16);
        world.getEvents().publish(new Events.BenchRested(world.getRoomId(), benchId));
        world.getEvents().publish(new Events.Sfx(SoundId.BENCH));
    }

    @Override
    public String getPrompt() { return "Rest"; }

    public String getBenchId() { return benchId; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        ctx.sprite(ctx.art().sheet("bench"), 0, centerX(), getY(), false);
        float flicker = 0.85f + 0.15f * (float) Math.sin(ctx.time() * 9f + getX());
        ctx.glow(centerX() + 22f, getY() + 34f, 6f * flicker, CANDLE, 0.9f);
        ctx.light(centerX() + 22f, getY() + 34f, 210f * flicker, CANDLE, 0.9f);
    }
}
