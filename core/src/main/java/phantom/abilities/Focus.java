package phantom.abilities;

import phantom.audio.SoundId;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.InputSource;
import phantom.util.Constants;
import phantom.world.WorldContext;

/**
 * Focus: hold still on the ground to convert Light into one restored flame of health.
 * Taking damage interrupts it.
 * <p>OOP concept: implements {@link Ability}, overriding {@link #onDamaged} and {@link #controlsMovement}.</p>
 */
public final class Focus implements Ability {
    private float channel;

    @Override
    public AbilityType getType() {
        return AbilityType.FOCUS;
    }

    @Override
    public void update(Player player, InputSource input, WorldContext world, float dt) {
        boolean canFocus = input.isDown(GameAction.FOCUS) && player.isOnGround()
                && player.getSoul().has(Constants.SOUL_COST) && !player.getHealth().isFull();
        if (!canFocus) {
            channel = 0;
            return;
        }
        if (channel == 0) world.getEvents().publish(new Events.Sfx(SoundId.FOCUS));
        channel += dt;
        player.setVelocityX(0);
        world.getEffects().focusGather(player.centerX(), player.centerY());
        if (channel >= Constants.FOCUS_TIME) {
            channel = 0;
            player.getSoul().spend(Constants.SOUL_COST);
            player.getHealth().heal(1);
            player.getAnimator().squash(1.2f, 1.2f);
            world.getEffects().soulGain(player.centerX(), player.centerY());
            world.getEvents().publish(new Events.PlayerHealed(player.getHealth().getCurrent()));
            world.getEvents().publish(new Events.Sfx(SoundId.HEAL));
        }
    }

    @Override
    public void onDamaged(Player player) {
        channel = 0;
    }

    @Override
    public boolean controlsMovement() {
        return channel > 0;
    }

    @Override
    public void reset() {
        channel = 0;
    }

    /** 0..1 progress of the current channel, for the renderer. */
    public float getProgress() {
        return Math.min(1f, channel / Constants.FOCUS_TIME);
    }
}
