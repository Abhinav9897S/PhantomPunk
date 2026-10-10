package phantom.entities;

import phantom.components.Hitbox;
import phantom.systems.CollisionResult;
import phantom.util.Direction;
import phantom.world.WorldContext;

/**
 * A {@link GameObject} with a size, a velocity, a facing direction and tile collision.
 * Position (x, y) is the bottom-left corner of the hitbox.
 * <p>OOP concept: <b>inheritance</b> (extends GameObject) and <b>abstraction</b>
 * ({@link #onDeath} is left to subclasses).</p>
 */
public abstract class Entity extends GameObject implements Collidable {
    private final Hitbox hitbox;
    private final CollisionResult contacts = new CollisionResult();
    private float vx;
    private float vy;
    private Direction facing = Direction.RIGHT;

    protected Entity(float x, float y, float width, float height) {
        super(x, y);
        hitbox = new Hitbox(x, y, width, height);
    }

    /** Called once when the entity dies or is destroyed. */
    public abstract void onDeath(WorldContext world);

    @Override
    public Hitbox getHitbox() {
        hitbox.setPosition(getX(), getY());
        return hitbox;
    }

    /** Applies gravity, then moves through the tile map resolving collisions. */
    protected void moveWithPhysics(WorldContext world, float dt, float gravity, float maxFall, boolean dropThrough) {
        vy = Math.max(vy - gravity * dt, -maxFall);
        world.getCollision().move(this, world.getTileMap(), dt, dropThrough);
    }

    /** Changes the hitbox size (used by pooled projectiles of different kinds). */
    protected void resize(float width, float height) {
        hitbox.set(getX(), getY(), width, height);
    }

    public float getWidth() { return hitbox.getWidth(); }
    public float getHeight() { return hitbox.getHeight(); }
    public float centerX() { return getX() + getWidth() * 0.5f; }
    public float centerY() { return getY() + getHeight() * 0.5f; }
    public float renderCenterX(float alpha) { return renderX(alpha) + getWidth() * 0.5f; }

    public float getVelocityX() { return vx; }
    public float getVelocityY() { return vy; }
    public void setVelocity(float nvx, float nvy) { vx = nvx; vy = nvy; }
    public void setVelocityX(float nvx) { vx = nvx; }
    public void setVelocityY(float nvy) { vy = nvy; }

    public Direction getFacing() { return facing; }
    public void setFacing(Direction d) { if (d.isHorizontal()) facing = d; }
    public void face(float towardsX) { facing = towardsX >= centerX() ? Direction.RIGHT : Direction.LEFT; }

    /** Collision flags from the most recent move. */
    public CollisionResult getContacts() { return contacts; }
    public boolean isOnGround() { return contacts.isOnGround(); }
}
