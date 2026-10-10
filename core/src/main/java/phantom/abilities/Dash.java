package phantom.abilities;

import phantom.audio.SoundId;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.InputSource;
import phantom.util.Constants;
import phantom.util.Direction;
import phantom.world.WorldContext;

/**
 * Umbral Dash: a short, gravity-free horizontal burst with a cooldown. One air dash per jump.
 * <p>OOP concept: implements {@link Ability}; owns its own timers (encapsulation).</p>
 */
public final class Dash implements Ability {
    private float activeTimer;
    private float cooldown;
    private boolean airDashReady = true;
    private Direction direction = Direction.RIGHT;

    @Override
    public AbilityType getType() {
        return AbilityType.DASH;
    }

    @Override
    public void update(Player player, InputSource input, WorldContext world, float dt) {
        if (cooldown > 0) cooldown -= dt;
        if (activeTimer > 0) {
            activeTimer -= dt;
            player.setVelocity(direction.dx() * Constants.DASH_SPEED, 0f);
            world.getEffects().dashTrail(player.centerX(), player.centerY());
            if (activeTimer <= 0 || player.getContacts().isHitWall()) {
                activeTimer = 0;
                player.setVelocityX(direction.dx() * Constants.RUN_SPEED * 0.7f);
            }
            return;
        }
        if (input.isJustPressed(GameAction.DASH) && cooldown <= 0 && (player.isOnGround() || airDashReady)) {
            start(player, world);
        }
    }

    private void start(Player player, WorldContext world) {
        direction = player.getFacing();
        activeTimer = Constants.DASH_TIME;
        cooldown = Constants.DASH_COOLDOWN;
        if (!player.isOnGround()) airDashReady = false;
        player.getAnimator().squash(1.35f, 0.75f);
        world.getEvents().publish(new Events.Sfx(SoundId.DASH));
    }

    @Override
    public void refresh(Player player) {
        airDashReady = true;
    }

    @Override
    public boolean controlsMovement() {
        return activeTimer > 0;
    }

    @Override
    public boolean suspendsGravity() {
        return activeTimer > 0;
    }

    @Override
    public void reset() {
        activeTimer = 0;
        cooldown = 0;
        airDashReady = true;
    }

    public boolean isDashing() {
        return activeTimer > 0;
    }
}
