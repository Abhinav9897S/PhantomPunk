package phantom.abilities;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.input.InputSource;
import phantom.util.Constants;
import phantom.world.WorldContext;

/**
 * Wisp Leap: one extra jump in mid-air, restored on landing, wall cling or pogo.
 * <p>OOP concept: implements {@link Ability}; most behaviour lives in {@link #onJumpPressed}.</p>
 */
public final class DoubleJump implements Ability {
    private static final Color WISP = new Color(0.85f, 0.9f, 1f, 1f);
    private boolean available = true;

    @Override
    public AbilityType getType() {
        return AbilityType.DOUBLE_JUMP;
    }

    @Override
    public void update(Player player, InputSource input, WorldContext world, float dt) {
        if (player.isOnGround()) available = true;
    }

    @Override
    public boolean onJumpPressed(Player player, WorldContext world) {
        if (!available || player.isOnGround()) return false;
        available = false;
        player.setVelocityY(Constants.DOUBLE_JUMP_VELOCITY);
        player.getAnimator().squash(0.75f, 1.3f);
        for (int i = 0; i < 14; i++) world.getEffects().wisp(player.centerX(), player.getY(), WISP);
        world.getEvents().publish(new Events.Sfx(SoundId.WISP_LEAP));
        return true;
    }

    @Override
    public void refresh(Player player) {
        available = true;
    }

    @Override
    public void reset() {
        available = true;
    }

    public boolean isAvailable() {
        return available;
    }
}
