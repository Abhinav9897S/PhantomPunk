package phantom.entities.props;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

import phantom.abilities.AbilityType;
import phantom.audio.SoundId;
import phantom.entities.Interactable;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * A shrine holding a lost ability. Touching its orb grants the ability once.
 * <p>OOP concept: implements {@link Interactable}; announces the unlock with events.</p>
 */
public final class AbilityShrine extends Prop implements Interactable {
    private static final Color ORB = new Color(0.8f, 0.95f, 1f, 1f);
    private final AbilityType ability;
    private final String shrineId;
    private boolean taken;

    public AbilityShrine(float x, float y, AbilityType ability, String shrineId, boolean taken) {
        super(x - 30f, y, 60f, 80f);
        this.ability = ability;
        this.shrineId = shrineId;
        this.taken = taken;
    }

    @Override
    public void update(float dt, WorldContext world) {
        if (!taken && world.getRandom().nextFloat() < 0.15f) world.getEffects().wisp(centerX(), getY() + 70f, ORB);
    }

    @Override
    public boolean canInteract(Player player) {
        return !taken;
    }

    @Override
    public void interact(Player player, WorldContext world) {
        taken = true;
        world.getProgress().markCollected(shrineId);
        player.getAbilities().unlock(ability);
        world.flash(0.7f);
        world.shake(0.4f);
        world.getEffects().lightBurst(centerX(), getY() + 70f, ORB, 40);
        world.getEvents().publish(new Events.AbilityUnlocked(ability));
        world.getEvents().publish(new Events.Sfx(SoundId.ABILITY));
        world.getEvents().publish(new Events.DialogueRequest(ability.getDisplayName(),
                List.of("A forgotten power stirs within the lantern.", ability.getHint()), List.of(), null));
    }

    @Override
    public String getPrompt() { return "Touch"; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        ctx.sprite(ctx.art().sheet("shrine"), taken ? 1 : 0, centerX(), getY(), false);
        if (taken) {
            ctx.light(centerX(), getY() + 70f, 90f, ORB, 0.4f);
            return;
        }
        float bob = (float) Math.sin(ctx.time() * 2f) * 5f;
        ctx.glow(centerX(), getY() + 72f + bob, 16f, ORB, 1f);
        ctx.glow(centerX(), getY() + 72f + bob, 40f, ORB, 0.35f);
        ctx.light(centerX(), getY() + 72f + bob, 300f, ORB, 1f);
    }
}
