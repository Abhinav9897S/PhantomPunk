package phantom.entities.player;

import phantom.audio.SoundId;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.InputSource;
import phantom.util.Constants;
import phantom.util.Direction;
import phantom.util.GameMath;
import phantom.world.WorldContext;

/**
 * Running and jumping for the player: acceleration, friction, variable jump height,
 * coyote time, jump buffering and dropping through one-way platforms.
 * <p>OOP concept: <b>composition</b> - the player delegates movement rules to this component,
 * keeping {@link Player} short and focused.</p>
 */
final class PlayerMotor {
    private float coyoteTimer;
    private float bufferTimer;
    private float dropTimer;
    private boolean rising;

    void updateHorizontal(Player p, InputSource in, float dt) {
        if (p.isControlLocked()) return; // keep wall-jump / knockback momentum
        int h = in.horizontal();
        float target = h * Constants.RUN_SPEED;
        float accel;
        if (p.isOnGround()) accel = h != 0 ? Constants.GROUND_ACCEL : Constants.GROUND_FRICTION;
        else accel = Constants.AIR_ACCEL;
        p.setVelocityX(GameMath.approach(p.getVelocityX(), target, accel * dt));
        if (h != 0 && !p.isClinging()) p.setFacing(h > 0 ? Direction.RIGHT : Direction.LEFT);
    }

    void updateJump(Player p, InputSource in, WorldContext world, float dt) {
        coyoteTimer = p.isOnGround() ? Constants.COYOTE_TIME : coyoteTimer - dt;
        bufferTimer = in.isJustPressed(GameAction.JUMP) ? Constants.JUMP_BUFFER : bufferTimer - dt;
        if (dropTimer > 0) dropTimer -= dt;

        if (bufferTimer > 0) {
            if (p.isOnGround() && in.isDown(GameAction.DOWN) && p.isStandingOnOneWay(world)) {
                dropTimer = 0.25f;
                bufferTimer = 0;
            } else if (coyoteTimer > 0) {
                groundJump(p, world);
            } else if (in.isJustPressed(GameAction.JUMP) && p.getAbilities().tryJump(p, world)) {
                bufferTimer = 0;
                rising = true;
            }
        }
        // Variable jump height: letting go early cuts the upward speed.
        if (rising && !in.isDown(GameAction.JUMP) && p.getVelocityY() > 0) {
            p.setVelocityY(p.getVelocityY() * Constants.JUMP_CUT);
            rising = false;
        }
        if (p.getVelocityY() <= 0) rising = false;
    }

    private void groundJump(Player p, WorldContext world) {
        p.setVelocityY(Constants.JUMP_VELOCITY);
        bufferTimer = 0;
        coyoteTimer = 0;
        rising = true;
        p.getAnimator().squash(0.8f, 1.2f);
        world.getEffects().dust(p.centerX(), p.getY(), 5);
        world.getEvents().publish(new Events.Sfx(SoundId.JUMP));
    }

    /** Pogo / bounce: an upward launch that can still be cut short. */
    void bounce(Player p, float velocity) {
        p.setVelocityY(velocity);
        rising = true;
        coyoteTimer = 0;
    }

    boolean isDroppingThrough() {
        return dropTimer > 0;
    }

    void reset() {
        coyoteTimer = 0;
        bufferTimer = 0;
        dropTimer = 0;
        rising = false;
    }
}
