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
 * Moss Grip: hold towards a wall while falling to slide slowly down it, then jump off.
 * <p>OOP concept: implements {@link Ability}, overriding {@link #onJumpPressed} and {@link #modifyMaxFall}.</p>
 */
public final class WallCling implements Ability {
    private boolean clinging;
    private Direction wall = Direction.LEFT;

    @Override
    public AbilityType getType() {
        return AbilityType.WALL_CLING;
    }

    @Override
    public void update(Player player, InputSource input, WorldContext world, float dt) {
        if (clinging) {
            boolean pressingAway = input.isDown(wall == Direction.LEFT ? GameAction.RIGHT : GameAction.LEFT);
            if (player.isOnGround() || !player.isTouchingWall(wall) || pressingAway) {
                clinging = false;
            } else {
                player.setFacing(wall.opposite());
                if (player.getVelocityY() < 0 && world.getRandom().nextFloat() < 0.3f) {
                    world.getEffects().dust(wall == Direction.LEFT ? player.getX() : player.getX() + player.getWidth(),
                            player.getY() + player.getHeight() * 0.6f, 1);
                }
            }
            return;
        }
        if (player.isOnGround() || player.getVelocityY() > 0 || player.isControlLocked()) return;
        if (input.isDown(GameAction.LEFT) && player.isTouchingWall(Direction.LEFT)) grab(player, Direction.LEFT);
        else if (input.isDown(GameAction.RIGHT) && player.isTouchingWall(Direction.RIGHT)) grab(player, Direction.RIGHT);
    }

    private void grab(Player player, Direction side) {
        clinging = true;
        wall = side;
        player.getAbilities().refreshAll(player);
        player.setVelocityY(Math.max(player.getVelocityY(), -Constants.WALL_SLIDE_SPEED));
    }

    @Override
    public boolean onJumpPressed(Player player, WorldContext world) {
        if (!clinging) return false;
        clinging = false;
        float away = wall.opposite().dx();
        player.setVelocity(away * Constants.WALL_JUMP_X, Constants.WALL_JUMP_Y);
        player.setFacing(wall.opposite());
        player.lockControl(Constants.WALL_JUMP_LOCK);
        player.getAnimator().squash(0.8f, 1.25f);
        world.getEvents().publish(new Events.Sfx(SoundId.JUMP));
        return true;
    }

    @Override
    public float modifyMaxFall(float normal) {
        return clinging ? Constants.WALL_SLIDE_SPEED : normal;
    }

    @Override
    public void reset() {
        clinging = false;
    }

    public boolean isClinging() {
        return clinging;
    }

    public Direction getWallSide() {
        return wall;
    }
}
